package service;

import api.conect_API_eBay;
import com.google.gson.JsonObject;
import dao.*;
import entities.*;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import org.hibernate.Hibernate;
import utils.ReportService;
import utils.Sesion;
import utils.cls_browseEBAY;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Servicio centralizado para el análisis de productos.
 * Contiene la lógica de negocio y acceso a BD.
 */
public class ProductAnalysisService {

    private final ProductDAO productDAO = new ProductDAO();
    private final PriceHistoryDAO priceHistoryDAO = new PriceHistoryDAO();
    private final CouponProDAO couponProDAO = new CouponProDAO();
    private final SellerDAO sellerDAO = new SellerDAO();
    private final ImagesProductDAO imagesProductDAO = new ImagesProductDAO();
    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final ProductAnalysisDAO productAnalysisDAO = new ProductAnalysisDAO();

    private final conect_API_eBay api = new conect_API_eBay();
    private final cls_browseEBAY browseEBAY = new cls_browseEBAY();

    private final String tokenAPI = Sesion.getTokenAPI();

    // Pool de hilos compartido
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    // Caché de precios de mercado por nombre de producto
    private static final Map<String, List<Double>> marketPriceCache = new ConcurrentHashMap<>();

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void shutdownExecutor() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
        }
    }

    // ======================================================
    // DTOs SIMPLES
    // ======================================================

    public static class MarketStats {
        public double promedio;
        public double min;
        public double max;
        public double desviacion;
        public double coefVariacion;
        public int cantidad;
        public String estabilidad;
        public String alertaPrecio;
        public String confianza;
    }

    public static class TendenciaInfo {
        public double cambioPorcentual;
        public double volatilidad;
        public String icono;
        public String descripcionTendencia;
        public String descripcionVolatilidad;
    }

    public static class EstadisticasGenerales {
        public int totalAnalizados;
        public double promedioVariacion;
        public List<ProductAnalysis> topPositivos;
        public List<ProductAnalysis> topNegativos;
        public List<ResumenDiario> resumenDiario;
    }

    public static class ResumenDiario {
        public String fecha;
        public long cantidad;
    }

    public static class ProductAnalysisResult {
        public Producto producto;
        public ProductAnalysis analisis;
        public PriceHistory precioActual;
        public CouponPro cupon;
        public Seller vendedor;
        public String antiguedad;
        public double trustScore;
        public MarketStats marketStats;
        public List<PriceHistory> historial;
        public TendenciaInfo tendencia;
        public String urlImagen; // URL principal del producto
    }

    // ======================================================
    // MÉTODOS PÚBLICOS PRINCIPALES
    // ======================================================

    /**
     * Analiza un producto completo: BD + mercado + vendedor + historial.
     */
    public ProductAnalysisResult analizarProducto(String itemId) {
        Producto producto = productDAO.findByItemId(itemId);
        if (producto == null) {
            return null;
        }

        ProductAnalysisResult result = new ProductAnalysisResult();
        result.producto = producto;

        ProductAnalysis analisis = new ProductAnalysis();
        analisis.setItem(producto);
        analisis.setAnalysisDate(Instant.now());
        result.analisis = analisis;

        try {
            // Futures en paralelo
            CompletableFuture<PriceHistory> precioFut = CompletableFuture.supplyAsync(
                    () -> priceHistoryDAO.findLatestByItemId(itemId), EXECUTOR
            );

            CompletableFuture<CouponPro> cuponFut = CompletableFuture.supplyAsync(
                    () -> couponProDAO.findByItemId(itemId), EXECUTOR
            );

            CompletableFuture<Seller> vendedorFut = CompletableFuture.supplyAsync(() -> {
                if (producto.getIdSeller() == null) return null;
                return sellerDAO.getUserById(producto.getIdSeller().getId());
            }, EXECUTOR);

            CompletableFuture<List<PriceHistory>> historialFut = CompletableFuture.supplyAsync(
                    () -> priceHistoryDAO.findAllByItemId(itemId), EXECUTOR
            );

            CompletableFuture<List<Double>> preciosMercadoFut = CompletableFuture.supplyAsync(
                    () -> cargarPreciosMercado(producto.getName()), EXECUTOR
            );

            CompletableFuture<String> urlImagenFut = CompletableFuture.supplyAsync(
                    () -> imagesProductDAO.findMainImageByProduct(producto), EXECUTOR
            );

            // Obtener resultados (bloqueo controlado)
            PriceHistory precioActual = precioFut.get(5, TimeUnit.SECONDS);
            // ===============================================
// 1️⃣  OBTENER NUEVO PRECIO DESDE API
// ===============================================
            Double precioAPI = obtenerPrecioDesdeAPI(itemId);

// Validar precio de API
            if (precioAPI != null && precioAPI > 0) {

                // ===============================================
                // 2️⃣  GUARDAR NUEVO PRECIO SI CAMBIÓ
                // ===============================================
                guardarNuevoPrecioSiCambio(producto, precioAPI);

                // Reemplazar precioActual con el nuevo valor
                // para que el análisis use ese precio
                precioActual = new PriceHistory();
                precioActual.setPrice(precioAPI);
                precioActual.setCurrency("USD");
            }

            double precioVal = (precioActual != null) ? precioActual.getPrice() : 0.0;

            // Guardar nuevo historial
            registrarNuevoPrecio(producto, precioVal);

            CouponPro cupon = cuponFut.get(3, TimeUnit.SECONDS);
            Seller vendedor = vendedorFut.get(5, TimeUnit.SECONDS);
            List<PriceHistory> historial = historialFut.get(5, TimeUnit.SECONDS);
            List<Double> preciosMercado = preciosMercadoFut.get(10, TimeUnit.SECONDS);
            String urlImagen = urlImagenFut.get(3, TimeUnit.SECONDS);

            result.precioActual = precioActual;
            result.cupon = cupon;
            result.vendedor = vendedor;
            result.historial = historial != null ? historial : Collections.emptyList();
            result.urlImagen = (urlImagen != null && !urlImagen.isEmpty())
                    ? urlImagen
                    : "/recursos/img/no-image.png";

            precioVal = (precioActual != null) ? precioActual.getPrice() : 0.0;

            // Market stats
            MarketStats stats = calcularMarketStats(preciosMercado, precioVal);
            result.marketStats = stats;

            // Antigüedad + trustScore
            if (vendedor != null) {
                AntiguedadTrust at = obtenerAntiguedadYTrustScore(
                        vendedor,
                        precioVal,
                        stats.promedio,
                        stats.desviacion
                );
                result.antiguedad = at.antiguedad;
                result.trustScore = at.trustScore;
                analisis.setTrustScore(at.trustScore);
                analisis.setIdSeller(vendedor);
            }

            // Completar análisis
            analisis.setPriceActual(precioVal);
            analisis.setMarketAverage(stats.promedio);
            analisis.setMarketMin(stats.min);
            analisis.setMarketMax(stats.max);
            analisis.setStdDeviation(stats.desviacion);
            analisis.setPriceDifference(precioVal - stats.promedio);

            // Tendencia
            TendenciaInfo tendencia = analizarTendencia(result.historial);
            result.tendencia = tendencia;

            // Guardar/actualizar análisis
            guardarAnalisis(producto, analisis);

            return result;

        } catch (TimeoutException te) {
            System.err.println("Timeout en análisis de producto: " + te.getMessage());
            return result; // Devolver lo que se tenga
        } catch (Exception e) {
            System.err.println("Error analizando producto: " + e.getMessage());
            e.printStackTrace();
            return result;
        }
    }

    /**
     * Carga estadísticas generales (RF-061 a RF-065).
     */
    public EstadisticasGenerales obtenerEstadisticasGenerales() {
        EstadisticasGenerales eg = new EstadisticasGenerales();
        try {
            eg.totalAnalizados = productAnalysisDAO.countAll();
            eg.promedioVariacion = productAnalysisDAO.getPromedioVariacionGeneral();

            // Ya no se usan los top desde BD
            eg.topPositivos = Collections.emptyList();
            eg.topNegativos = Collections.emptyList();

            var resumen = productAnalysisDAO.getResumenDiarioConsultas();
            List<ResumenDiario> lista = new ArrayList<>();
            for (Object[] row : resumen) {
                ResumenDiario rd = new ResumenDiario();
                rd.fecha = String.valueOf(row[0]);
                rd.cantidad = ((Number) row[1]).longValue();
                lista.add(rd);
            }
            eg.resumenDiario = lista;

        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas generales: " + e.getMessage());
            eg.resumenDiario = Collections.emptyList();
        }
        return eg;
    }

    /**
     * Elimina un producto de la wishlist del usuario.
     */
    public boolean eliminarDeWishlist(Producto producto, int userId) {
        try {
            if (producto == null) return false;

            WishlistProduct wp = wishlistDAO.findByItemId(producto.getItemId());
            if (wp == null) {
                return false;
            }
            wishlistDAO.delete(wp);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error eliminando de wishlist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cuenta cuántos productos tiene guardados el usuario.
     */
    public int contarWishlistPorUsuario(int userId) {
        try {
            return wishlistDAO.countByUser(userId);
        } catch (Exception e) {
            System.err.println("❌ Error contando wishlist: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Genera el reporte PDF único de un producto.
     */
    public boolean generarReporteProducto(Producto productoActual, ProductAnalysis analisisActual) {
        if (productoActual == null || analisisActual == null) return false;

        EntityManager em = null;
        try {
            em = genericDAO.getEmf().createEntityManager();

            // Producto con todas sus relaciones
            Producto productoCompleto = productDAO.findByItemIdWithFullDetails(productoActual.getItemId());
            if (productoCompleto == null) {
                System.err.println("❌ No se pudo obtener el producto completo para el reporte");
                return false;
            }

            // Aseguramos que el análisis esté adjunto a la sesión
            ProductAnalysis analisisCompleto = em.merge(analisisActual);

            // Inicializar vendedor y marketplace si existen
            if (analisisCompleto.getIdSeller() != null) {
                Hibernate.initialize(analisisCompleto.getIdSeller());
                if (analisisCompleto.getIdSeller().getMarketplace() != null) {
                    Hibernate.initialize(analisisCompleto.getIdSeller().getMarketplace());
                }
            }

            ReportService.generarReporteUnico(productoCompleto, analisisCompleto);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error generando reporte: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    // ======================================================
    // LÓGICA INTERNA DE ANÁLISIS
    // ======================================================

    private void guardarAnalisis(Producto producto, ProductAnalysis analisisActual) {
        try {
            ProductAnalysis previo = productAnalysisDAO.findLastAnalysisByItem(producto.getItemId());

            if (previo != null) {
                previo.setAnalysisDate(analisisActual.getAnalysisDate());
                previo.setPriceActual(analisisActual.getPriceActual());
                previo.setPriceDifference(analisisActual.getPriceDifference());
                previo.setMarketAverage(analisisActual.getMarketAverage());
                previo.setMarketMin(analisisActual.getMarketMin());
                previo.setMarketMax(analisisActual.getMarketMax());
                previo.setStdDeviation(analisisActual.getStdDeviation());
                previo.setTrustScore(analisisActual.getTrustScore());
                previo.setIdSeller(analisisActual.getIdSeller());
                productAnalysisDAO.update(previo);
            } else {
                productAnalysisDAO.create(analisisActual);
            }
        } catch (Exception e) {
            System.err.println("❌ Error guardando análisis en BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Double> cargarPreciosMercado(String nombreProducto) {
        try {
            // caché
            List<Double> cached = marketPriceCache.get(nombreProducto);
            if (cached != null && !cached.isEmpty()) {
                System.out.println("♻ Precios mercado desde caché");
                return cached;
            }

            String query = construirQueryInteligente(nombreProducto);
            List<Double> precios = api.obtenerPreciosDelMercado(query, tokenAPI);
            if (precios != null && !precios.isEmpty()) {
                marketPriceCache.put(nombreProducto, precios);
            }
            return precios;
        } catch (Exception e) {
            System.err.println("⚠ Error cargando precios de mercado: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String construirQueryInteligente(String nombreProducto) {
        String[] stopWords = {"nuevo", "usado", "original", "garantía", "envío", "gratis",
                "new", "used", "free", "shipping"};
        String query = nombreProducto.toLowerCase();

        for (String stop : stopWords) {
            query = query.replaceAll("\\b" + stop + "\\b", "");
        }

        String[] palabras = query.trim().split("\\s+");
        int maxPalabras = Math.min(5, palabras.length);

        return String.join(" ", Arrays.copyOfRange(palabras, 0, maxPalabras)).trim();
    }

    private MarketStats calcularMarketStats(List<Double> precios, double precioActual) {
        MarketStats stats = new MarketStats();

        if (precios == null || precios.isEmpty()) {
            stats.promedio = 0;
            stats.min = 0;
            stats.max = 0;
            stats.desviacion = 0;
            stats.coefVariacion = 0;
            stats.cantidad = 0;
            stats.estabilidad = "Sin datos";
            stats.alertaPrecio = "Sin datos de mercado suficientes";
            stats.confianza = "Sin datos";
            return stats;
        }

        List<Double> filtrados = filtrarOutliers(precios);
        stats.cantidad = filtrados.size();

        DoubleSummaryStatistics summary = filtrados.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        stats.promedio = summary.getAverage();
        stats.min = summary.getMin();
        stats.max = summary.getMax();

        double var = filtrados.stream()
                .mapToDouble(p -> Math.pow(p - stats.promedio, 2))
                .average()
                .orElse(0);

        stats.desviacion = Math.sqrt(var);
        stats.coefVariacion = stats.promedio > 0 ? (stats.desviacion / stats.promedio) * 100 : 0;

        stats.estabilidad = stats.coefVariacion < 20
                ? "Alta estabilidad"
                : stats.coefVariacion < 40
                ? "Variabilidad moderada"
                : "Alta variabilidad";

        stats.confianza = calcularConfianzaAnalisis(stats.cantidad, stats.desviacion, stats.promedio);
        stats.alertaPrecio = detectarAlertaPrecio(precioActual, stats.promedio, stats.desviacion);

        return stats;
    }

    private List<Double> filtrarOutliers(List<Double> precios) {
        if (precios.size() < 4) return precios;

        List<Double> sorted = new ArrayList<>(precios);
        Collections.sort(sorted);

        int n = sorted.size();
        double q1 = sorted.get(n / 4);
        double q3 = sorted.get(3 * n / 4);
        double iqr = q3 - q1;

        double limiteInferior = q1 - 1.5 * iqr;
        double limiteSuperior = q3 + 1.5 * iqr;

        return precios.stream()
                .filter(p -> p >= limiteInferior && p <= limiteSuperior)
                .collect(Collectors.toList());
    }

    private String detectarAlertaPrecio(double precio, double promedio, double desviacion) {
        if (promedio == 0 || desviacion == 0) {
            return "Sin datos de mercado suficientes";
        }

        double zScore = (promedio - precio) / desviacion;

        if (zScore > 3) {
            double descuento = ((promedio - precio) / promedio) * 100;
            return "ALERTA: Precio sospechosamente bajo (-" +
                    String.format("%.0f", descuento) + "%)";
        } else if (zScore > 2) {
            return "ADVERTENCIA: Precio muy por debajo del mercado";
        } else if (precio < promedio * 0.7) {
            return "OPORTUNIDAD: Precio 30% por debajo del promedio";
        } else if (precio > promedio * 1.3) {
            return "PRECIO ALTO: 30% por encima del promedio";
        } else if (Math.abs(precio - promedio) < desviacion * 0.5) {
            return "Precio justo, dentro del rango normal";
        }

        return "Precio aceptable";
    }

    private String calcularConfianzaAnalisis(int cantidadPrecios,
                                             double desviacionEstandar,
                                             double promedio) {
        if (promedio == 0) return "Sin datos";

        double coefVariacion = (desviacionEstandar / promedio) * 100;

        if (cantidadPrecios < 5) {
            return "Muy Baja (muestra insuficiente)";
        } else if (cantidadPrecios < 15) {
            return "Baja (muestra pequeña)";
        } else if (coefVariacion > 50) {
            return "Media (alta variabilidad)";
        } else if (cantidadPrecios >= 30 && coefVariacion < 30) {
            return "Alta (muestra robusta)";
        } else if (cantidadPrecios >= 20 && coefVariacion < 40) {
            return "Buena";
        }

        return "Media";
    }

    private static class AntiguedadTrust {
        String antiguedad;
        double trustScore;
    }

    private AntiguedadTrust obtenerAntiguedadYTrustScore(Seller vendedor,
                                                         double precioActual,
                                                         double promedioMercado,
                                                         double desviacion) {
        AntiguedadTrust at = new AntiguedadTrust();
        try {
            Map<String, String> infoAntiguedad =
                    browseEBAY.calcularAccountAge(tokenAPI, vendedor.getUsername());
            String antiguedad = infoAntiguedad.getOrDefault("antiguedad", "Desconocida");
            at.antiguedad = antiguedad;

            double score = calcularTrustScoreMejorado(
                    precioActual,
                    promedioMercado,
                    desviacion,
                    vendedor,
                    antiguedad
            );
            at.trustScore = score;

        } catch (Exception e) {
            at.antiguedad = "No disponible";
            at.trustScore = 0;
        }
        return at;
    }

    private double calcularTrustScoreMejorado(double precio, double promedioMercado,
                                              double desviacion, Seller vendedor,
                                              String antiguedad) {
        double score = 0;

        // 1. Precio competitivo (35 pts)
        if (desviacion > 0 && promedioMercado > 0) {
            double zScore = Math.abs((precio - promedioMercado) / desviacion);

            if (zScore <= 1) score += 35;
            else if (zScore <= 2) score += 25;
            else if (zScore <= 3) score += 10;
        }

        // 2. Feedback positivo (30 pts)
        double feedbackPct = vendedor.getFeedbackPorcentage();
        if (feedbackPct >= 99) score += 30;
        else if (feedbackPct >= 98) score += 27;
        else if (feedbackPct >= 95) score += 22;
        else if (feedbackPct >= 90) score += 15;
        else if (feedbackPct >= 80) score += 5;

        // 3. Volumen de feedback (20 pts)
        int feedbackScore = vendedor.getFeedbackScore();
        if (feedbackScore >= 10000) score += 20;
        else if (feedbackScore >= 5000) score += 18;
        else if (feedbackScore >= 1000) score += 15;
        else if (feedbackScore >= 500) score += 12;
        else if (feedbackScore >= 100) score += 8;
        else if (feedbackScore >= 50) score += 5;
        else if (feedbackScore >= 10) score += 2;

        // 4. Antigüedad (15 pts)
        try {
            if (antiguedad.contains("año")) {
                String[] parts = antiguedad.split(" ");
                int años = Integer.parseInt(parts[0]);
                score += Math.min(años * 2.5, 15);
            } else if (antiguedad.contains("mes")) {
                String[] parts = antiguedad.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].contains("mes") && i > 0) {
                        int meses = Integer.parseInt(parts[i - 1]);
                        score += Math.min(meses * 0.5, 6);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        return Math.min(100, Math.round(score * 10) / 10.0);
    }

    private TendenciaInfo analizarTendencia(List<PriceHistory> historial) {
        TendenciaInfo t = new TendenciaInfo();
        if (historial == null || historial.size() < 2) {
            t.cambioPorcentual = 0;
            t.volatilidad = 0;
            t.icono = "";
            t.descripcionTendencia = "Sin datos suficientes";
            t.descripcionVolatilidad = "N/A";
            return t;
        }

        historial.sort(Comparator.comparing(PriceHistory::getRecordedAt));

        double precioInicial = historial.get(0).getPrice();
        double precioFinal = historial.get(historial.size() - 1).getPrice();
        double cambio = ((precioFinal - precioInicial) / precioInicial) * 100;
        t.cambioPorcentual = cambio;

        double[] precios = historial.stream()
                .mapToDouble(PriceHistory::getPrice)
                .toArray();

        double volatilidad = calcularVolatilidad(precios);
        t.volatilidad = volatilidad;

        if (cambio > 10) {
            t.icono = "";
            t.descripcionTendencia = "Tendencia alcista fuerte";
        } else if (cambio > 5) {
            t.icono = "";
            t.descripcionTendencia = "Tendencia al alza";
        } else if (cambio < -10) {
            t.icono = "";
            t.descripcionTendencia = "Tendencia bajista fuerte";
        } else if (cambio < -5) {
            t.icono = "";
            t.descripcionTendencia = "Tendencia a la baja";
        } else {
            t.icono = "";
            t.descripcionTendencia = "Precio estable";
        }

        if (volatilidad < 5) {
            t.descripcionVolatilidad = "Baja volatilidad (precio estable)";
        } else if (volatilidad < 15) {
            t.descripcionVolatilidad = "Volatilidad moderada";
        } else {
            t.descripcionVolatilidad = "Alta volatilidad (precio inestable)";
        }

        return t;
    }

    private double calcularVolatilidad(double[] precios) {
        if (precios.length < 2) return 0;

        double suma = 0;
        for (int i = 1; i < precios.length; i++) {
            double cambio = (precios[i] - precios[i - 1]) / precios[i - 1];
            suma += cambio * cambio;
        }

        return Math.sqrt(suma / (precios.length - 1)) * 100;
    }
    private void registrarNuevoPrecio(Producto item, double newPrice) {

        // Obtener último precio guardado
        PriceHistory ultimo = priceHistoryDAO.findLatestByItemId(item.getItemId());

        // Evitar guardar precios iguales
        if (ultimo != null && ultimo.getPrice() == newPrice) {
            return; // no crear duplicados
        }

        // Crear nuevo registro
        PriceHistory ph = new PriceHistory();
        ph.setItemId(item.getItemId());
        ph.setPrice(newPrice);
        ph.setCurrency("USD");
        ph.setRecordedAt(Instant.now().toString());

        priceHistoryDAO.create(ph);

        System.out.println("Nuevo historial guardado para " + item.getName() + ": " + newPrice);
    }

    private Double obtenerPrecioDesdeAPI(String itemId) {
        try {
            // Usa tu clase conect_API_eBay para obtener el precio actual
            return api.obtenerPrecioActual(itemId, tokenAPI);
        } catch (Exception e) {
            System.err.println("Error al obtener precio desde la API: " + e.getMessage());
            return null;
        }
    }

    private void guardarNuevoPrecioSiCambio(Producto item, double nuevoPrecio) {

        // Obtiene el último precio guardado en price_history
        PriceHistory ultimo = priceHistoryDAO.findLatestByItemId(item.getItemId());

        // Si no existe historial → lo crea
        if (ultimo == null) {
            registrarNuevoPrecio(item, nuevoPrecio);
            return;
        }

        // Si el precio es igual → no guardes nada
        if (ultimo.getPrice() == nuevoPrecio) {
            System.out.println("El precio no cambió. No se guarda historial nuevo.");
            return;
        }

        // Si el precio cambió → guarda un nuevo registro
        registrarNuevoPrecio(item, nuevoPrecio);
    }

    public List<TopProductoDTO> obtenerTop5API(String nombreProducto) {


        System.out.println("Buscando productos similares por modelo: " + nombreProducto);

        List<JsonObject> items = api.buscarProductosSimilares(nombreProducto, tokenAPI);

        List<TopProductoDTO> lista = new ArrayList<>();

        for (JsonObject item : items) {
            try {
                String title = item.has("title") ? item.get("title").getAsString() : "Sin nombre";

                JsonObject priceObj = item.getAsJsonObject("price");
                if (priceObj == null) continue;

                double price = priceObj.get("value").getAsDouble();

                TopProductoDTO dto = new TopProductoDTO();
                dto.titulo = title;
                dto.precio = price;

                lista.add(dto);

            } catch (Exception ignored) {}
        }

        // Ordenar por precio ASC
        return lista.stream()
                .sorted(Comparator.comparingDouble(o -> o.precio))
                .limit(5)
                .toList();
    }
    public static class TopProductoDTO {
        public String titulo;
        public double precio;
    }
    public List<TopProductoDTO> obtenerTop5NegativosAPI(String nombreProducto) {
        System.out.println("Buscando productos similares (caros) por modelo: " + nombreProducto);

            List<JsonObject> items = api.buscarProductosSimilares(nombreProducto, tokenAPI);

        List<TopProductoDTO> lista = new ArrayList<>();

        for (JsonObject item : items) {
            try {
                String title = item.has("title") ? item.get("title").getAsString() : "Sin nombre";

                JsonObject priceObj = item.getAsJsonObject("price");
                if (priceObj == null) continue;

                double price = priceObj.get("value").getAsDouble();

                TopProductoDTO dto = new TopProductoDTO();
                dto.titulo = title;
                dto.precio = price;

                lista.add(dto);

            } catch (Exception ignored) {}
        }

        // Ordenar por precio DESC (más caros primero)
        return lista.stream()
                .sorted((a, b) -> Double.compare(b.precio, a.precio))
                .limit(5)
                .toList();
    }
    public boolean existeEnWishlist(int userId, String itemId) {
        WishlistDAO dao = new WishlistDAO();
        return dao.existsWishlist(userId, itemId);
    }
    public boolean guardarEnWishlist(Producto producto, int userId) {

        WishlistDAO wishlistDAO = new WishlistDAO();
        ProductDAO productDAO = new ProductDAO();

        // Asegurar que el producto exista
        Producto pBD = productDAO.findByItemId(producto.getItemId());
        if (pBD == null) {
            return false; // o guardarlo primero si quieres
        }

        boolean existe = wishlistDAO.existsWishlist(userId, producto.getItemId());
        if (existe) return false;

        WishlistProduct wp = new WishlistProduct();
        wp.setIdUser(Sesion.getUsuario());
        wp.setIdItem(pBD);

        wishlistDAO.create(wp);
        return true;
    }

}


