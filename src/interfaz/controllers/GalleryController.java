package interfaz.controllers;

import entities.Auth;
import entities.Producto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import utils.NotificationManager;
import utils.Sesion;
import utils.TokenManager;
import utils.cls_browseEBAY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryController {
    private String token = Sesion.getTokenAPI();

    @FXML
    private BorderPane root;
    @FXML
    private GridPane gridPane;

    @FXML private ScrollPane filterPanel;
    @FXML
    private HBox paginationBar;

    @FXML
    private Button btnPrev, btnNext;


    // 🔹 Filtros
    @FXML private TextField txtPrecioMin;
    @FXML private TextField txtPrecioMax;
    @FXML private CheckBox cbNuevo;
    @FXML private CheckBox cbUsado;
    @FXML private CheckBox cbMenorPrecio;
    @FXML private CheckBox cbMayorPrecio;
    @FXML private CheckBox cbRecomendado;
    @FXML private CheckBox cbMasRecientes;

    private MainController mainController;

    private final List<Producto> allProducts = new ArrayList<>();
    private int currentPage = 1;
    private cls_browseEBAY apiLoader = new cls_browseEBAY();
    private static final int COLUMNS = 5;
    private static final int ROWS = 3;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS;

    private String searchTerm;
    public Node getRootNode() {
        return root;
    }
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setSearchTerm(String term) {
        if(term != null) {
            this.searchTerm = term;
            loadProducts(term);
        }else {
            NotificationManager.info("Ingresa una palabra para la busqueda.");
        }

    }

    @FXML
    public void initialize() {
        System.out.println("🧠 GalleryController inicializado correctamente");
    }
    /** 🔹 Carga productos directamente desde la API eBay */
    public void loadProducts(String palabra) {
        try {
            System.out.println("🌐 Cargando productos desde la API eBay...");

            List<Producto> productos = apiLoader.obtenerProductos(token, palabra,null,null,null, null);

            if (productos.isEmpty()) {
                gridPane.getChildren().clear();
                VBox placeholder = new VBox();
                Label label = new Label("No se encontraron productos para \"" + palabra + "\"");
                label.setStyle("-fx-font-size: 18; -fx-text-fill: gray;");
                placeholder.getChildren().add(label);
                gridPane.add(placeholder, 0, 0);
                return;
            }

            allProducts.clear();
            allProducts.addAll(productos);
            currentPage = 1;

            renderPage(); // renderiza en cuadrícula
            updatePaginationButtons();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 🔹 Renderiza los productos en cuadrícula (5x3) */
    private void renderPage() {
        gridPane.getChildren().clear();

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allProducts.size());

        int col = 0;
        int row = 0;

        for (int i = start; i < end; i++) {
            Producto producto = allProducts.get(i);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaz/product_card.fxml"));
                VBox card = loader.load();

                ProductCardController controller = loader.getController();
                controller.setMainController(mainController);
                controller.setData(producto, producto.getPriceHistory(), producto.getImageUrls()); // sin BD → precio e imagen son null

                card.setPrefWidth(180);
                card.setPrefHeight(240);
                GridPane.setMargin(card, new Insets(15));

                // Evento de clic → detalle del producto
                mainController.setLastGallery(this);
                card.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> openProductDetail(producto));


                gridPane.add(card, col, row);
                col++;
                if (col == COLUMNS) {
                    col = 0;
                    row++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** 🔹 Botones de paginación dinámicos */
    private void updatePaginationButtons() {
        paginationBar.getChildren().removeIf(node -> node != btnPrev && node != btnNext);
        int totalPages = (int) Math.ceil((double) allProducts.size() / ITEMS_PER_PAGE);

        for (int i = 1; i <= totalPages; i++) {
            final int pageNumber = i;
            Button pageBtn = new Button(String.valueOf(pageNumber));

            pageBtn.setStyle(
                    pageNumber == currentPage
                            ? "-fx-background-color: black; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12;"
                            : "-fx-background-color: transparent; -fx-border-color: black; -fx-border-width: 1; -fx-text-fill: black; -fx-padding: 6 12;"
            );

            pageBtn.setOnAction(e -> {
                currentPage = pageNumber;
                renderPage();
                updatePaginationButtons();
            });

            paginationBar.getChildren().add(paginationBar.getChildren().size() - 1, pageBtn);
        }

        btnPrev.setDisable(currentPage == 1);
        btnNext.setDisable(currentPage == totalPages);
    }

    /** 🔹 Abre detalle de producto (más adelante se usará para mostrar información extendida) */
    @FXML
    private void openProductDetail(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaz/panel_product.fxml"));
            Node productPanel = loader.load();
            ProductController controller = loader.getController();

            // 🔹 Cargar info adicional ANTES de mostrar el panel
            cls_browseEBAY helper = new cls_browseEBAY();
            helper.mtd_informationAditional(token, producto);

            // 🔹 Ahora el producto ya tiene descripción, atributos y envío
            controller.setMainController(mainController);
            controller.loadProduct(producto);

            mainController.loadCustomPanel(productPanel);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    @FXML private void nextPage() {
        int totalPages = (int) Math.ceil((double) allProducts.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
            updatePaginationButtons();
        }
    }

    @FXML private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
            updatePaginationButtons();
        }
    }

    /** 🔹 Mostrar/Ocultar filtros */
    public void toggleFilters() {
        boolean v = filterPanel.isVisible();
        filterPanel.setVisible(!v);
        filterPanel.setManaged(!v);
    }

    /** ============================================================
     APLICAR FILTRO
     ============================================================ */
    @FXML
    private void applyFiltersButton() {
        try {
            // FILTROS DE PRECIOS
            Double min = txtPrecioMin.getText().isEmpty() ? null : Double.parseDouble(txtPrecioMin.getText());
            Double max = txtPrecioMax.getText().isEmpty() ? null : Double.parseDouble(txtPrecioMax.getText());

            // VALIDAR CONDICIÓN (solo 1)
            String condition = null;

            if (cbNuevo.isSelected() && cbUsado.isSelected()) {
                NotificationManager.warning("Selecciona solo una condición: Nuevo o Usado.");
                return;
            } else if (cbNuevo.isSelected()) {
                condition = "new";
            } else if (cbUsado.isSelected()) {
                condition = "used";
            }

            // VALIDAR SORT (solo 1)
            String sort = null;
            int sortCount = 0;

            if (cbMenorPrecio.isSelected()) { sort = "price_asc"; sortCount++; }
            if (cbMayorPrecio.isSelected()) { sort = "price_desc"; sortCount++; }
            if (cbMasRecientes.isSelected()) { sort = "recent"; sortCount++; }

            if (sortCount > 1) {
                NotificationManager.warning("Selecciona solo un ordenamiento.");
                return;
            }

            // HACER LA CONSULTA
            List<Producto> filtrados = apiLoader.obtenerProductos(
                    token,
                    searchTerm,
                    min,
                    max,
                    condition,
                    sort
            );

            // Render
            allProducts.clear();
            allProducts.addAll(filtrados);
            currentPage = 1;

            renderPage();
            updatePaginationButtons();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
