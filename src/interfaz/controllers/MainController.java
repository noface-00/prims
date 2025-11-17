package interfaz.controllers;

import dao.WishlistDAO;
import entities.Producto;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import utils.NotificationManager;
import utils.Sesion;

import java.io.IOException;

public class MainController {
    private GalleryController lastGallery;
    @FXML
    private AnchorPane contentArea; // Área central donde se cargan los paneles
    @FXML
    private TextField txtBuscar;
    @FXML
    private VBox blockFiltrar;
    @FXML
    private HBox blockBuscar, blockAnalisis;
    @FXML
    private Button btnOptions, btnOptions1, btnOptions2, btnOptions3, btnAnalisis, btnFiltrar;
    @FXML
    private Label lblNumSave;

    @FXML private StackPane rootPane;
    @FXML
    public void initialize() {
        if (rootPane != null) {
            NotificationManager.initialize(rootPane);
            System.out.println("✅ Sistema de notificaciones inicializado en MainController");
        } else {
            System.err.println("⚠️ rootContainer es null. Verifica que tu FXML tenga un StackPane como raíz.");
        }
        if (blockFiltrar != null)
            blockFiltrar.setVisible(false);
        if (blockBuscar != null)
            blockBuscar.setVisible(false);

        if (txtBuscar != null) {
            txtBuscar.setOnAction(e -> onSearch());
        }
        // Al iniciar el main.fxml, carga el panel principal de búsqueda
        loadPanel("/interfaz/panelLogin.fxml");
    }

    /** 🔹 Carga un panel FXML en el área central (sin parámetro de búsqueda) */
    public void loadPanel(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            Object controller = loader.getController();
            System.out.println("🧠 Controlador cargado: " + (controller != null ? controller.getClass().getName() : "null"));
            // Login
            if (controller instanceof LoginController login) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(false);
                btnFiltrar.setVisible(false);
                if (blockBuscar != null)
                    blockBuscar.setVisible(false);
                if (blockAnalisis != null)
                    blockAnalisis.setVisible(false);
                login.setMainController(this);
            }
            // Register
            if (controller instanceof RegisterController register) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(true);
                btnFiltrar.setVisible(false);
                if (blockBuscar != null)
                    blockBuscar.setVisible(false);
                if (blockAnalisis != null)
                    blockAnalisis.setVisible(false);
                register.setMainController(this);
            }
            // Reset
            if (controller instanceof ResetController reset) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(true);
                btnFiltrar.setVisible(false);
                if (blockBuscar != null)
                    blockBuscar.setVisible(false);
                if (blockAnalisis != null)
                    blockAnalisis.setVisible(false);
                reset.setMainController(this);
            }

            // 🔹 Panel principal de búsqueda
            if (controller instanceof SearchMainController searchController) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(false);
                    btnFiltrar.setVisible(true);
                if (blockBuscar != null)
                    blockBuscar.setVisible(false);
                if (blockAnalisis != null)
                    blockAnalisis.setVisible(true);
                WishlistDAO wdao = new WishlistDAO();
                int total = wdao.countWishlistByUser(Sesion.getUsuario().getId());

                lblNumSave.setText(String.valueOf(total));

                System.out.println("✅ Entró al if de SearchMainController");
                searchController.setMainController(this);
            }

            // 🔹 Galería de productos (panel_search.fxml)
            if (controller instanceof GalleryController galleryController) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(true);
                if (blockBuscar != null)
                    blockBuscar.setVisible(true);
                System.out.println("✅ Entró al if de GalleryController");
                galleryController.setMainController(this);
            }

            // 🔹 Panel de análisis (AcordPanel)
            if (controller instanceof AcordPanelController acordController) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(true);
                    btnFiltrar.setVisible(false);
                if (blockBuscar != null)
                    blockBuscar.setVisible(false);
                if (blockAnalisis != null)
                    blockAnalisis.setVisible(false);
                System.out.println("✅ Entró al if de AcordPanelController");
                // Aquí podrías inicializar cosas si fuera necesario:
                // acordController.initDatos() o acordController.setMainController(this);
            }

            // Reemplaza el contenido central
            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error cargando panel: " + fxmlPath);
        }
    }
    public void setTextSearch(String textSearch) {
        txtBuscar.setText(textSearch);
    }
    /** 🔹 Carga un panel con parámetro de búsqueda */
    public void loadPanelWithSearch(String fxmlPath, String palabra) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            Object controller = loader.getController();
            System.out.println("🧠 Controlador cargado (búsqueda): " + (controller != null ? controller.getClass().getName() : "null"));

            if (controller instanceof GalleryController galleryController) {
                if (blockFiltrar != null)
                    blockFiltrar.setVisible(true);
                if (blockBuscar != null)
                    blockBuscar.setVisible(true);

                System.out.println("✅ Entró al if de GalleryController desde loadPanelWithSearch");
                galleryController.setMainController(this);
                galleryController.setSearchTerm(palabra); // 👉 carga los productos automáticamente
            }

            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error cargando panel con búsqueda: " + fxmlPath);
        }
    }

    /** 🔹 Carga un panel ya instanciado (por ejemplo, el detalle de producto) */
    public void loadCustomPanel(Node node) {
        contentArea.getChildren().setAll(node);
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
    }

    public void setLastGallery(GalleryController gallery) {
        this.lastGallery = gallery;
    }
    @FXML
    public void onSearch() {
        loadPanelWithSearch("/interfaz/panel_search.fxml", txtBuscar.getText());
    }
    // Estado del menú hamburguesa
    private boolean menuVisible = false;

    @FXML
    private void onDisplayOP() {
        menuVisible = !menuVisible;

        // 🔥 Animación de transición del símbolo
        animateMenuIcon(btnOptions, menuVisible);

        // Cambiar icono después de la animación (pequeño delay)
        javafx.application.Platform.runLater(() -> {
            btnOptions.setText(menuVisible ? "Ⅹ" : "⟩");
        });

        // Los botones a mostrar u ocultar
        Button[] optionButtons = {btnOptions1, btnOptions2, btnOptions3};

        double delay = 0; // tiempo de retraso entre cada botón (para efecto acordeón)
        double stepDelay = 100; // milisegundos entre botones

        for (Button btn : optionButtons) {
            if (menuVisible) {
                // Mostrar con animación tipo acordeón (de izquierda a derecha)
                btn.setVisible(true);
                btn.setOpacity(0);
                btn.setTranslateX(-20); // parte desde un poco a la izquierda

                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), btn);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), btn);
                slideIn.setFromX(-20);
                slideIn.setToX(0);

                javafx.animation.ParallelTransition appear = new javafx.animation.ParallelTransition(fadeIn, slideIn);
                appear.setDelay(javafx.util.Duration.millis(delay));
                appear.play();

                delay += stepDelay; // incremento del retardo para el siguiente botón

            } else {
                // Ocultar con animación acordeón inversa
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), btn);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                javafx.animation.TranslateTransition slideOut = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), btn);
                slideOut.setFromX(0);
                slideOut.setToX(-20);

                javafx.animation.ParallelTransition disappear = new javafx.animation.ParallelTransition(fadeOut, slideOut);
                disappear.setDelay(javafx.util.Duration.millis(delay));
                disappear.setOnFinished(e -> btn.setVisible(false));
                disappear.play();

                delay += stepDelay;
            }
        }
    }
    private void animateMenuIcon(Button btn, boolean opening) {

        // Animación de crecimiento
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), btn);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);

        // Rotación ligera (opcional, le da más vida)
        RotateTransition rotate = new RotateTransition(Duration.millis(180), btn);
        rotate.setFromAngle(0);
        rotate.setToAngle(opening ? 10 : -10);
        rotate.setAutoReverse(true);
        rotate.setCycleCount(2);

        new ParallelTransition(scale, rotate).play();
    }


    @FXML
    private void onBack() {
        if (contentArea.getChildren().isEmpty()) {
            System.out.println("⚠️ No hay panel activo para regresar.");
            return;
        }

        Node currentPanel = contentArea.getChildren().get(0);
        String currentId = currentPanel.getId();

        System.out.println("🔍 Panel actual: " + currentPanel.getClass().getSimpleName() + " | fx:id: " + currentId);

        // 🔹 Si vienes del panel de producto → vuelve a la galería (sin recargar)
        if ("productPanel".equals(currentId)) {
            if (lastGallery != null) {
                contentArea.getChildren().setAll(lastGallery.getRootNode());
                System.out.println("⬅️ Volviendo al mismo panel de productos (sin recargar)");
            } else {
                loadPanel("/interfaz/panel_search.fxml");
                System.out.println("ℹ️ No había galería guardada, se recarga desde cero");
            }
            return; // 👈 Detiene el flujo aquí
        }

        // 🔹 Si vienes del panel de análisis (acordPanel) → vuelve al panel principal
        if ("acordPanel".equals(currentId)) {
            loadPanel("/interfaz/panel_search_main.fxml");
            System.out.println("⬅️ Volviendo al panel principal (main)");
            return; // 👈 Detiene el flujo aquí
        }
        // 🔹 Si estás en panelRegister → vuelve al Login
        if ("panelRegister".equals(currentId)) {
            loadPanel("/interfaz/panelLogin.fxml");
            System.out.println("⬅️ Volviendo del Register al Login");
            return;
        }

// 🔹 Si estás en panelReset → vuelve al Login
        if ("panelReset".equals(currentId)) {
            loadPanel("/interfaz/panelLogin.fxml");
            System.out.println("⬅️ Volviendo del Reset al Login");
            return;
        }


        // 🔹 Si no es ninguno de los anteriores → vuelve al principal por defecto
        System.out.println("ℹ️ Panel no reconocido, volviendo al principal.");
        loadPanel("/interfaz/panel_search_main.fxml");
    }

    public void openProductDetail(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaz/panel_product.fxml"));
            Parent panel = loader.load();

            ProductController pc = loader.getController();
            pc.setMainController(this);
            pc.loadProduct(producto);   // 🔥 AQUÍ YA PASA EL PRODUCTO CORRECTO

            contentArea.getChildren().setAll(panel);

            AnchorPane.setTopAnchor(panel, 0.0);
            AnchorPane.setBottomAnchor(panel, 0.0);
            AnchorPane.setLeftAnchor(panel, 0.0);
            AnchorPane.setRightAnchor(panel, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onShow() {
        if (lastGallery != null)
            lastGallery.toggleFilters();
    }

    @FXML
    private void onPanelAnalisis() {
        // Usa el sistema centralizado de carga que ya tienes
        loadPanel("/interfaz/panel_wishlist.fxml");
        System.out.println("✅ Panel de análisis cargado mediante loadPanel().");
    }

    protected void slideInFromRight(Node node) {
        node.setTranslateX(600); // fuera de pantalla a la derecha
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setToX(0);
        slide.play();
    }

    protected void slideOutToLeft(Node node, Runnable after) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setToX(-600); // sale hacia la izquierda
        slide.setOnFinished(e -> after.run());
        slide.play();
    }

    protected void slideInFromLeft(Node node) {
        node.setTranslateX(-600);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setToX(0);
        slide.play();
    }

    protected void slideOutToRight(Node node, Runnable after) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setToX(600);
        slide.setOnFinished(e -> after.run());
        slide.play();
    }


    public void changeWithSlide(Node oldPanel, String newFXML) {

        // 1 → animar salida del panel actual
        slideOutToRight(oldPanel, () -> {

            // 2 → cargar panel nuevo
            loadPanel(newFXML);

            // 3 → animación de entrada del panel nuevo
            Node newPanel = contentArea.getChildren().get(0);
            slideInFromLeft(newPanel);
        });
    }
    protected void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(80), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }
    public void actualizarWishlistCount(int count) {
        lblNumSave.setText(String.valueOf(count));
    }
    public void guardarProductoDesdeCard(Producto producto) {
        ProductController pc = new ProductController();
        pc.guardarProductoDirecto(producto);
    }

}
