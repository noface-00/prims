package api;

import com.google.gson.*;
import utils.ErrorHandler;
import utils.NotificationManager;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class conect_API_eBay {

    private static final String BASE_URL = "https://api.ebay.com/buy/browse/v1/item_summary/search";
    private static final String BASE_URL_PRO = "https://api.ebay.com/buy/browse/v1/item/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))  // Timeout de conexión
            .build();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ═══════════════════════════════════════════════════════
    // 🔧 CONFIGURACIÓN Y CONSTANTES
    // ═══════════════════════════════════════════════════════

    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_SECONDS = 30;

    public conect_API_eBay() {
    }

    /**
     * 🔑 Obtiene token de acceso con manejo de errores
     */
    public static String getAccesToken(String CLIENT_ID, String CLIENT_SECRET) {
        try {
            return ErrorHandler.retryOperation(() -> {
                return getAccessTokenInternal(CLIENT_ID, CLIENT_SECRET);
            }, MAX_RETRIES, "obtener token de eBay");

        } catch (Exception e) {
            ErrorHandler.handleApiError(e, "obtener token de autenticación");
            return null;
        }
    }

    /**
     * Implementación interna de obtención de token
     */
    private static String getAccessTokenInternal(String CLIENT_ID, String CLIENT_SECRET) throws Exception {
        String tokenUrl = "https://api.ebay.com/identity/v1/oauth2/token";
        String scope = "https://api.ebay.com/oauth/api_scope";

        String body = "grant_type=" + URLEncoder.encode("client_credentials", "UTF-8")
                + "&scope=" + URLEncoder.encode(scope, "UTF-8");

        String credentials = Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + credentials)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && response.body().contains("\"access_token\"")) {
            String json = response.body();
            int start = json.indexOf("\"access_token\":\"") + 16;
            int end = json.indexOf("\"", start);
            System.out.println("✅ Conexión exitosa con la API de eBay");
            return json.substring(start, end);
        } else {
            throw new IOException("Error HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * 🔍 Busca productos con manejo robusto de errores
     */
    /**
     * 🔍 Busca productos aplicando filtros avanzados.
     */
    public static JsonArray browseProducts(
            String token,
            String palabra,
            int limite,
            Double minPrice,
            Double maxPrice,
            String condition,
            String category
    ) {
        try {
            if (token == null || token.isEmpty()) {
                NotificationManager.error("🔑 Token de autenticación no válido");
                return new JsonArray();
            }

            if (palabra == null || palabra.trim().isEmpty()) {
                NotificationManager.warning("⚠️ Debes ingresar un término de búsqueda");
                return new JsonArray();
            }

            return ErrorHandler.retryOperation(() ->
                            browseProductsInternal(
                                    token, palabra, limite,
                                    minPrice, maxPrice, condition, category
                            ),
                    2,
                    "buscar productos en eBay");

        } catch (Exception e) {
            ErrorHandler.handleSearchError(e);
            return new JsonArray();
        }
    }

    /**
     * Implementación interna de búsqueda de productos
     */
    private static JsonArray browseProductsInternal(
            String token,
            String palabra,
            int limite,
            Double minPrice,
            Double maxPrice,
            String condition,
            String category
    ) throws Exception {
        JsonArray resultados = new JsonArray();

        String query = URLEncoder.encode(palabra, StandardCharsets.UTF_8);
        String url = BASE_URL + "?q=" + query + "&limit=" + limite + "&offset=0";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_US")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        // Verificar código de respuesta
        if (response.statusCode() != 200) {
            throw new IOException("Error HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        if (json.has("itemSummaries")) {
            JsonArray items = json.getAsJsonArray("itemSummaries");
            System.out.println("✅ Encontrados " + items.size() + " resultados para: " + palabra);

            for (JsonElement itemElem : items) {
                JsonObject item = itemElem.getAsJsonObject();
                JsonObject nuevo = new JsonObject();

                try {
                    // ID y título
                    nuevo.addProperty("itemId",
                            item.has("itemId") ? item.get("itemId").getAsString().replace("|", "%7C") : "");
                    nuevo.addProperty("title",
                            item.has("title") ? item.get("title").getAsString() : "Sin título");

                    // Precio
                    if (item.has("price") && item.get("price").isJsonObject()) {
                        JsonObject price = item.getAsJsonObject("price");
                        nuevo.addProperty("price",
                                price.has("value") ? price.get("value").getAsString() : "0");
                        nuevo.addProperty("currency",
                                price.has("currency") ? price.get("currency").getAsString() : "USD");
                    }

                    // Categorías
                    if (item.has("categories") && item.get("categories").isJsonArray()) {
                        JsonArray categorias = new JsonArray();
                        for (JsonElement cat : item.getAsJsonArray("categories")) {
                            if (cat.isJsonObject()) {
                                JsonObject catObj = cat.getAsJsonObject();
                                JsonObject simpleCat = new JsonObject();
                                simpleCat.addProperty("categoryId",
                                        catObj.has("categoryId") ? catObj.get("categoryId").getAsString() : "");
                                simpleCat.addProperty("categoryName",
                                        catObj.has("categoryName") ? catObj.get("categoryName").getAsString() : "");
                                categorias.add(simpleCat);
                            }
                        }
                        nuevo.add("categories", categorias);
                    }

                    // Condición
                    if (item.has("conditionId") && item.has("condition")) {
                        JsonObject condicion = new JsonObject();
                        condicion.addProperty("conditionId", item.get("conditionId").getAsString());
                        condicion.addProperty("condition", item.get("condition").getAsString());
                        nuevo.add("condition", condicion);
                    }

                    // Imágenes
                    JsonArray allImages = new JsonArray();
                    if (item.has("image") && item.get("image").isJsonObject()) {
                        JsonObject img = item.getAsJsonObject("image");
                        if (img.has("imageUrl")) {
                            allImages.add(img.get("imageUrl").getAsString());
                        }
                    }
                    if (item.has("additionalImages") && item.get("additionalImages").isJsonArray()) {
                        JsonArray adicionales = item.getAsJsonArray("additionalImages");
                        for (JsonElement addElem : adicionales) {
                            if (addElem.isJsonObject()) {
                                JsonObject add = addElem.getAsJsonObject();
                                if (add.has("imageUrl")) {
                                    allImages.add(add.get("imageUrl").getAsString());
                                }
                            }
                        }
                    }
                    if (allImages.size() == 0) {
                        allImages.add("Sin imágenes disponibles");
                    }
                    nuevo.add("images", allImages);

                    // Vendedor
                    if (item.has("seller") && item.get("seller").isJsonObject()) {
                        JsonObject seller = item.getAsJsonObject("seller");
                        JsonObject vendedor = new JsonObject();
                        vendedor.addProperty("username",
                                seller.has("username") ? seller.get("username").getAsString() : "Desconocido");
                        vendedor.addProperty("feedbackPercentage",
                                seller.has("feedbackPercentage") ? seller.get("feedbackPercentage").getAsString() : "N/A");
                        vendedor.addProperty("feedbackScore",
                                seller.has("feedbackScore") ? seller.get("feedbackScore").getAsInt() : 0);
                        nuevo.add("seller", vendedor);
                    }

                    // Otros campos
                    nuevo.addProperty("topRatedBuyingExperience",
                            item.has("topRatedBuyingExperience") && item.get("topRatedBuyingExperience").isJsonPrimitive()
                                    ? item.get("topRatedBuyingExperience").getAsBoolean() : false);

                    if (item.has("itemCreationDate") && item.get("itemCreationDate").isJsonPrimitive()) {
                        nuevo.addProperty("itemCreationDate", item.get("itemCreationDate").getAsString());
                    }

                    nuevo.addProperty("itemWebUrl",
                            item.has("itemWebUrl") ? item.get("itemWebUrl").getAsString() : "");

                    resultados.add(nuevo);

                } catch (Exception e) {
                    ErrorHandler.logWarning("Error procesando item: " + e.getMessage());
                    // Continuar con el siguiente item
                }
            }
        } else {
            System.out.println("⚠️ No se encontraron productos para: " + palabra);
        }

        return resultados;
    }

    /**
     * 📦 Obtiene información adicional del producto
     */
    public static JsonArray aditional_info_pro(String token, String itemId) {
        try {
            if (token == null || token.isEmpty()) {
                NotificationManager.error("🔑 Token no válido");
                return new JsonArray();
            }

            if (itemId == null || itemId.isEmpty()) {
                NotificationManager.warning("⚠️ ID de producto no válido");
                return new JsonArray();
            }

            return ErrorHandler.retryOperation(() -> {
                return getAdditionalInfoInternal(token, itemId);
            }, 2, "obtener información adicional del producto");

        } catch (Exception e) {
            ErrorHandler.handleApiError(e, "cargar detalles del producto");
            return new JsonArray();
        }
    }

    /**
     * Implementación interna de información adicional
     */
    private static JsonArray getAdditionalInfoInternal(String token, String itemId) throws Exception {
        JsonArray resultados = new JsonArray();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL_PRO + itemId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject item = gson.fromJson(response.body(), JsonObject.class);
        JsonObject nuevo = new JsonObject();

        // Procesar campos (tu código actual aquí, pero con manejo de errores)
        try {
            // Descripción corta
            nuevo.addProperty("shortDescription",
                    item.has("shortDescription") && !item.get("shortDescription").isJsonNull()
                            ? item.get("shortDescription").getAsString()
                            : "Sin descripción disponible");

            // Opciones de envío
            if (item.has("shippingOptions") && item.get("shippingOptions").isJsonArray()) {
                JsonArray shippingArray = new JsonArray();
                for (JsonElement shipElem : item.getAsJsonArray("shippingOptions")) {
                    // ... tu código de procesamiento
                }
                nuevo.add("shippingOptions", shippingArray);
            }

            // ... resto de campos

            resultados.add(nuevo);

        } catch (Exception e) {
            ErrorHandler.logWarning("Error procesando detalles del producto: " + e.getMessage());
        }

        return resultados;
    }

    /**
     * 🏷️ Busca productos por vendedor con manejo de errores
     */
    public JsonArray buscarProductosPorVendedor(String token, String sellerUsername, int limit) {
        try {
            if (token == null || sellerUsername == null || sellerUsername.isEmpty()) {
                NotificationManager.warning("⚠️ Datos de búsqueda inválidos");
                return new JsonArray();
            }

            return ErrorHandler.retryOperation(() -> {
                return searchBySellerInternal(token, sellerUsername, limit);
            }, 2, "buscar productos del vendedor");

        } catch (Exception e) {
            ErrorHandler.handleApiError(e, "buscar productos del vendedor");
            return new JsonArray();
        }
    }

    private JsonArray searchBySellerInternal(String token, String sellerUsername, int limit) throws Exception {
        JsonArray resultados = new JsonArray();

        String endpoint = "https://api.ebay.com/buy/browse/v1/item_summary/search";
        String url = endpoint + "?q=" + URLEncoder.encode(sellerUsername, StandardCharsets.UTF_8)
                + "&limit=" + limit;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_US")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.has("itemSummaries")) {
                resultados = json.getAsJsonArray("itemSummaries");
            }
        } else {
            throw new IOException("Error HTTP " + response.statusCode());
        }

        return resultados;
    }

    /**
     * 💰 Obtiene precios del mercado con manejo de errores
     */
    public static java.util.List<Double> obtenerPreciosDelMercado(String query, String token) {
        java.util.List<Double> precios = new java.util.ArrayList<>();

        try {
            JsonArray items = browseProducts(token, query, 50,null,null,null,null);

            for (JsonElement elem : items) {
                try {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.has("price")) {
                        String priceStr = obj.get("price").getAsString();
                        double price = Double.parseDouble(priceStr);
                        if (price > 0) {
                            precios.add(price);
                        }
                    }
                } catch (Exception e) {
                    // Ignorar items con precio inválido
                    ErrorHandler.logWarning("Precio inválido en item: " + e.getMessage());
                }
            }

            if (precios.isEmpty()) {
                NotificationManager.info("ℹ️ No se encontraron precios válidos para análisis");
            }

        } catch (Exception e) {
            ErrorHandler.handleApiError(e, "obtener precios del mercado");
        }

        return precios;
    }
}