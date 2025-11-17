package utils;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * 🛡️ Sistema centralizado de manejo de errores
 *
 * Convierte excepciones técnicas en mensajes amigables para el usuario
 * y registra errores detallados para debugging
 *
 * @author Kevin
 */
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);
    private static boolean showTechnicalDetails = false; // Cambiar a true en desarrollo

    // ═══════════════════════════════════════════════════════
    // 🌐 ERRORES DE API (eBay)
    // ═══════════════════════════════════════════════════════

    /**
     * Maneja errores relacionados con la API de eBay
     */
    public static void handleApiError(Exception e, String operation) {
        log.error("Error en operación de API: {}", operation, e);

        String userMessage = getUserFriendlyApiMessage(e, operation);
        String technicalDetails = getTechnicalDetails(e);

        Platform.runLater(() -> {
            NotificationManager.error(userMessage);

            if (showTechnicalDetails) {
                NotificationManager.info("Detalles: " + technicalDetails);
            }
        });
    }

    private static String getUserFriendlyApiMessage(Exception e, String operation) {
        String exceptionType = e.getClass().getSimpleName();
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // 🔴 Sin conexión a internet
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return "Sin conexión a internet. Verifica tu conexión y vuelve a intentar.";
        }

        // ⏱️ Timeout
        if (e instanceof SocketTimeoutException || message.contains("timeout")) {
            return "⏱️ La operación tardó demasiado. Intenta de nuevo.";
        }

        // 🔑 Problemas de autenticación (Token)
        if (message.contains("401") || message.contains("unauthorized") ||
                message.contains("invalid token") || message.contains("expired")) {
            return "Tu sesión expiró. Reinicia la aplicación.";
        }

        // 🚫 Acceso denegado
        if (message.contains("403") || message.contains("forbidden")) {
            return "🚫 No tienes permiso para realizar esta acción.";
        }

        // 🔍 No encontrado
        if (message.contains("404") || message.contains("not found")) {
            return "No se encontró el recurso solicitado.";
        }

        // ⚠️ Error del servidor (500)
        if (message.contains("500") || message.contains("internal server error")) {
            return "El servidor de eBay tiene problemas. Intenta más tarde.";
        }

        // 🛑 Rate limit (demasiadas peticiones)
        if (message.contains("429") || message.contains("too many requests") ||
                message.contains("rate limit")) {
            return "Demasiadas solicitudes. Espera un momento antes de continuar.";
        }

        // ❌ Solicitud incorrecta
        if (message.contains("400") || message.contains("bad request")) {
            return "Solicitud incorrecta. Verifica los datos ingresados.";
        }

        // 📡 Problemas de red
        if (message.contains("network") || message.contains("connection")) {
            return "Problemas de red. Verifica tu conexión.";
        }

        // 🔄 Error genérico de API
        return "Error al comunicarse con eBay. " +
                "Intenta de nuevo en unos momentos.";
    }

    // ═══════════════════════════════════════════════════════
    // 🗄️ ERRORES DE BASE DE DATOS
    // ═══════════════════════════════════════════════════════

    /**
     * Maneja errores relacionados con la base de datos
     */
    public static void handleDatabaseError(Exception e, String operation) {
        log.error("Error en operación de BD: {}", operation, e);

        String userMessage = getUserFriendlyDatabaseMessage(e, operation);
        String technicalDetails = getTechnicalDetails(e);

        Platform.runLater(() -> {
            NotificationManager.error(userMessage);

            if (showTechnicalDetails) {
                NotificationManager.warning("Detalles técnicos: " + technicalDetails);
            }
        });
    }

    private static String getUserFriendlyDatabaseMessage(Exception e, String operation) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String exceptionType = e.getClass().getSimpleName();

        // 🔌 Sin conexión a la base de datos
        if (e instanceof SQLException &&
                (message.contains("connection") || message.contains("communications link failure"))) {
            return "No se pudo conectar a la base de datos. " +
                    "Verifica que el servidor esté activo.";
        }

        // ⏱️ Timeout en consulta
        if (e instanceof SQLTimeoutException || message.contains("timeout")) {
            return "La consulta tardó demasiado. " +
                    "La base de datos puede estar sobrecargada.";
        }

        // 🔑 Error de autenticación
        if (message.contains("access denied") || message.contains("authentication failed")) {
            return "Error de autenticación con la base de datos. " +
                    "Contacta al administrador.";
        }

        // 🔄 Violación de clave duplicada
        if (message.contains("duplicate") || message.contains("unique constraint")) {
            return "Este registro ya existe en la base de datos.";
        }

        // 🔗 Violación de clave foránea
        if (message.contains("foreign key") || message.contains("constraint")) {
            return "🔗 No se puede completar la operación debido a " +
                    "restricciones de integridad de datos.";
        }

        // 📊 Tabla no existe
        if (message.contains("table") && message.contains("doesn't exist")) {
            return "Error en la estructura de la base de datos. " +
                    "Contacta al administrador.";
        }

        // 💾 Error de sintaxis SQL
        if (message.contains("syntax error") || message.contains("sql syntax")) {
            return "Error interno en la consulta de base de datos.";
        }

        // 🚫 Permiso denegado
        if (message.contains("permission denied") || message.contains("access denied")) {
            return "No tienes permisos suficientes en la base de datos.";
        }

        // 📦 Base de datos llena
        if (message.contains("disk full") || message.contains("out of space")) {
            return "📦 La base de datos está llena. Contacta al administrador.";
        }

        // 🔄 Transacción fallida
        if (message.contains("rollback") || message.contains("transaction")) {
            return "La operación fue cancelada para mantener " +
                    "la integridad de los datos.";
        }

        // ❌ Error genérico de BD
        return "Error en la base de datos al " + operation + ". " +
                "Intenta de nuevo o contacta al soporte.";
    }

    // ═══════════════════════════════════════════════════════
    // 🔧 ERRORES GENERALES
    // ═══════════════════════════════════════════════════════

    /**
     * Maneja errores generales de la aplicación
     */
    public static void handleGeneralError(Exception e, String operation) {
        log.error("Error general: {}", operation, e);

        String userMessage = getUserFriendlyGeneralMessage(e, operation);

        Platform.runLater(() -> {
            NotificationManager.error(userMessage);
        });
    }

    private static String getUserFriendlyGeneralMessage(Exception e, String operation) {
        String exceptionType = e.getClass().getSimpleName();
        String message = e.getMessage() != null ? e.getMessage() : "";

        // NullPointerException
        if (e instanceof NullPointerException) {
            return "Error interno: datos no encontrados. " +
                    "Intenta recargar la aplicación.";
        }

        // NumberFormatException
        if (e instanceof NumberFormatException) {
            return "Formato de número inválido. " +
                    "Verifica los datos ingresados.";
        }

        // IllegalArgumentException
        if (e instanceof IllegalArgumentException) {
            return "Datos inválidos proporcionados. " +
                    "Verifica la información ingresada.";
        }

        // ClassCastException
        if (e instanceof ClassCastException) {
            return "Error interno de conversión de datos.";
        }

        // IOException
        if (e instanceof java.io.IOException) {
            return "Error de lectura/escritura de archivos.";
        }

        return "Error inesperado al " + operation + ". " +
                "Intenta de nuevo.";
    }

    // ═══════════════════════════════════════════════════════
    // 📋 UTILIDADES
    // ═══════════════════════════════════════════════════════

    /**
     * Obtiene detalles técnicos de la excepción
     */
    private static String getTechnicalDetails(Exception e) {
        StringBuilder details = new StringBuilder();
        details.append(e.getClass().getSimpleName());

        if (e.getMessage() != null) {
            details.append(": ").append(e.getMessage());
        }

        if (e.getCause() != null) {
            details.append(" | Causa: ").append(e.getCause().getMessage());
        }

        return details.toString();
    }

    /**
     * Activa/desactiva la visualización de detalles técnicos
     */
    public static void setShowTechnicalDetails(boolean show) {
        showTechnicalDetails = show;
    }

    /**
     * Registra información en el log sin mostrar notificación
     */
    public static void logInfo(String message) {
        log.info(message);
    }

    /**
     * Registra advertencia en el log sin mostrar notificación
     */
    public static void logWarning(String message) {
        log.warn(message);
    }

    // ═══════════════════════════════════════════════════════
    // 🎯 MÉTODOS ESPECIALIZADOS
    // ═══════════════════════════════════════════════════════

    /**
     * Maneja errores en carga de productos
     */
    public static void handleProductLoadError(Exception e) {
        handleApiError(e, "cargar productos");
    }

    /**
     * Maneja errores en búsqueda
     */
    public static void handleSearchError(Exception e) {
        handleApiError(e, "realizar búsqueda");
    }

    /**
     * Maneja errores en guardado
     */
    public static void handleSaveError(Exception e) {
        handleDatabaseError(e, "guardar datos");
    }

    /**
     * Maneja errores de login
     */
    public static void handleLoginError(Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("credentials")) {
            Platform.runLater(() ->
                    NotificationManager.error("Usuario o contraseña incorrectos")
            );
        } else {
            handleDatabaseError(e, "iniciar sesión");
        }
    }

    /**
     * Maneja errores de análisis
     */
    public static void handleAnalysisError(Exception e) {
        log.error("Error en análisis de producto", e);
        Platform.runLater(() ->
                NotificationManager.error("Error al analizar producto. Intenta de nuevo.")
        );
    }

    /**
     * Maneja errores de generación de reportes
     */
    public static void handleReportError(Exception e) {
        log.error("Error generando reporte", e);
        Platform.runLater(() ->
                NotificationManager.error("Error al generar reporte PDF. Verifica los datos.")
        );
    }

    // ═══════════════════════════════════════════════════════
    // 🔄 RETRY AUTOMÁTICO
    // ═══════════════════════════════════════════════════════

    /**
     * Reintenta una operación automáticamente
     */
    public static <T> T retryOperation(
            RetryableOperation<T> operation,
            int maxRetries,
            String operationName
    ) throws Exception {

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();

            } catch (Exception e) {
                lastException = e;
                log.warn("Intento {} de {} falló para: {}",
                        attempt, maxRetries, operationName);

                if (attempt < maxRetries) {
                    // Esperar antes del siguiente intento (backoff exponencial)
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
                    Thread.sleep(Math.min(waitTime, 10000)); // máx 10 segundos
                }
            }
        }

        // Si llegamos aquí, todos los intentos fallaron
        throw lastException;
    }

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}