package utils;

import entities.Auth;

public class Sesion {
    private static Auth usuarioActual;
    private static String tokenAPI;
    private static long tokenExpireTime;

    public static void iniciar(Auth usuario){
        usuarioActual = usuario;
    }

    public static Auth getUsuario() {
        return usuarioActual;
    }

    public static void setTokenAPI(String token, long nuevaExpiracion) {
        tokenAPI = token;
    }
    public static String getTokenAPI() {
        return tokenAPI;
    }

    public static long getTokenExpireTime() {
        return tokenExpireTime;
    }
    public static void cerrar(){
        usuarioActual = null;
        tokenAPI = null;
    }
}
