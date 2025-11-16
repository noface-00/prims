package utils;

import api.conect_API_eBay;
import keys.config;

public class TokenManager {

    private static final String CLIENT_ID = config.getProperty("CLIENT_ID");
    private static final String CLIENT_SECRET = config.getProperty("CLIENT_SECRET");

    public static boolean isTokenExpired() {
        long now = System.currentTimeMillis();
        return now >= Sesion.getTokenExpireTime();
    }

    // 🔥 ESTE MÉTODO AHORA RETORNA EL TOKEN
    public static void refreshToken() {

        try {

            // SI NO hay token, O ya expiró → generar uno nuevo
            if (Sesion.getTokenAPI() == null || isTokenExpired()) {

                System.out.println("🔄 Generando nuevo token...");

                String nuevoToken = obtenerNuevoToken();
                long expiration = System.currentTimeMillis() + (60 * 60 * 1000);

                // ✔ Guardar token en sesión
                Sesion.setTokenAPI(nuevoToken, expiration);

                System.out.println("🔑 Token renovado correctamente.");
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Error renovando token: " + e.getMessage(), e);
        }
    }


    // 🔥 Método REAL de obtener token desde eBay
    private static String obtenerNuevoToken() throws Exception {

        conect_API_eBay api = new conect_API_eBay();
        String token = api.getAccesToken(CLIENT_ID, CLIENT_SECRET);

        return token;
    }
}
