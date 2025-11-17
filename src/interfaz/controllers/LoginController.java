package interfaz.controllers;

import dao.AuthDAO;
import entities.Auth;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import utils.HashUtil;
import utils.NotificationManager;
import utils.Sesion;
import utils.TokenManager;

public class LoginController {

    @FXML private ImageView logo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Button btnIngresar;
    // Ahora el root es StackPane
    @FXML private StackPane rootLogin;

    private MainController main;
    public void setMainController(MainController m) { this.main = m; main.slideInFromLeft(rootLogin);}

    @FXML
    public void initialize() {
    }

    @FXML
    private void onLogin() {

        String user = txtUsuario.getText().trim();
        String pass = txtContrasena.getText().trim();
        String hash = HashUtil.sha256(pass);

        if (user.isEmpty()) {
            NotificationManager.warning("Ingresa el usuario.");
            main.shake(txtUsuario);
            return;
        }

        if (pass.isEmpty()) {
            NotificationManager.warning("Ingresa el contraseña.");
            main.shake(txtContrasena);
            return;
        }

        // Obtenemos el usuario real desde la BD
        Auth auth = validarCredenciales(user, hash);

        if (auth != null) {

            // ✔ Guardar usuario real en sesión
            Sesion.iniciar(auth);

            // ✔ Renovar token si caducó (o generar uno si aplica)
            NotificationManager.success("Inicio exitoso.");
            TokenManager.refreshToken();
            playFadeOut(() -> {
                main.loadPanel("/interfaz/panel_search_main.fxml");
            });

        } else {
            main.shake(txtUsuario);
            main.shake(txtContrasena);
            NotificationManager.warning("Credenciales no validas.");
        }
    }



    @FXML
    private void onRegister() {
        main.slideOutToLeft(rootLogin, () -> {
            main.loadPanel("/interfaz/panelRegister.fxml");
        });
    }



    @FXML
    private void onResetPass(){
        main.slideOutToLeft(rootLogin, () -> {main.loadPanel("/interfaz/panelReset.fxml");
        });
    }

    private Auth validarCredenciales(String username, String passwordHash) {

        AuthDAO dao = new AuthDAO();
        return dao.login(username, passwordHash); // devuelve el usuario real o null
    }

    private void playFadeOut(Runnable after) {
        FadeTransition fade = new FadeTransition(Duration.millis(500), rootLogin);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> after.run());
        fade.play();
    }
}
