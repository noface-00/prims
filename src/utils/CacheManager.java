package utils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de caché para optimizar consultas repetidas.
 * Reduce llamadas a la BD y a la API.
 */
public class CacheManager {

    // Tiempo de vida del caché (5 minutos)
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    // Estructura para almacenar datos con timestamp
    private static class CacheEntry<T> {
        T data;
        long timestamp;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL_MS;
        }
    }

    // Cachés específicos
    private static final Map<String, CacheEntry<Object>> genericCache = new ConcurrentHashMap<>();

    /**
     * Guarda un valor en caché con una clave
     */
    public static <T> void put(String key, T value) {
        if (key != null && value != null) {
            genericCache.put(key, new CacheEntry<>(value));
        }
    }

    /**
     * Obtiene un valor del caché si existe y no ha expirado
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type) {
        CacheEntry<Object> entry = genericCache.get(key);

        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            genericCache.remove(key);
            return null;
        }

        try {
            return type.cast(entry.data);
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Verifica si existe un valor en caché y no ha expirado
     */
    public static boolean has(String key) {
        CacheEntry<Object> entry = genericCache.get(key);

        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            genericCache.remove(key);
            return false;
        }

        return true;
    }

    /**
     * Elimina una entrada del caché
     */
    public static void remove(String key) {
        genericCache.remove(key);
    }

    /**
     * Limpia todo el caché
     */
    public static void clear() {
        genericCache.clear();
    }

    /**
     * Limpia entradas expiradas
     */
    public static void cleanExpired() {
        genericCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Obtiene el tamaño actual del caché
     */
    public static int size() {
        return genericCache.size();
    }

    // ═══════════════════════════════════════════════════════
    // 🔹 MÉTODOS ESPECÍFICOS PARA TIPOS COMUNES
    // ═══════════════════════════════════════════════════════

    /**
     * Clave para precios de mercado
     */
    public static String marketPriceKey(String productName) {
        return "market_price:" + productName.toLowerCase().trim();
    }

    /**
     * Clave para imagen de producto
     */
    public static String imageKey(String itemId) {
        return "image:" + itemId;
    }

    /**
     * Clave para antigüedad de vendedor
     */
    public static String sellerAgeKey(String username) {
        return "seller_age:" + username;
    }

    /**
     * Clave para análisis de producto
     */
    public static String analysisKey(String itemId) {
        return "analysis:" + itemId;
    }
}
