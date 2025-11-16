package interfaz.controllers;

import api.conect_API_eBay;
import dao.*;
import entities.*;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import utils.Sesion;
import utils.cls_browseEBAY;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controlador del módulo de análisis de producto.
 * Integra datos locales (BD) y datos dinámicos (API eBay) para análisis.
 *
 * @author Kevin
 */
public class ProductAnalysisController {
    @FXML private BorderPane togleAnalys;
    @FXML private VBox content;
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

    // 🔹 Gráfico
    @FXML private LineChart<String, Number> chartHistorial;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // 🔧 Variables internas
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String token = Sesion.getTokenAPI();
    private final conect_API_eBay api = new conect_API_eBay();
    private final cls_browseEBAY browseEBAY = new cls_browseEBAY();

    @FXML
    public void initialize() {

        // Hover limpio, sin zoom
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

    }


    public ProductAnalysisController() {
    }

    /**
     * Carga y analiza un producto desde la base de datos + API eBay.
     */
    public void cargarProducto(String itemId) {
        try {
            ProductDAO productDAO = new ProductDAO();
            Producto productoBD = productDAO.findByItemId(itemId);

            if (productoBD == null) {
                txtPrecioActual.setText("Producto no encontrado en la base de datos.");
                return;
            }

            // 🔹 Mostrar título del producto
            lblTitle.setText(productoBD.getName());

            // 🔹 Buscar imagen principal
            ImagesProductDAO imageDAO = new ImagesProductDAO();
            String urlImagen = imageDAO.findMainImageByProduct(productoBD);
            if (urlImagen != null && !urlImagen.isEmpty()) {
                productImage.setImage(new Image(urlImagen, true));
            } else {
                productImage.setImage(new Image("/recursos/img/no-image.png"));
            }

            // 🔹 Buscar precio más reciente
            PriceHistoryDAO priceDAO = new PriceHistoryDAO();
            PriceHistory precioActual = priceDAO.findLatestByItemId(itemId);

            double precio = (precioActual != null) ? precioActual.getPrice() : 0.0;
            String moneda = (precioActual != null) ? precioActual.getCurrency() : "USD";
            txtPrecioActual.setText("Precio actual: " + moneda + " " + String.format("%.2f", precio));

            // 🔹 Buscar cupón asociado
            CouponProDAO couponDAO = new CouponProDAO();
            CouponPro cupon = couponDAO.findByItemId(itemId);
            if (cupon != null) {
                txtPromedio.setText("Cupón: " + cupon.getCouponRedemption() + "\n");
                txtDiferencia.setText("Válido hasta: " + cupon.getExpirationAt() + "\n");
                System.out.println("Cupón encontrado: " + cupon.getCouponRedemption());
            } else {
                txtPromedio.setText("\nSin cupones activos\n");
                txtDiferencia.setText("");
            }

            // 🔹 Buscar vendedor relacionado (según el idSeller en la entidad Producto)
            SellerDAO sellerDAO = new SellerDAO();
            Seller vendedor = sellerDAO.getUserById(productoBD.getIdSeller().getId());


            if (vendedor != null) {
                txtVendedorNombre.setText("Vendedor: " + vendedor.getUsername() + "\n");
                txtFeedback.setText("Feedback positivo: " +
                        String.format("%.2f", vendedor.getFeedbackPorcentage()) + "%\n");
                txtFeedbackScore.setText("Feedback Puntuacion: " + vendedor.getFeedbackScore() + "\n");

                // 🔹 Calcular antigüedad vía API eBay
                try {
                    Map<String, String> infoAntiguedad =
                            cls_browseEBAY.calcularAccountAge(token, vendedor.getUsername());

                    String fecha = infoAntiguedad.getOrDefault("fecha", "N/A");
                    String antiguedad = infoAntiguedad.getOrDefault("antiguedad", "Desconocida");

                    txtAntiguedad.setText("Antigüedad: " + antiguedad + " (desde " + fecha + ")\n");

                    // 🔹 Calcular TrustScore
                    double trustScore = calcularTrustScore(
                            precio, 1000,
                            String.valueOf(vendedor.getFeedbackPorcentage()), String.valueOf(vendedor.getFeedbackScore()),
                            antiguedad
                    );
                    txtTrustScore.setText("TrustScore: " + String.format("%.1f", trustScore) + " / 100\n");

                    System.out.println("Antigüedad calculada: " + antiguedad + " | Desde: " + fecha);
                } catch (Exception e) {
                    txtAntiguedad.setText("Antigüedad: No disponible\n");
                }
            } else {
                txtVendedorNombre.setText("Vendedor: No registrado en BD\n");
                txtFeedback.setText("Feedback: N/A\n");
                txtAntiguedad.setText("Antigüedad: N/A\n");
                txtTrustScore.setText("TrustScore: N/A\n");
            }


            // Mostrar analisis de precio de mercado
            List<Double> preciosDelMercado = api.obtenerPreciosDelMercado(productoBD.getName(), token);
            System.out.println("PreciosDelMercado: " + preciosDelMercado);
            // 🔹 Mostrarlos en pantalla
            calcularAnalisisMercado(preciosDelMercado);


            // 🔹 Cargar historial completo del producto
            List<PriceHistory> historial = priceDAO.findAllByItemId(itemId);

            // 🔹 Graficar historial real
            llenarGraficoHistorialReal(historial);

        } catch (Exception e) {
            e.printStackTrace();
            txtPrecioActual.setText("Error al cargar producto desde la BD.");
        }
    }


    /**
     * Calcula un TrustScore ponderado por precio, feedback y antigüedad.
     */
    private double calcularTrustScore(double precio, double promedio, String feedback, String feedbackScore, String antiguedad) {
        double score = 0;

        // ----------------------------------------
        // 1. Precio competitivo → +40 pts
        // ----------------------------------------
        double diff = Math.abs(precio - promedio);
        double scorePrecio = Math.max(0, 40 - (diff / promedio * 100));
        score += Math.max(0, Math.min(scorePrecio, 40));

        // ----------------------------------------
        // 2. Feedback positivo % → +40 pts
        // ----------------------------------------
        try {
            double feedbackVal = Double.parseDouble(feedback);
            score += (feedbackVal / 100) * 40;
        } catch (Exception ignored) {}

        // ----------------------------------------
        // 3. Feedback Score (cantidad de feedbacks) → +20 pts
        // ----------------------------------------
        try {
            int fs = Integer.parseInt(feedbackScore);

            if (fs >= 1000) score += 20;
            else if (fs >= 200) score += 15;
            else if (fs >= 50) score += 10;
            else if (fs >= 10) score += 5;
            else if (fs > 0) score += 2;

        } catch (Exception ignored) {}

        // ----------------------------------------
        // 4. Antigüedad → +20 pts
        // ----------------------------------------
        if (antiguedad.matches(".*(\\d+) año.*")) {
            int años = Integer.parseInt(antiguedad.replaceAll("\\D+", ""));
            score += Math.min(años * 4, 20); // 5 años = 20 pts
        }

        return Math.min(100, score);
    }
    public void calcularAnalisisMercado(List<Double> precios) {

        if (precios == null || precios.isEmpty()) {
            txtPromedioMercado.setText("Promedio: --\n");
            txtRango.setText("Rango: $-- a $--\n");
            txtEstabilidad.setText("Desviación estándar: --\n");
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

        txtPromedioMercado.setText(String.format("Promedio mercado: USD %.2f\n", promedio));
        txtRango.setText(String.format("Rango: USD %.2f – %.2f\n", min, max));
        txtEstabilidad.setText(String.format("Desviación estándar: %.2f\n", desviacion));
    }

    /**
     * Genera un gráfico de precios con valores simulados.
     */
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
                // recordedAt: "2024-11-03T12:35:00Z"
                String fecha = ph.getRecordedAt().substring(0, 10); // yyyy-MM-dd
                double precio = ph.getPrice();

                serie.getData().add(new XYChart.Data<>(fecha, precio));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        chartHistorial.getData().add(serie);
    }

    @FXML
    private void onClicked(){
        if(!content.isVisible()) {
            content.setVisible(true);
        } else {
            content.setVisible(false);
        }
    }

}