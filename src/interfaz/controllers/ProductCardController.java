package interfaz.controllers;

import entities.ImagesProduct;
import entities.PriceHistory;
import entities.Producto;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.*;
import java.net.URI;
import java.util.List;

public class ProductCardController {

    @FXML
    private VBox rootCard;

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblPrice;

    @FXML
    private ImageView imgProduct;
    @FXML
    private Button btn_guardarInfo;
    @FXML
    private Button btn_url;

    private Producto producto;
    private PriceHistory price;
    private ImagesProduct image;
    private MainController mainController;

    /** 🔹 Carga los datos del producto (API o BD) */

    public void setData(Producto producto, PriceHistory price, List<String> imageUrls) {
        // Título y precio
        this.producto = producto;
        this.price = price;

        lblTitle.setText(producto.getName());
        if (price != null) {
            lblPrice.setText("USD " + price.getPrice());
        } else {
            lblPrice.setText("USD ?");
        }

        // Imagen principal
        if (imageUrls != null && !imageUrls.isEmpty()) {
            try {
                String imageUrl = imageUrls.get(0);
                Image img = new Image(imageUrl, true);
                imgProduct.setImage(img);
            } catch (Exception e) {
                imgProduct.setImage(new Image(getClass().getResource("/interfaz/recursos/imagen-rota.png").toExternalForm()));
            }
        } else {// Imagen por defecto si no hay ninguna

            imgProduct.setImage(new Image(getClass().getResource("/interfaz/recursos/imagen-rota.png").toExternalForm()));
        }

        // Asegurar tamaño visible
        imgProduct.setFitWidth(160);
        imgProduct.setFitHeight(160);
        imgProduct.setPreserveRatio(true);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        makeCardInteractive();
    }

    /** 🔹 Animación y acción */
    private void makeCardInteractive() {
        rootCard.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), rootCard);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        rootCard.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), rootCard);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });

        // Clic → detalle
        rootCard.setOnMouseClicked(this::onCardClicked);
    }

    /** 🔹 Acción al hacer clic sobre la tarjeta */
    private void onCardClicked(MouseEvent event) {
        String precioTexto;
        if (price != null) {
            precioTexto = price.getCurrency() + " " + price.getPrice();
        } else if (producto.getPriceHistory() != null) {
            precioTexto = producto.getPriceHistory().getCurrency() + " " + producto.getPriceHistory().getPrice();
        } else {
            precioTexto = "sin precio";
        }

        if (mainController != null && producto != null) {
            mainController.openProductDetail(producto);
        }
    }
    private boolean isPressed = true;
    @FXML
    private void onSaveInfo() {
        if(!isPressed) {
            btn_guardarInfo.setText("Guardar Producto");
            ProductController productController = new ProductController();
            productController.loadProduct(producto);
            productController.guardarProducto();
            isPressed = true;
        } else{
            btn_guardarInfo.setText("Cancelar");
            ProductAnalysisController productAnalysisController = new ProductAnalysisController();
//            productAnalysisController.();
            isPressed = false;
        }
    }

    @FXML
    private void onClickedURL() {
        try {
            Desktop.getDesktop().browse(new URI(producto.getUrlProduct()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
