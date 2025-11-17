package interfaz.controllers;

import api.conect_API_eBay;
import dao.*;
import entities.*;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import utils.NotificationManager;
import utils.ReportService;
import utils.Sesion;
import utils.cls_browseEBAY;

import java.awt.*;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Controlador optimizado del módulo de análisis de producto.
 * Usa multithreading y caché para mejorar el rendimiento.
 *
 * @author Kevin
 */
public class ProductAnalysisController {
    @FXML private BorderPane togleAnalys;
    @FXML private VBox content;
    @FXML private Button btnGenerarReporte;
    @FXML private ProgressIndicator progressIndicator;
    private MainController mainController;

    // 🖼️ Interfaz
    @FXML private ImageView productImage;
    @FXML private Text lblTitle;

    // 🔹 TextFlow - Precio / Cupón
    @FXML private Text txtPrecioActual;
    @FXML private Text txtPromedio;
    @FXML private Text txtDiferencia;

    // 🔹 TextFlow - Vendedor
    @FXML private Text txtVendedorNombre;
    @FXML private Text txtFeedback;
    @FXML private Text txtFeedbackScore;
    @FXML private Text txtAntiguedad;
    @FXML private Text txtTrustScore;

    // TextFlow - Precio de mercado
    @FXML private Text txtPromedioMercado;
    @FXML private Text txtRango;
    @FXML private Text txtEstabilidad;

    // 📊 Estadísticas Generales
    @FXML private Text txtTotalProductos;
    @FXML private Text txtPromedioVariacionGeneral;
    @FXML private Text txtTopVariacionesPositivas;
    @FXML private Text txtTopVariacionesNegativas;
    @FXML private Text txtResumenConsultasDiarias;

    // 🔹 Gráfico
    @FXML private LineChart<String, Number> chartHistorial;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // 🔧 Variables internas
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String token = Sesion.getTokenAPI();
    private final conect_API_eBay api = new conect_API_eBay();
    private final cls_browseEBAY browseEBAY = new cls_browseEBAY();

    // Variables para guardar el análisis
    private Producto productoActual;
    private ProductAnalysis analisisActual;
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // 🔹 Pool de hilos para tareas asíncronas
    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // 🔹 Caché de imágenes
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    // 🔹 Caché de análisis de mercado (evita consultas repetidas)
    private static final Map<String, List<Double>> marketPriceCache = new ConcurrentHashMap<>();

    private AcordPanelController acordController;

    public void setAcordController(AcordPanelController controller) {
        this.acordController = controller;
    }

    @FXML
    public void initialize() {
        // Hover limpio
        togleAnalys.setOnMouseEntered(e -> {
            togleAnalys.setStyle(
                    "-fx-cursor: hand;" +
                            "-fx-translate-y: -3;"
            );
        });

        togleAnalys.setOnMouseExited(e -> {
            togleAnalys.setStyle(
                    "-fx-cursor: hand;" +
                            "-fx-effect: none;" +
                            "-fx-translate-y: 0;"
            );
        });

        // Ocultar indicador de progreso inicialmente
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    /**
     * Carga y analiza un producto de forma OPTIMIZADA usando multithreading
     */
    public void cargarProducto(String itemId) {
        // Mostrar indicador de carga
        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
        }

        // 🔹 Crear tarea asíncrona
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                cargarProductoAsync(itemId);
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            if (progressIndicator != null) {
                progressIndicator.setVisible(false);
            }
            System.out.println("✅ Análisis completado");
            cargarEstadisticas();
        });

        loadTask.setOnFailed(e -> {
            if (progressIndicator != null) {
                progressIndicator.setVisible(false);
            }
            System.err.println("❌ Error en análisis: " + loadTask.getException().getMessage());
        });

        // Ejecutar en hilo separado
        new Thread(loadTask).start();
    }

    /**
     * Carga el producto de forma asíncrona con múltiples hilos paralelos
     */
    private void cargarProductoAsync(String itemId) throws Exception {
        ProductDAO productDAO = new ProductDAO();
        Producto productoBD = productDAO.findByItemId(itemId);

        if (productoBD == null) {
            Platform.runLater(() -> txtPrecioActual.setText("Producto no encontrado en la base de datos."));
            return;
        }

        this.productoActual = productoBD;
        this.analisisActual = new ProductAnalysis();
        analisisActual.setItem(productoBD);
        analisisActual.setAnalysisDate(Instant.now());

        // 🔹 Mostrar título inmediatamente
        Platform.runLater(() -> lblTitle.setText(productoBD.getName()));

        // ═══════════════════════════════════════════════════════
        // 🚀 EJECUCIÓN PARALELA DE TAREAS INDEPENDIENTES
        // ═══════════════════════════════════════════════════════

        CompletableFuture<Void> imagenFuture = CompletableFuture.runAsync(() ->
                cargarImagen(itemId, productoBD), executorService);

        CompletableFuture<PriceHistory> precioFuture = CompletableFuture.supplyAsync(() ->
                cargarPrecioActual(itemId), executorService);

        CompletableFuture<CouponPro> cuponFuture = CompletableFuture.supplyAsync(() ->
                cargarCupon(itemId), executorService);

        CompletableFuture<Seller> vendedorFuture = CompletableFuture.supplyAsync(() ->
                cargarVendedor(productoBD), executorService);

        CompletableFuture<List<Double>> mercadoFuture = CompletableFuture.supplyAsync(() ->
                cargarPreciosMercado(productoBD.getName()), executorService);

        CompletableFuture<List<PriceHistory>> historialFuture = CompletableFuture.supplyAsync(() ->
                cargarHistorial(itemId), executorService);

        // ═══════════════════════════════════════════════════════
        // 🔹 ESPERAR A QUE TODAS LAS TAREAS TERMINEN
        // ═══════════════════════════════════════════════════════

        try {
            PriceHistory precio = precioFuture.get(5, TimeUnit.SECONDS);
            CouponPro cupon = cuponFuture.get(3, TimeUnit.SECONDS);
            Seller vendedor = vendedorFuture.get(5, TimeUnit.SECONDS);
            List<Double> preciosMercado = mercadoFuture.get(10, TimeUnit.SECONDS);
            List<PriceHistory> historial = historialFuture.get(5, TimeUnit.SECONDS);

            // Procesar resultados
            procesarPrecio(precio);
            procesarCupon(cupon);
            procesarVendedor(vendedor, precio != null ? precio.getPrice() : 0.0);
            procesarMercado(preciosMercado, precio != null ? precio.getPrice() : 0.0);
            procesarHistorial(historial);

            // Guardar análisis
            guardarAnalisisEnBD();

        } catch (TimeoutException e) {
            System.err.println("⚠️ Timeout en alguna operación, continuando con datos parciales");
        } catch (Exception e) {
            System.err.println("❌ Error procesando análisis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    // 🔹 MÉTODOS AUXILIARES OPTIMIZADOS
    // ═══════════════════════════════════════════════════════

    private void cargarImagen(String itemId, Producto producto) {
        try {
            // Verificar caché primero
            Image cachedImage = imageCache.get(itemId);
            if (cachedImage != null) {
                Platform.runLater(() -> productImage.setImage(cachedImage));
                return;
            }

            ImagesProductDAO imageDAO = new ImagesProductDAO();
            String urlImagen = imageDAO.findMainImageByProduct(producto);

            if (urlImagen != null && !urlImagen.isEmpty()) {
                Image img = new Image(urlImagen, true);
                imageCache.put(itemId, img); // Guardar en caché
                Platform.runLater(() -> productImage.setImage(img));
            } else {
                Image defaultImg = new Image("/recursos/img/no-image.png");
                Platform.runLater(() -> productImage.setImage(defaultImg));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando imagen: " + e.getMessage());
        }
    }

    private PriceHistory cargarPrecioActual(String itemId) {
        try {
            PriceHistoryDAO priceDAO = new PriceHistoryDAO();
            return priceDAO.findLatestByItemId(itemId);
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando precio: " + e.getMessage());
            return null;
        }
    }

    private CouponPro cargarCupon(String itemId) {
        try {
            CouponProDAO couponDAO = new CouponProDAO();
            return couponDAO.findByItemId(itemId);
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando cupón: " + e.getMessage());
            return null;
        }
    }

    private Seller cargarVendedor(Producto producto) {
        try {
            if (producto.getIdSeller() == null) return null;
            SellerDAO sellerDAO = new SellerDAO();
            return sellerDAO.getUserById(producto.getIdSeller().getId());
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando vendedor: " + e.getMessage());
            return null;
        }
    }

    private List<Double> cargarPreciosMercado(String nombreProducto) {
        try {
            // Verificar caché primero
            List<Double> cached = marketPriceCache.get(nombreProducto);
            if (cached != null && !cached.isEmpty()) {
                System.out.println("♻️ Precios de mercado obtenidos desde caché");
                return cached;
            }

            List<Double> precios = api.obtenerPreciosDelMercado(nombreProducto, token);

            // Guardar en caché (válido por esta sesión)
            if (precios != null && !precios.isEmpty()) {
                marketPriceCache.put(nombreProducto, precios);
            }

            return precios;
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando precios de mercado: " + e.getMessage());
            return null;
        }
    }

    private List<PriceHistory> cargarHistorial(String itemId) {
        try {
            PriceHistoryDAO priceDAO = new PriceHistoryDAO();
            return priceDAO.findAllByItemId(itemId);
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando historial: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════
    // 🔹 PROCESADORES DE DATOS
    // ═══════════════════════════════════════════════════════

    private void procesarPrecio(PriceHistory precio) {
        double precioVal = (precio != null) ? precio.getPrice() : 0.0;
        String moneda = (precio != null) ? precio.getCurrency() : "USD";

        Platform.runLater(() ->
                txtPrecioActual.setText("Precio actual: " + moneda + " " + String.format("%.2f", precioVal))
        );

        analisisActual.setPriceActual(precioVal);
    }

    private void procesarCupon(CouponPro cupon) {
        Platform.runLater(() -> {
            if (cupon != null) {
                txtPromedio.setText("Cupón: " + cupon.getCouponRedemption() + "\n");
                txtDiferencia.setText("Válido hasta: " + cupon.getExpirationAt() + "\n");
            } else {
                txtPromedio.setText("\nSin cupones activos\n");
                txtDiferencia.setText("");
            }
        });
    }

    private void procesarVendedor(Seller vendedor, double precioActual) {
        analisisActual.setIdSeller(vendedor);

        if (vendedor == null) {
            Platform.runLater(() -> {
                txtVendedorNombre.setText("Vendedor: No registrado en BD\n");
                txtFeedback.setText("Feedback: N/A\n");
                txtAntiguedad.setText("Antigüedad: N/A\n");
                txtTrustScore.setText("TrustScore: N/A\n");
            });
            return;
        }

        Platform.runLater(() -> {
            txtVendedorNombre.setText("Vendedor: " + vendedor.getUsername() + "\n");
            txtFeedback.setText("Feedback positivo: " +
                    String.format("%.2f", vendedor.getFeedbackPorcentage()) + "%\n");
            txtFeedbackScore.setText("Feedback Puntuacion: " + vendedor.getFeedbackScore() + "\n");
        });

        // Calcular antigüedad en segundo plano
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> infoAntiguedad = cls_browseEBAY.calcularAccountAge(token, vendedor.getUsername());
                String antiguedad = infoAntiguedad.getOrDefault("antiguedad", "Desconocida");

                Platform.runLater(() ->
                        txtAntiguedad.setText("Antigüedad: " + antiguedad + "\n")
                );

                // Calcular TrustScore
                double trustScore = calcularTrustScore(
                        precioActual, 1000,
                        String.valueOf(vendedor.getFeedbackPorcentage()),
                        String.valueOf(vendedor.getFeedbackScore()),
                        antiguedad
                );

                Platform.runLater(() ->
                        txtTrustScore.setText("TrustScore: " + String.format("%.1f", trustScore) + " / 100\n")
                );

                analisisActual.setTrustScore(trustScore);
            } catch (Exception e) {
                Platform.runLater(() -> txtAntiguedad.setText("Antigüedad: No disponible\n"));
            }
        }, executorService);
    }

    private void procesarMercado(List<Double> precios, double precioActual) {
        if (precios == null || precios.isEmpty()) {
            Platform.runLater(() -> {
                txtPromedioMercado.setText("Promedio: --\n");
                txtRango.setText("Rango: $-- a $--\n");
                txtEstabilidad.setText("Desviación estándar: --\n");
            });
            return;
        }

        // Filtrar outliers
        List<Double> preciosFiltrados = filtrarOutliers(precios);

        double promedio = preciosFiltrados.stream().mapToDouble(d -> d).average().orElse(0);
        double min = preciosFiltrados.stream().min(Double::compare).orElse(0.0);
        double max = preciosFiltrados.stream().max(Double::compare).orElse(0.0);

        double variance = preciosFiltrados.stream()
                .mapToDouble(p -> Math.pow(p - promedio, 2))
                .average()
                .orElse(0);

        double desviacion = Math.sqrt(variance);
        double diferencia = precioActual - promedio;
        double coefVariacion = promedio > 0 ? (desviacion / promedio) * 100 : 0;

        String estabilidad = coefVariacion < 20 ? "Alta estabilidad" :
                coefVariacion < 40 ? "Variabilidad moderada" :
                        "Alta variabilidad";

        String confianza = calcularConfianzaAnalisis(preciosFiltrados.size(), desviacion, promedio);
        String alertaPrecio = detectarAlertaPrecio(precioActual, promedio, desviacion);

        analisisActual.setMarketAverage(promedio);
        analisisActual.setMarketMin(min);
        analisisActual.setMarketMax(max);
        analisisActual.setStdDeviation(desviacion);
        analisisActual.setPriceDifference(diferencia);

        Platform.runLater(() -> {
            txtPromedioMercado.setText(String.format("Promedio mercado: USD %.2f\n", promedio));
            txtRango.setText(String.format("Rango: USD %.2f – %.2f\n", min, max));
            txtEstabilidad.setText(String.format(
                    "Desviación: %.2f (%.1f%%)\n%s\n" +
                            "Productos analizados: %d\n" +
                            "Confianza del análisis: %s\n" +
                            "Estado del precio: %s\n",
                    desviacion, coefVariacion, estabilidad, preciosFiltrados.size(), confianza, alertaPrecio
            ));
        });
    }
    private void procesarHistorial(List<PriceHistory> historial) {
        Platform.runLater(() -> llenarGraficoHistorialReal(historial));
    }

    // ═══════════════════════════════════════════════════════
    // 🔹 MÉTODOS EXISTENTES (sin cambios significativos)
    // ═══════════════════════════════════════════════════════

    private double calcularTrustScore(double precio, double promedio, String feedback, String feedbackScore, String antiguedad) {
        double score = 0;

        double diff = Math.abs(precio - promedio);
        double scorePrecio = Math.max(0, 40 - (diff / promedio * 100));
        score += Math.max(0, Math.min(scorePrecio, 40));

        try {
            double feedbackVal = Double.parseDouble(feedback);
            score += (feedbackVal / 100) * 40;
        } catch (Exception ignored) {}

        try {
            int fs = Integer.parseInt(feedbackScore);
            if (fs >= 1000) score += 20;
            else if (fs >= 200) score += 15;
            else if (fs >= 50) score += 10;
            else if (fs >= 10) score += 5;
            else if (fs > 0) score += 2;
        } catch (Exception ignored) {}

        if (antiguedad.matches(".*(\\d+) año.*")) {
            int años = Integer.parseInt(antiguedad.replaceAll("\\D+", ""));
            score += Math.min(años * 4, 20);
        }

        return Math.min(100, score);
    }

    private void llenarGraficoHistorialReal(List<PriceHistory> historial) {
        chartHistorial.getData().clear();

        if (historial == null || historial.isEmpty()) {
            System.out.println("⚠ No hay historial de precios para graficar.");
            return;
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Historial de precios");

        for (PriceHistory ph : historial) {
            try {
                String fecha = ph.getRecordedAt().substring(0, 10);
                double precio = ph.getPrice();
                serie.getData().add(new XYChart.Data<>(fecha, precio));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        chartHistorial.getData().add(serie);
    }

    private void guardarAnalisisEnBD() {
        if (analisisActual == null || productoActual == null) {
            System.err.println("❌ No hay análisis para guardar");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                ProductAnalysisDAO dao = new ProductAnalysisDAO();

                ProductAnalysis previo = dao.findLastAnalysisByItem(productoActual.getItemId());

                if (previo != null) {
                    // actualizar
                    previo.setAnalysisDate(analisisActual.getAnalysisDate());
                    previo.setPriceActual(analisisActual.getPriceActual());
                    previo.setPriceDifference(analisisActual.getPriceDifference());
                    previo.setMarketAverage(analisisActual.getMarketAverage());
                    previo.setMarketMin(analisisActual.getMarketMin());
                    previo.setMarketMax(analisisActual.getMarketMax());
                    previo.setStdDeviation(analisisActual.getStdDeviation());
                    previo.setTrustScore(analisisActual.getTrustScore());
                    previo.setIdSeller(analisisActual.getIdSeller());

                    dao.update(previo);

                    System.out.println("♻️ Análisis actualizado para producto: " + productoActual.getItemId());
                } else {
                    // crear nuevo únicamente si no existe
                    dao.create(analisisActual);
                    System.out.println("🆕 Nuevo análisis guardado en BD para producto: " + productoActual.getItemId());
                }

            } catch (Exception e) {
                System.err.println("❌ Error guardando análisis: " + e.getMessage());
                e.printStackTrace();
            }
        }, executorService);
    }


    private void cargarEstadisticas() {
        ProductAnalysisDAO dao = new ProductAnalysisDAO();

        try {
            // RF-061: Total de productos analizados
            int total = dao.countAll();
            Platform.runLater(() ->
                    txtTotalProductos.setText("Total analizados: " + total)
            );

            // RF-062: Promedio general de variación (price_difference)
            double promedioVariacion = dao.getPromedioVariacionGeneral();
            Platform.runLater(() ->
                    txtPromedioVariacionGeneral.setText(
                            "Promedio de variación: " + String.format("%.2f", promedioVariacion)
                    )
            );

            // RF-063: Top 5 variaciones positivas
            var topPos = dao.getTopVariacionesPositivas(5);
            StringBuilder posText = new StringBuilder();
            for (var pa : topPos) {
                posText.append(pa.getItem().getName())
                        .append(" → +")
                        .append(String.format("%.2f", pa.getPriceDifference()))
                        .append("\n");
            }
            Platform.runLater(() ->
                    txtTopVariacionesPositivas.setText(posText.toString())
            );

            // RF-064: Top 5 variaciones negativas
            var topNeg = dao.getTopVariacionesNegativas(5);
            StringBuilder negText = new StringBuilder();
            for (var pa : topNeg) {
                negText.append(pa.getItem().getName())
                        .append(" → ")
                        .append(String.format("%.2f", pa.getPriceDifference()))
                        .append("\n");
            }
            Platform.runLater(() ->
                    txtTopVariacionesNegativas.setText(negText.toString())
            );

            // RF-065: Resumen diario (fecha → consultas)
            var resumen = dao.getResumenDiarioConsultas();
            StringBuilder resumenText = new StringBuilder();
            for (Object[] row : resumen) {
                String fecha = String.valueOf(row[0]);
                long cantidad = ((Number) row[1]).longValue();
                resumenText.append(fecha)
                        .append(" → ")
                        .append(cantidad)
                        .append(" consultas\n");
            }
            Platform.runLater(() ->
                    txtResumenConsultasDiarias.setText(resumenText.toString())
            );

        } catch (Exception e) {
            System.err.println("❌ Error cargando estadísticas: " + e.getMessage());
        }
    }

    @FXML
    private void onReportGenerated() {
        System.out.println("🔍 Iniciando generación de reporte...");

        if (productoActual == null) {
            NotificationManager.error("❌ No hay producto cargado");
            return;
        }

        if (analisisActual == null) {
            NotificationManager.error("❌ No hay análisis disponible");
            return;
        }

        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
        }

        Task<Boolean> reportTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                EntityManager em = null;
                try {
                    System.out.println("📦 Inicializando todo dentro de la sesión de Hibernate...");

                    // Crear EntityManager para mantener sesión abierta
                    em = genericDAO.getEmf().createEntityManager();

                    ProductDAO productDAO = new ProductDAO();

                    // Obtener producto con todas las relaciones
                    Producto productoCompleto = productDAO.findByItemIdWithFullDetails(
                            productoActual.getItemId()
                    );

                    if (productoCompleto == null) {
                        System.err.println("❌ No se pudo obtener el producto");
                        return false;
                    }

                    // También inicializar el análisis dentro de la sesión
                    ProductAnalysis analisisCompleto = em.merge(analisisActual);

                    // Forzar inicialización del vendedor en el análisis
                    if (analisisCompleto.getIdSeller() != null) {
                        org.hibernate.Hibernate.initialize(analisisCompleto.getIdSeller());
                        if (analisisCompleto.getIdSeller().getMarketplace() != null) {
                            org.hibernate.Hibernate.initialize(analisisCompleto.getIdSeller().getMarketplace());
                        }
                    }

                    System.out.println("✅ Todo inicializado correctamente");
                    System.out.println("🔧 Generando reporte PDF...");

                    // Generar el reporte
                    ReportService.generarReporteUnico(productoCompleto, analisisCompleto);

                    return true;

                } catch (Exception e) {
                    System.err.println("❌ Error en generación de reporte:");
                    e.printStackTrace();
                    return false;
                } finally {
                    if (em != null && em.isOpen()) {
                        em.close();
                    }
                }
            }

            @Override
            protected void succeeded() {
                if (progressIndicator != null) {
                    progressIndicator.setVisible(false);
                }

                Boolean resultado = getValue();
                if (resultado != null && resultado) {
                    System.out.println("Reporte generado exitosamente");
                    NotificationManager.success("Reporte guardado en Descargas");
                } else {
                    NotificationManager.error("Error al generar el reporte");
                }
            }

            @Override
            protected void failed() {
                if (progressIndicator != null) {
                    progressIndicator.setVisible(false);
                }

                Throwable exception = getException();
                if (exception != null) {
                    exception.printStackTrace();
                }

                NotificationManager.error("Error al generar el reporte PDF");
            }
        };

        Thread thread = new Thread(reportTask);
        thread.setDaemon(true);
        thread.start();
    }
    @FXML
    private void onClicked() {
        content.setVisible(!content.isVisible());
    }

    @FXML
    private void visitURL() {
        if (productoActual == null || productoActual.getUrlProduct() == null) {
            NotificationManager.warning("⚠️ Este producto no tiene una URL válida.");
            return;
        }

        try {
            String url = productoActual.getUrlProduct();
            Desktop.getDesktop().browse(new URI(url));
            System.out.println("🌐 Abriendo en navegador: " + url);

        } catch (Exception e) {
            NotificationManager.error("❌ No se pudo abrir el enlace.");
            System.err.println("Error abriendo URL: " + e.getMessage());
        }
    }

    @FXML
    protected void onDeletedWish() {

        if (productoActual == null) {
            NotificationManager.warning("⚠️ No hay producto cargado.");
            return;
        }

        try {
            WishlistDAO wishlistDAO = new WishlistDAO();
            WishlistProduct wp = wishlistDAO.findByItemId(productoActual.getItemId());

            if (wp == null) {
                NotificationManager.info("Este producto no está en tu lista.");
                return;
            }

            wishlistDAO.delete(wp);
            NotificationManager.success("Eliminado de la lista de guardados.");

            // Actualizar contador visual (si lo usas)
            actualizarNumeroGuardados();
            if (acordController != null) {
                Platform.runLater(() -> acordController.recargarLista());
            }


        } catch (Exception e) {
            NotificationManager.error("Error eliminando de tu wishlist.");
            e.printStackTrace();
        }
    }


    /**
     * Limpia recursos al cerrar
     */
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void actualizarNumeroGuardados() {
        try {
            WishlistDAO dao = new WishlistDAO();
            int count = dao.countByUser(Sesion.getUsuario().getId());

            // Notifica al MainController
            if (mainController != null) {
                Platform.runLater(() -> mainController.actualizarWishlistCount(count));
            }

        } catch (Exception e) {
            System.err.println("No se pudo actualizar el contador de wishlist");
        }
    }

// ═══════════════════════════════════════════════════════
// 🆕 MÉTODOS DE ANÁLISIS MEJORADO - AGREGAR AL FINAL
// ═══════════════════════════════════════════════════════

    /**
     * 📊 Analiza el mercado completo con filtros mejorados
     */
    private Map<String, Object> analizarMercadoCompleto(String nombreProducto, String condicion) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            // Verificar caché primero
            List<Double> cached = marketPriceCache.get(nombreProducto);
            List<Double> precios;

            if (cached != null && !cached.isEmpty()) {
                System.out.println("♻️ Precios de mercado obtenidos desde caché");
                precios = cached;
            } else {
                // Construir query inteligente
                String queryMejorada = construirQueryInteligente(nombreProducto);

                // Obtener precios del mercado
                precios = api.obtenerPreciosDelMercado(queryMejorada, token);

                if (precios != null && !precios.isEmpty()) {
                    marketPriceCache.put(nombreProducto, precios);
                }
            }

            if (precios == null || precios.isEmpty()) {
                resultado.put("promedio", 0.0);
                resultado.put("min", 0.0);
                resultado.put("max", 0.0);
                resultado.put("desviacion", 0.0);
                resultado.put("cantidad", 0);
                return resultado;
            }

            // Filtrar outliers extremos
            List<Double> preciosFiltrados = filtrarOutliers(precios);

            // Calcular estadísticas
            DoubleSummaryStatistics stats = preciosFiltrados.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            double promedio = stats.getAverage();
            double varianza = preciosFiltrados.stream()
                    .mapToDouble(p -> Math.pow(p - promedio, 2))
                    .average()
                    .orElse(0);
            double desviacion = Math.sqrt(varianza);

            resultado.put("promedio", Math.round(promedio * 100.0) / 100.0);
            resultado.put("min", Math.round(stats.getMin() * 100.0) / 100.0);
            resultado.put("max", Math.round(stats.getMax() * 100.0) / 100.0);
            resultado.put("desviacion", Math.round(desviacion * 100.0) / 100.0);
            resultado.put("cantidad", preciosFiltrados.size());

        } catch (Exception e) {
            System.err.println("Error en análisis de mercado: " + e.getMessage());
            resultado.put("promedio", 0.0);
            resultado.put("min", 0.0);
            resultado.put("max", 0.0);
            resultado.put("desviacion", 0.0);
            resultado.put("cantidad", 0);
        }

        return resultado;
    }

    /**
     * 🔍 Construye una query inteligente eliminando ruido
     */
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

    /**
     * 📉 Filtra outliers usando método IQR
     */
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

    /**
     * 🚨 Detecta si el precio es sospechoso
     */
    private String detectarAlertaPrecio(double precio, double promedio, double desviacion) {
        if (promedio == 0 || desviacion == 0) {
            return "⚪ Sin datos de mercado suficientes";
        }

        double zScore = (promedio - precio) / desviacion;

        if (zScore > 3) {
            double descuento = ((promedio - precio) / promedio) * 100;
            return "🚨 ALERTA: Precio sospechosamente bajo (-" +
                    String.format("%.0f", descuento) + "%)";
        } else if (zScore > 2) {
            return "⚠️ ADVERTENCIA: Precio muy por debajo del mercado";
        } else if (precio < promedio * 0.7) {
            return "💡 OPORTUNIDAD: Precio 30% por debajo del promedio";
        } else if (precio > promedio * 1.3) {
            return "📈 PRECIO ALTO: 30% por encima del promedio";
        } else if (Math.abs(precio - promedio) < desviacion * 0.5) {
            return "✅ Precio justo, dentro del rango normal";
        }

        return "🟡 Precio aceptable";
    }

    /**
     * 🔍 Evalúa confiabilidad del vendedor
     */
    private String evaluarConfiabilidadVendedor(Seller vendedor) {
        double feedback = vendedor.getFeedbackPorcentage();
        int score = vendedor.getFeedbackScore();

        if (feedback >= 99 && score >= 5000) {
            return "🟢 VENDEDOR TOP";
        } else if (feedback >= 98 && score >= 1000) {
            return "🟢 MUY CONFIABLE";
        } else if (feedback >= 95 && score >= 100) {
            return "🟡 CONFIABLE";
        } else if (feedback >= 90 && score >= 50) {
            return "🟡 PROMEDIO";
        } else if (feedback < 85 || score < 10) {
            return "🔴 POCO CONFIABLE";
        }

        return "⚪ SIN SUFICIENTES DATOS";
    }

    /**
     * 🎯 TrustScore mejorado con Z-score
     */
    private double calcularTrustScoreMejorado(double precio, double promedioMercado,
                                              double desviacion, Seller vendedor,
                                              String antiguedad) {
        double score = 0;

        // 1. Precio competitivo (35 pts) - Usando Z-score
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
        } catch (Exception e) {
            // No suma puntos si hay error
        }

        return Math.min(100, Math.round(score * 10) / 10.0);
    }

    /**
     * 🎯 Calcula indicador de confianza
     */
    private String calcularConfianzaAnalisis(int cantidadPrecios,
                                             double desviacionEstandar,
                                             double promedio) {
        if (promedio == 0) return "⚪ Sin datos";

        double coefVariacion = (desviacionEstandar / promedio) * 100;

        if (cantidadPrecios < 5) {
            return "🔴 Muy Baja (muestra insuficiente)";
        } else if (cantidadPrecios < 15) {
            return "🟡 Baja (muestra pequeña)";
        } else if (coefVariacion > 50) {
            return "🟡 Media (alta variabilidad)";
        } else if (cantidadPrecios >= 30 && coefVariacion < 30) {
            return "🟢 Alta (muestra robusta)";
        } else if (cantidadPrecios >= 20 && coefVariacion < 40) {
            return "🟢 Buena";
        }

        return "🟡 Media";
    }

    /**
     * 📈 Analiza tendencia del historial
     */
    private void analizarTendencia(List<PriceHistory> historial) {
        if (historial == null || historial.size() < 2) {
            return;
        }

        historial.sort(Comparator.comparing(PriceHistory::getRecordedAt));

        double precioInicial = historial.get(0).getPrice();
        double precioFinal = historial.get(historial.size() - 1).getPrice();
        double cambio = ((precioFinal - precioInicial) / precioInicial) * 100;

        double[] precios = historial.stream()
                .mapToDouble(PriceHistory::getPrice)
                .toArray();

        double volatilidad = calcularVolatilidad(precios);

        String iconoTendencia;
        String textoTendencia;

        if (cambio > 10) {
            iconoTendencia = "📈";
            textoTendencia = "Tendencia alcista fuerte";
        } else if (cambio > 5) {
            iconoTendencia = "📈";
            textoTendencia = "Tendencia al alza";
        } else if (cambio < -10) {
            iconoTendencia = "📉";
            textoTendencia = "Tendencia bajista fuerte";
        } else if (cambio < -5) {
            iconoTendencia = "📉";
            textoTendencia = "Tendencia a la baja";
        } else {
            iconoTendencia = "➡️";
            textoTendencia = "Precio estable";
        }

        String cambioTexto = String.format("%+.1f%%", cambio);

        String volatilidadTexto;
        if (volatilidad < 5) {
            volatilidadTexto = "Baja volatilidad (precio estable)";
        } else if (volatilidad < 15) {
            volatilidadTexto = "Volatilidad moderada";
        } else {
            volatilidadTexto = "Alta volatilidad (precio inestable)";
        }

        String textoActual = txtEstabilidad.getText();
        txtEstabilidad.setText(textoActual +
                "\n" + iconoTendencia + " " + textoTendencia + " (" + cambioTexto + ")\n" +
                volatilidadTexto + " (" + String.format("%.1f%%", volatilidad) + ")\n");
    }

    /**
     * 📊 Calcula volatilidad
     */
    private double calcularVolatilidad(double[] precios) {
        if (precios.length < 2) return 0;

        double suma = 0;
        for (int i = 1; i < precios.length; i++) {
            double cambio = (precios[i] - precios[i - 1]) / precios[i - 1];
            suma += cambio * cambio;
        }

        return Math.sqrt(suma / (precios.length - 1)) * 100;
    }
}