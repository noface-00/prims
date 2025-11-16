package interfaz.controllers;

import dao.AuthDAO;
import entities.Auth;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import utils.HashUtil;

public class RegisterController {

    @FXML private TextField txtUsuario;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtContrasena;
    @FXML private PasswordField txtContrasenaRep;
    @FXML private Button btnRegister;
    @FXML private StackPane panelRegister;

    private MainController main;
    public void setMainController(MainController m){ this.main = m; main.slideInFromRight(panelRegister);}

    @FXML
    public void initialize() {
    }

    @FXML
    private void onRegister() {

        String user = txtUsuario.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = txtContrasena.getText().trim();
        String pass2 = txtContrasenaRep.getText().trim();

        // Validaciones
        if (user.isEmpty()) {
            alert("Ingresa un nombre de usuario.");
            main.shake(txtUsuario);
            return;
        }

        if (email.isEmpty()) {
            alert("Ingresa tu correo electrónico.");
            main.shake(txtEmail);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            alert("Correo electrónico no válido.");
            main.shake(txtEmail);
            return;
        }

        if (pass.length() < 6) {
            main.shake(txtContrasena);
            alert("La contraseña debe tener mínimo 6 caracteres.");
            return;
        }

        if (!pass.equals(pass2)) {
            main.shake(txtContrasena);
            main.shake(txtContrasenaRep);
            alert("Las contraseñas no coinciden.");
            return;
        }

        AuthDAO dao = new AuthDAO();
        // -------------------------------------
        // VALIDAR SI USUARIO O EMAIL YA EXISTE
        // -------------------------------------

        // -------------------------------------
        // GUARDAR REGISTRO
        // -------------------------------------
        String hash = HashUtil.sha256(pass);

        Auth nuevo = new Auth();
        nuevo.setUsername(user);
        nuevo.setEmail(email);
        nuevo.setPasswordHash(hash);

        dao.create(nuevo);

        if (dao.existsUser(user, email)) {
            alert("El usuario o email ya están registrados.");
            return;
        }


        alert("Cuenta creada con éxito 😊");

            main.slideOutToRight(panelRegister, () -> {
                main.loadPanel("/interfaz/panelLogin.fxml");
            });
    }

    @FXML
    private void onBack() {
        main.changeWithSlide(panelRegister, "/interfaz/panelLogin.fxml");
    }



    private void alert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }


}
