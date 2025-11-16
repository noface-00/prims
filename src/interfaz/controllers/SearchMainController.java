package interfaz.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class SearchMainController {

    private MainController mainController;
    @FXML
    private Button btnSearch;

    @FXML
    private TextField txtSearch;

    /** Permite que el MainController se inyecte al cargar el panel */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    @FXML
    private void initialize() {
        // 🔹 Permitir buscar presionando Enter en el campo de texto
        txtSearch.setOnAction(e -> onSearch());
    }

    /** 🔍 Evento principal del botón de búsqueda */
    @FXML
    private void onSearch() {
        String palabra = txtSearch.getText().trim();
        if (palabra.isEmpty()) {
            System.out.println("⚠️ Debes ingresar una palabra de búsqueda.");
            return;
        }

        System.out.println("🔍 Buscando productos en eBay con palabra: " + palabra);

        // ✅ Delega la carga al MainController
        if (mainController != null) {
            mainController.setTextSearch(txtSearch.getText());
            mainController.loadPanelWithSearch("/interfaz/panel_search.fxml", palabra);
        } else {
            System.err.println("⚠️ mainController es null. No se ha inyectado correctamente.");
        }
    }
}
