package interfaz.controllers;

import dao.WishlistDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import utils.Sesion;

import java.io.IOException;

public class AcordPanelController {

    @FXML
    private VBox acordPanel;

    private final WishlistDAO wishlistDAO = new WishlistDAO();


    @FXML
    public void initialize() {
        int userId = Sesion.getUsuario().getId();

        var listaItems = wishlistDAO.getAllItemIdsByUser(userId);
        System.out.println("🎯 Productos en wishlist: " + listaItems.size());

        acordPanel.getChildren().clear();

        if (listaItems.isEmpty()) {
            Label lbl = new Label("No tienes productos guardados para analizar.");
            lbl.setStyle("-fx-font-family:'Poppins SemiBold'; -fx-text-fill:#888; -fx-font-size:14;");
            acordPanel.getChildren().add(lbl);
            return;
        }

        for (String itemId : listaItems) {
            loadProductAnalysis(itemId);
        }
    }



    private void loadProductAnalysis(String itemId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interfaz/productAnalize.fxml"));
            Node analysisPanel = loader.load();

            ProductAnalysisController controller = loader.getController();
            controller.cargarProducto(itemId);

            acordPanel.getChildren().add(analysisPanel);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error al cargar productAnalize.fxml dentro del acordPanel.");
        }
    }

}
