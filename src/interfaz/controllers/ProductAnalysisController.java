package interfaz.controllers;

import dao.WishlistDAO; // si aún lo usas en otros lados
import entities.ProductAnalysis;
import entities.PriceHistory;
import entities.Producto;
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
import service.ProductAnalysisService;
import utils.NotificationManager;
import utils.Sesion;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProductAnalysisController {

    @FXML private BorderPane togleAnalys;
    @FXML private VBox content;
    @FXML private Button btnGenerarReporte;
    @FXML private ProgressIndicator progressIndicator;

    private MainController mainController;

    // Interfaz
    @FXML private ImageView productImage;
    @FXML private Text lblTitle;

    // Precio / Cupón
    @FXML private Text txtPrecioActual;
    @FXML private Text txtPromedio;
    @FXML private Text txtDiferencia;

    // Vendedor
    @FXML private Text txtVendedorNombre;
    @FXML private Text txtFeedback;
    @FXML private Text txtFeedbackScore;
    @FXML private Text txtAntiguedad;
    @FXML private Text txtTrustScore;

    // Mercado
    @FXML private Text txtPromedioMercado;
    @FXML private Text txtRango;
    @FXML private Text txtEstabilidad;

    // Estadísticas generales
    @FXML private Text txtTotalProductos;
    @FXML private Text txtPromedioVariacionGeneral;
    @FXML private Text txtTopVariacionesPositivas;
    @FXML private Text txtTopVariacionesNegativas;
    @FXML private Text txtResumenConsultasDiarias;

    // Gráfico
    @FXML private LineChart<String, Number> chartHistorial;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Servicio central
    private final ProductAnalysisService analysisService = new ProductAnalysisService();

    // Estado actual
    private Producto productoActual;
    private ProductAnalysis analisisActual;
    private AcordPanelController acordController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public void setAcordController(AcordPanelController controller) {
        this.acordController = controller;
    }

    @FXML
    public void initialize() {
        togleAnalys.setOnMouseEntered(e ->
                togleAnalys.setStyle("-fx-cursor: hand; -fx-translate-y: -3;")
        );
        togleAnalys.setOnMouseExited(e ->
                togleAnalys.setStyle("-fx-cursor: hand; -fx-effect: none; -fx-translate-y: 0;")
        );

        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    // ======================================================
    // CARGA PRINCIPAL DE PRODUCTO (ASYNC LIMPIO)
    // ======================================================

    public void cargarProducto(String itemId) {
        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
        }

        CompletableFuture
                .supplyAsync(() -> analysisService.analizarProducto(itemId),
                        ProductAnalysisService.getExecutor())
                .thenAccept(result -> {
                    if (result == null || result.producto == null) {
                        Platform.runLater(() ->
                                NotificationManager.error("Producto no encontrado en la base de datos.")
                        );
                        return;
                    }

                    this.productoActual = result.producto;
                    this.analisisActual = result.analisis;

                    Platform.runLater(() -> actualizarUIConResultado(result));
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            NotificationManager.error("Error cargando análisis de producto.")
                    );
                    return null;
                })
                .thenRunAsync(() -> {
                    cargarEstadisticasUI();
                    cargarTop5API();
                    cargarTop5NegativosAPI();
                }, ProductAnalysisService.getExecutor())
                .whenComplete((v, ex) -> {
                    if (progressIndicator != null) {
                        Platform.runLater(() -> progressIndicator.setVisible(false));
                    }
                });
    }

    private void actualizarUIConResultado(ProductAnalysisService.ProductAnalysisResult r) {
        // Título
        lblTitle.setText(r.producto.getName());

        // Imagen
        try {
            Image img = new Image(r.urlImagen, true);
            productImage.setImage(img);
        } catch (Exception e) {
            productImage.setImage(new Image("/recursos/img/no-image.png"));
        }

        // Precio actual
        double precioVal = (r.precioActual != null) ? r.precioActual.getPrice() : 0.0;
        String moneda = (r.precioActual != null) ? r.precioActual.getCurrency() : "USD";
        txtPrecioActual.setText("Precio actual: " + moneda + " " + String.format("%.2f", precioVal));

        // Cupón
        if (r.cupon != null) {
            txtPromedio.setText("\nCupón: " + r.cupon.getCouponRedemption());
            txtDiferencia.setText("\nVálido hasta: " + r.cupon.getExpirationAt());
        } else {
            txtPromedio.setText("\nSin cupones activos");
            txtDiferencia.setText("");
        }

        // Vendedor
        if (r.vendedor != null) {
            txtVendedorNombre.setText("Vendedor: " + r.vendedor.getUsername());
            txtFeedback.setText("\nFeedback positivo: " +
                    String.format("%.2f", r.vendedor.getFeedbackPorcentage()) + "%");
            txtFeedbackScore.setText("\nFeedback puntuación: " + r.vendedor.getFeedbackScore());
            txtAntiguedad.setText("\nAntigüedad: " + (r.antiguedad != null ? r.antiguedad : "N/A"));
            txtTrustScore.setText("\nTrustScore: " +
                    String.format("%.1f", r.trustScore) + " / 100");
        } else {
            txtVendedorNombre.setText("Vendedor: No registrado N/A");
            txtFeedback.setText("\nFeedback: N/A");
            txtFeedbackScore.setText("\nFeedback puntuación: N/A");
            txtAntiguedad.setText("\nAntigüedad: N/A");
            txtTrustScore.setText("\nTrustScore: N/A");
        }

        // Mercado
        if (r.marketStats != null && r.marketStats.cantidad > 0) {
            txtPromedioMercado.setText(
                    String.format("Promedio mercado: USD %.2f", r.marketStats.promedio)
            );
            txtRango.setText(
                    String.format("\nRango: USD %.2f – %.2f", r.marketStats.min, r.marketStats.max)
            );

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\nDesviación: %.2f (%.1f%%)\n",
                    r.marketStats.desviacion, r.marketStats.coefVariacion));
            sb.append(r.marketStats.estabilidad).append("\n");
            sb.append("Productos analizados: ").append(r.marketStats.cantidad).append("\n");
            sb.append("Confianza del análisis: ").append(r.marketStats.confianza).append("\n");
            sb.append("Estado del precio: ").append(r.marketStats.alertaPrecio).append("\n");

            if (r.tendencia != null) {
                sb.append("\n")
                        .append(r.tendencia.icono).append(" ")
                        .append(r.tendencia.descripcionTendencia)
                        .append(" (")
                        .append(String.format("%+.1f%%", r.tendencia.cambioPorcentual))
                        .append(")\n")
                        .append(r.tendencia.descripcionVolatilidad)
                        .append(" (")
                        .append(String.format("%.1f%%", r.tendencia.volatilidad))
                        .append(")\n");
            }

            txtEstabilidad.setText(sb.toString());
        } else {
            txtPromedioMercado.setText("Promedio: --");
            txtRango.setText("Rango: $-- a $--");
            txtEstabilidad.setText("Sin datos de mercado suficientes");
        }

        // Historial en gráfico
        llenarGraficoHistorial(r.historial);
    }

    private void llenarGraficoHistorial(List<PriceHistory> historial) {
        chartHistorial.getData().clear();

        if (historial == null || historial.isEmpty()) {
            System.out.println("No hay historial de precios para graficar.");
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

    // ======================================================
    // ESTADÍSTICAS GENERALES
    // ======================================================

    private void cargarEstadisticasUI() {
        try {
            ProductAnalysisService.EstadisticasGenerales eg = analysisService.obtenerEstadisticasGenerales();

            Platform.runLater(() -> {
                txtTotalProductos.setText("\nTotal analizados: " + eg.totalAnalizados + "\n");
                txtPromedioVariacionGeneral.setText(
                        "Promedio de variación: " + String.format("%.2f", eg.promedioVariacion) + "\n"
                );

                StringBuilder resumen = new StringBuilder();
                if (eg.resumenDiario != null) {
                    for (ProductAnalysisService.ResumenDiario rd : eg.resumenDiario) {
                        resumen.append(rd.fecha)
                                .append(" → ")
                                .append(rd.cantidad)
                                .append(" consultas");
                    }
                }
                txtResumenConsultasDiarias.setText(resumen.toString());
            });

        } catch (Exception e) {
            System.err.println("Error cargando estadísticas: " + e.getMessage());
        }
    }

    // ======================================================
    // BOTONES / ACCIONES UI
    // ======================================================

    @FXML
    private void onReportGenerated() {
        if (productoActual == null) {
            NotificationManager.error("No hay producto cargado");
            return;
        }

        if (analisisActual == null) {
            NotificationManager.error("No hay análisis disponible");
            return;
        }

        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
        }

        CompletableFuture
                .supplyAsync(() -> analysisService.generarReporteProducto(productoActual, analisisActual),
                        ProductAnalysisService.getExecutor())
                .thenAccept(success -> Platform.runLater(() -> {
                    if (progressIndicator != null) {
                        progressIndicator.setVisible(false);
                    }

                    if (success) {
                        NotificationManager.success("Reporte guardado en Descargas");
                    } else {
                        NotificationManager.error("Error al generar el reporte PDF");
                    }
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        if (progressIndicator != null) {
                            progressIndicator.setVisible(false);
                        }
                        NotificationManager.error("Error al generar el reporte PDF");
                    });
                    return null;
                });
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

        int userId = Sesion.getUsuario().getId();

        CompletableFuture
                .supplyAsync(() -> analysisService.eliminarDeWishlist(productoActual, userId),
                        ProductAnalysisService.getExecutor())
                .thenAccept(eliminado -> {
                    if (!eliminado) {
                        Platform.runLater(() ->
                                NotificationManager.info("Este producto no está en tu lista.")
                        );
                        return;
                    }

                    // Actualizar contador
                    int count = analysisService.contarWishlistPorUsuario(userId);

                    Platform.runLater(() -> {
                        NotificationManager.success("Eliminado de la lista de guardados.");
                        if (mainController != null) {
                            mainController.actualizarWishlistCount(count);
                        }
                        if (acordController != null) {
                            acordController.recargarLista();
                        }
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() ->
                            NotificationManager.error("Error eliminando de tu wishlist.")
                    );
                    return null;
                });
    }

    /**
     * Método estático para apagar el pool de hilos al cerrar la app.
     */
    public static void shutdown() {
        ProductAnalysisService.shutdownExecutor();
    }
    private void cargarTop5API() {

        CompletableFuture
                .supplyAsync(() -> analysisService.obtenerTop5API(productoActual.getName()))
                .thenAccept(lista -> {

                    if (lista == null || lista.isEmpty()) {
                        Platform.runLater(() -> {
                            txtTopVariacionesPositivas.setText("No se encontraron productos similares");
                        });
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    for (ProductAnalysisService.TopProductoDTO dto : lista) {
                        sb.append(dto.titulo)
                                .append(" → USD ")
                                .append(String.format("%.2f", dto.precio))
                                .append("\n");
                    }

                    Platform.runLater(() -> {
                        txtTopVariacionesPositivas.setText(sb.toString());
                    });
                });
    }
    private void cargarTop5NegativosAPI() {

        CompletableFuture
                .supplyAsync(() -> analysisService.obtenerTop5NegativosAPI(productoActual.getName()))
                .thenAccept(lista -> {

                    if (lista == null || lista.isEmpty()) {
                        Platform.runLater(() -> {
                            txtTopVariacionesNegativas.setText("No se encontraron productos similares");
                        });
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    for (ProductAnalysisService.TopProductoDTO dto : lista) {
                        sb.append(dto.titulo)
                                .append(" → USD ")
                                .append(String.format("%.2f", dto.precio))
                                .append("\n");
                    }

                    Platform.runLater(() -> {
                        txtTopVariacionesNegativas.setText(sb.toString());
                    });
                });
    }


}


