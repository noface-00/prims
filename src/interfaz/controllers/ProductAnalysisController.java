package interfaz.controllers;

import api.conect_API_eBay;
import dao.*;
import entities.*;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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

        double promedio = precios.stream().mapToDouble(d -> d).average().orElse(0);
        double min = precios.stream().min(Double::compare).orElse(0.0);
        double max = precios.stream().max(Double::compare).orElse(0.0);

        double variance = precios.stream()
                .mapToDouble(p -> Math.pow(p - promedio, 2))
                .average()
                .orElse(0);

        double desviacion = Math.sqrt(variance);
        double diferencia = precioActual - promedio;

        analisisActual.setMarketAverage(promedio);
        analisisActual.setMarketMin(min);
        analisisActual.setMarketMax(max);
        analisisActual.setStdDeviation(desviacion);
        analisisActual.setPriceDifference(diferencia);

        Platform.runLater(() -> {
            txtPromedioMercado.setText(String.format("Promedio mercado: USD %.2f\n", promedio));
            txtRango.setText(String.format("Rango: USD %.2f – %.2f\n", min, max));
            txtEstabilidad.setText(String.format("Desviación estándar: %.2f\n", desviacion));
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
        if (productoActual == null || analisisActual == null) {
            System.err.println("❌ No hay análisis disponible para generar reporte");
            return;
        }

        Task<Void> reportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ProductDAO productDAO = new ProductDAO();
                Producto productoCompleto = productDAO.read(productoActual.getItemId());

                if (productoCompleto != null) {
                    ReportService.generarReporteUnico(productoCompleto, analisisActual);
                    Platform.runLater(() ->
                            System.out.println("📄 Reporte PDF generado exitosamente")
                    );
                }
                return null;
            }
        };

        new Thread(reportTask).start();
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


}