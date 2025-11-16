package interfaz.controllers;

import dao.AuthDAO;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.HashUtil;

public class ResetController {

    private MainController main;
    public void setMainController(MainController m){ this.main = m; main.slideInFromRight(panelReset);}

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private PasswordField txtContrasenaRep;
    @FXML private StackPane panelReset;
    @FXML
    private void onResetPass() {

        String user = txtUsuario.getText().trim();
        String pass = txtContrasena.getText().trim();
        String pass2 = txtContrasenaRep.getText().trim();

        if (user.isEmpty()) {
            alert("Por favor ingrese el usuario o email.");
            main.shake(txtUsuario);
            return;
        }

        // Validar contraseña mínima
        if (pass.length() < 6) {
            alert("La contraseña debe tener al menos 6 caracteres.");
            main.shake(txtContrasena);
            return;
        }

        if (!pass.equals(pass2)) {
            alert("Las contraseñas no coinciden.");
            main.shake(txtContrasenaRep);
            return;
        }

        // -------------------------
        // NUEVO HASH
        // -------------------------
        String newHash = HashUtil.sha256(pass);

        AuthDAO dao = new AuthDAO();

        boolean ok = dao.resetPasswordByUserOrEmail(user, newHash);

        if (ok) {
            alert("Contraseña restablecida exitosamente ✔");
        } else {
            alert("El usuario o email no existe.");
        }
        volverLogin();
    }

    private void volverLogin() {
        main.loadPanel("/interfaz/panelLogin.fxml");
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
