package utils;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 🔔 Sistema de notificaciones toast para JavaFX (VERSIÓN CORREGIDA)
 *
 * SOLUCIÓN:
 * - Las notificaciones se muestran DENTRO de la ventana principal
 * - No crea ventanas nuevas (Stage)
 * - Funciona correctamente en pantalla completa
 * - Sistema de cola para múltiples notificaciones
 *
 * @author Kevin
 */
public class NotificationManager {

    private static StackPane notificationContainer;
    private static final Queue<NotificationData> notificationQueue = new LinkedList<>();
    private static boolean isShowingNotification = false;
    private static final int MAX_WIDTH = 400;

    public enum NotificationType {
        SUCCESS("", "#7A7A78", "#FFFFFF"),
        ERROR("", "#7A7A78", "#FFFFFF"),
        WARNING("", "#7A7A78", "#FFFFFF"),
        INFO("", "#7A7A78", "#FFFFFF"),
        LOADING("", "#7A7A78", "#FFFFFF");

        private final String icon;
        private final String backgroundColor;
        private final String textColor;

        NotificationType(String icon, String backgroundColor, String textColor) {
            this.icon = icon;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }

        public String getIcon() { return icon; }
        public String getBackgroundColor() { return backgroundColor; }
        public String getTextColor() { return textColor; }
    }

    private static class NotificationData {
        String message;
        NotificationType type;
        int durationMs;

        NotificationData(String message, NotificationType type, int durationMs) {
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
        }
    }

    /**
     * 🔧 INICIALIZACIÓN: Debe llamarse desde MainController
     * Configura el contenedor donde aparecerán las notificaciones
     */
    public static void initialize(StackPane mainContainer) {
        if (notificationContainer == null) {
            notificationContainer = new StackPane();
            notificationContainer.setPickOnBounds(false);
            notificationContainer.setMouseTransparent(true);
            notificationContainer.setMaxWidth(Region.USE_PREF_SIZE);
            notificationContainer.setMaxHeight(Region.USE_PREF_SIZE);

            // Posicionar en la esquina superior derecha
            StackPane.setAlignment(notificationContainer, Pos.TOP_RIGHT);
            StackPane.setMargin(notificationContainer, new Insets(20, 20, 0, 0));
            notificationContainer.setPickOnBounds(false);
            mainContainer.getChildren().add(notificationContainer);

            System.out.println("✅ NotificationManager inicializado");
        }
    }

    /**
     * Muestra una notificación toast
     */
    public static void show(String message, NotificationType type) {
        show(message, type, 3000);
    }

    /**
     * Muestra una notificación toast con duración personalizada
     */
    public static void show(String message, NotificationType type, int durationMs) {
        if (notificationContainer == null) {
            System.err.println("⚠️ NotificationManager no inicializado. Llama a initialize() primero.");
            return;
        }

        NotificationData data = new NotificationData(message, type, durationMs);
        notificationQueue.offer(data);

        if (!isShowingNotification) {
            processQueue();
        }
    }

    /**
     * Procesa la cola de notificaciones
     */
    private static void processQueue() {
        if (notificationQueue.isEmpty()) {
            isShowingNotification = false;
            return;
        }

        isShowingNotification = true;
        NotificationData data = notificationQueue.poll();

        Platform.runLater(() -> showNotificationInternal(data));
    }

    /**
     * Muestra la notificación interna
     */
    private static void showNotificationInternal(NotificationData data) {
        // Contenedor de la notificación
        VBox notificationBox = new VBox(5);
        notificationBox.setMaxWidth(400);
        notificationBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setMaxWidth(MAX_WIDTH);
        notificationBox.setPadding(new Insets(15, 25, 15, 25));
        notificationBox.setStyle(
                "-fx-background-color: " + data.type.getBackgroundColor() + ";" +
                        "-fx-background-radius: 5;"
        );

        // Etiqueta del mensaje
        Label label = new Label(data.type.getIcon() + " " + data.message);
        label.setWrapText(true);
        label.setMaxWidth(MAX_WIDTH - 50);
        label.setStyle(
                "-fx-text-fill: " + data.type.getTextColor() + ";" +
                        "-fx-font-family: 'Poppins SemiBold', 'System', 'Arial';" +
                        "-fx-font-size: 14px;"
        );

        notificationBox.getChildren().add(label);

        // Permitir hacer clic para cerrar
        notificationBox.setPickOnBounds(true);
        notificationBox.setMouseTransparent(false);
        notificationBox.setOnMouseClicked(e -> {
            hideNotification(notificationBox);
        });

        // Agregar al contenedor
        notificationContainer.getChildren().add(notificationBox);

        // ═══════════════════════════════════════
        // 🎬 ANIMACIONES
        // ═══════════════════════════════════════

        // Animación de entrada
        notificationBox.setOpacity(0);
        notificationBox.setTranslateX(50);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notificationBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notificationBox);
        slideIn.setFromX(50);
        slideIn.setToX(0);

        ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);

        // Pausa antes de salir
        PauseTransition pause = new PauseTransition(Duration.millis(data.durationMs));

        // Animación de salida
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), notificationBox);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), notificationBox);
        slideOut.setFromX(0);
        slideOut.setToX(50);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        exit.setOnFinished(e -> {
            notificationContainer.getChildren().remove(notificationBox);
            processQueue(); // Mostrar siguiente notificación
        });

        // Secuencia completa
        SequentialTransition sequence = new SequentialTransition(entrance, pause, exit);
        sequence.play();
    }

    /**
     * Cierra una notificación manualmente
     */
    private static void hideNotification(Node notification) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), notification);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            notificationContainer.getChildren().remove(notification);
            processQueue();
        });
        fadeOut.play();
    }

    // ═══════════════════════════════════════════════════════
    // 🎯 MÉTODOS DE CONVENIENCIA
    // ═══════════════════════════════════════════════════════

    public static void success(String message) {
        show(message, NotificationType.SUCCESS);
    }

    public static void error(String message) {
        show(message, NotificationType.ERROR);
    }

    public static void warning(String message) {
        show(message, NotificationType.WARNING);
    }

    public static void info(String message) {
        show(message, NotificationType.INFO);
    }

    public static void loading(String message) {
        show(message, NotificationType.LOADING, 2000);
    }

    // ═══════════════════════════════════════════════════════
    // 📊 NOTIFICACIONES DE PROGRESO PERSISTENTES
    // ═══════════════════════════════════════════════════════

    private static VBox progressNotification;

    /**
     * Muestra notificación de progreso que no se cierra automáticamente
     */
    public static void showProgress(String message) {
        if (notificationContainer == null) {
            System.err.println("⚠️ NotificationManager no inicializado.");
            return;
        }

        Platform.runLater(() -> {
            // Cerrar progreso anterior si existe
            if (progressNotification != null) {
                notificationContainer.getChildren().remove(progressNotification);
            }

            progressNotification = new VBox(10);
            progressNotification.setAlignment(Pos.CENTER);
            progressNotification.setMaxWidth(MAX_WIDTH);
            progressNotification.setPadding(new Insets(15, 25, 15, 25));
            progressNotification.setStyle(
                    "-fx-background-color: #2196F3;" +
                            "-fx-background-radius: 10;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);"
            );

            Label label = new Label("⏳ " + message);
            label.setWrapText(true);
            label.setMaxWidth(MAX_WIDTH - 50);
            label.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-family: 'Poppins SemiBold', 'System', 'Arial';" +
                            "-fx-font-size: 14px;"
            );

            // Barra de progreso indeterminada
            javafx.scene.control.ProgressIndicator progress =
                    new javafx.scene.control.ProgressIndicator();
            progress.setMaxSize(30, 30);
            progress.setStyle("-fx-progress-color: white;");

            progressNotification.getChildren().addAll(label, progress);
            notificationContainer.getChildren().add(progressNotification);

            // Animación de entrada
            progressNotification.setOpacity(0);
            progressNotification.setTranslateX(50);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), progressNotification);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), progressNotification);
            slideIn.setFromX(50);
            slideIn.setToX(0);

            new ParallelTransition(fadeIn, slideIn).play();
        });
    }

    /**
     * Cierra la notificación de progreso
     */
    public static void closeProgress() {
        if (progressNotification == null) return;

        Platform.runLater(() -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), progressNotification);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), progressNotification);
            slideOut.setFromX(0);
            slideOut.setToX(50);

            ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
            exit.setOnFinished(e -> {
                notificationContainer.getChildren().remove(progressNotification);
                progressNotification = null;
            });
            exit.play();
        });
    }

    /**
     * Actualiza el mensaje de la notificación de progreso
     */
    public static void updateProgress(String newMessage) {
        if (progressNotification == null) {
            showProgress(newMessage);
            return;
        }

        Platform.runLater(() -> {
            Label label = (Label) progressNotification.getChildren().get(0);
            label.setText("⏳ " + newMessage);
        });
    }

    /**
     * Limpia todas las notificaciones
     */
    public static void clearAll() {
        if (notificationContainer != null) {
            Platform.runLater(() -> {
                notificationContainer.getChildren().clear();
                notificationQueue.clear();
                isShowingNotification = false;
                progressNotification = null;
            });
        }
    }
}