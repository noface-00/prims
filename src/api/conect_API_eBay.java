package api;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class conect_API_eBay {

    private static final String BASE_URL = "https://api.ebay.com/buy/browse/v1/item_summary/search";
    private static final String BASE_URL_PRO = "https://api.ebay.com/buy/browse/v1/item/";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public conect_API_eBay() {
    }

    private static String makeRequest(String url, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_US")
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            System.err.println("Error HTTP: " + response.statusCode());
            System.err.println("Detalles: " + response.body());
            throw new RuntimeException("Fallo al obtener datos desde eBay API");
        }
    }

    public static String getAccesToken(String CLIENT_ID, String CLIENT_SECRET) throws Exception {
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
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && response.body().contains("\"access_token\"")) {
            String json = response.body();
            int start = json.indexOf("\"access_token\":\"") + 16;
            int end = json.indexOf("\"", start);
            System.out.println("Conexion correcta con la API");
            return json.substring(start, end);
        } else {
            System.err.println("Error al obtener token: " + response.statusCode());
            System.err.println(response.body());
            return null;
        }
    }

    public static JsonArray browseProducts(String token, String palabra, int limite) {
        JsonArray resultados = new JsonArray();

        try {
            String query = URLEncoder.encode(palabra, StandardCharsets.UTF_8);
            String url = BASE_URL + "?q=" + query + "&limit=" + limite + "&offset=0";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_US")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);

                if (json.has("itemSummaries")) {
                    JsonArray items = json.getAsJsonArray("itemSummaries");

                    System.out.println("Resultados para: " + palabra);

                    for (JsonElement itemElem : items) {
                        JsonObject item = itemElem.getAsJsonObject();
                        JsonObject nuevo = new JsonObject();

                        // ID y título
                        nuevo.addProperty("itemId", (item.get("itemId").getAsString()).replace("|", "%7C"));
                        nuevo.addProperty("title", item.has("title") ? item.get("title").getAsString() : "Sin título");

                        // Precio
                        if (item.has("price")) {
                            JsonObject price = item.getAsJsonObject("price");
                            nuevo.addProperty("price", price.get("value").getAsString());
                            nuevo.addProperty("currency", price.get("currency").getAsString());
                        }

                        // 🏷️ Categorías
                        if (item.has("categories")) {
                            JsonArray categorias = new JsonArray();
                            for (JsonElement cat : item.getAsJsonArray("categories")) {
                                JsonObject catObj = cat.getAsJsonObject();
                                JsonObject simpleCat = new JsonObject();
                                simpleCat.addProperty("categoryId", catObj.get("categoryId").getAsString());
                                simpleCat.addProperty("categoryName", catObj.get("categoryName").getAsString());
                                categorias.add(simpleCat);
                            }
                            nuevo.add("categories", categorias);
                        }
                        // Condicion
                        if (item.has("conditionId") && item.has("condition")) {
                            JsonObject condicion = new JsonObject();
                            condicion.addProperty("conditionId", item.get("conditionId").getAsString());
                            condicion.addProperty("condition", item.get("condition").getAsString());
                            nuevo.add("condition", condicion);
                        }

                        // 🖼️ Guardar TODAS las imágenes del producto
                        JsonArray allImages = new JsonArray();

                        // Imagen principal (si existe)
                        if (item.has("image")) {
                            JsonObject img = item.getAsJsonObject("image");
                            if (img.has("imageUrl")) {
                                allImages.add(img.get("imageUrl").getAsString());
                            }
                        }

                        // Imágenes adicionales (additionalImages)
                        if (item.has("additionalImages")) {
                            JsonArray adicionales = item.getAsJsonArray("additionalImages");
                            for (JsonElement addElem : adicionales) {
                                JsonObject add = addElem.getAsJsonObject();
                                if (add.has("imageUrl")) {
                                    allImages.add(add.get("imageUrl").getAsString());
                                }
                            }
                        }

                        // Si no se encontró ninguna imagen
                        if (allImages.size() == 0) {
                            allImages.add("Sin imágenes disponibles");
                        }

                        // Guardar todas las imágenes en el objeto final
                        nuevo.add("images", allImages);


                        // 👤 Vendedor
                        if (item.has("seller")) {
                            JsonObject seller = item.getAsJsonObject("seller");
                            JsonObject vendedor = new JsonObject();
                            vendedor.addProperty("username", seller.has("username") ? seller.get("username").getAsString() : "Desconocido");
                            vendedor.addProperty("feedbackPercentage", seller.has("feedbackPercentage") ? seller.get("feedbackPercentage").getAsString() : "N/A");
                            vendedor.addProperty("feedbackScore", seller.has("feedbackScore") ? seller.get("feedbackScore").getAsInt() : 0);
                            nuevo.add("seller", vendedor);
                        }


                        // Mejor compra de experencia
                        if (item.has("topRatedBuyingExperience") && item.get("topRatedBuyingExperience").isJsonPrimitive()) {
                            boolean topRated = item.get("topRatedBuyingExperience").getAsBoolean();
                            nuevo.addProperty("topRatedBuyingExperience", topRated);
                        } else {
                            // Si el campo no existe, puedes establecer un valor por defecto
                            nuevo.addProperty("topRatedBuyingExperience", false);
                        }

                        // Fecha de creacion de item
                        if (item.has("itemCreationDate") && item.get("itemCreationDate").isJsonPrimitive()) {
                            String fechaCreacion = item.get("itemCreationDate").getAsString();
                            nuevo.addProperty("itemCreationDate", fechaCreacion);
                        }
                        // Url del producto
                        nuevo.addProperty("itemWebUrl", item.get("itemWebUrl").getAsString());
                        // Agregar producto filtrado al array final
                        resultados.add(nuevo);
                    }
                } else {
                    System.out.println("No se encontraron productos.");
                }
            } else {
                System.out.println("Error HTTP: " + response.statusCode());
                System.out.println("Detalles: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultados; // Devuelve solo los campos seleccionados
    }

    public static JsonArray aditional_info_pro(String token, String itemId) {
        JsonArray resultados = new JsonArray();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL_PRO + itemId))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject item = gson.fromJson(response.body(), JsonObject.class);
                JsonObject nuevo = new JsonObject();


                // Descripcion corta del producto
                if (item.has("shortDescription") && !item.get("shortDescription").isJsonNull()) {
                    nuevo.addProperty("shortDescription", item.get("shortDescription").getAsString());
                } else {
                    nuevo.addProperty("shortDescription", "Sin descripción disponible");
                }

                // Opciones de envío (shippingOptions)
                if (item.has("shippingOptions") && item.get("shippingOptions").isJsonArray()) {
                    JsonArray shippingArray = new JsonArray();

                    for (JsonElement shipElem : item.getAsJsonArray("shippingOptions")) {
                        JsonObject ship = shipElem.getAsJsonObject();
                        JsonObject envio = new JsonObject();

                        // Tipo de envío (Standard, Expedited, etc.)
                        envio.addProperty("type",
                                ship.has("type") ? ship.get("type").getAsString() : "N/A");

                        // Transportista (FedEx, USPS, etc.)
                        envio.addProperty("shippingCarrierCode",
                                ship.has("shippingCarrierCode") ? ship.get("shippingCarrierCode").getAsString() : "N/A");

                        // Costo de envío principal
                        if (ship.has("shippingCost") && ship.get("shippingCost").isJsonObject()) {
                            JsonObject cost = ship.getAsJsonObject("shippingCost");
                            JsonObject costoEnvio = new JsonObject();
                            costoEnvio.addProperty("value", cost.has("value") ? cost.get("value").getAsString() : "0.00");
                            costoEnvio.addProperty("currency", cost.has("currency") ? cost.get("currency").getAsString() : "USD");
                            envio.add("shippingCost", costoEnvio);
                        }

                        // Cantidad usada para el cálculo
                        envio.addProperty("quantityUsedForEstimate",
                                ship.has("quantityUsedForEstimate") ? ship.get("quantityUsedForEstimate").getAsInt() : 0);

                        // Fechas estimadas
                        envio.addProperty("minEstimatedDeliveryDate",
                                ship.has("minEstimatedDeliveryDate") ? ship.get("minEstimatedDeliveryDate").getAsString() : "N/A");
                        envio.addProperty("maxEstimatedDeliveryDate",
                                ship.has("maxEstimatedDeliveryDate") ? ship.get("maxEstimatedDeliveryDate").getAsString() : "N/A");

                        // Costo adicional por unidad
                        if (ship.has("additionalShippingCostPerUnit") && ship.get("additionalShippingCostPerUnit").isJsonObject()) {
                            JsonObject addCost = ship.getAsJsonObject("additionalShippingCostPerUnit");
                            JsonObject costoAdicional = new JsonObject();
                            costoAdicional.addProperty("value", addCost.has("value") ? addCost.get("value").getAsString() : "0.00");
                            costoAdicional.addProperty("currency", addCost.has("currency") ? addCost.get("currency").getAsString() : "USD");
                            envio.add("additionalShippingCostPerUnit", costoAdicional);
                        }

                        // Tipo de costo
                        envio.addProperty("shippingCostType",
                                ship.has("shippingCostType") ? ship.get("shippingCostType").getAsString() : "N/A");

                        shippingArray.add(envio);
                    }

                    nuevo.add("shippingOptions", shippingArray);
                }

                // Política de devoluciones
                if (item.has("returnTerms") && item.get("returnTerms").isJsonObject()) {
                    JsonObject returnTerms = item.getAsJsonObject("returnTerms");
                    boolean returnsAccepted = returnTerms.has("returnsAccepted") && returnTerms.get("returnsAccepted").getAsBoolean();
                    nuevo.addProperty("returnsAccepted", returnsAccepted);
                } else {
                    nuevo.addProperty("returnsAccepted", false);
                }
                // Cupones disponibles (availableCoupons)
                if (item.has("availableCoupons") && item.get("availableCoupons").isJsonArray()) {
                    JsonArray couponsArray = new JsonArray();

                    for (JsonElement couponElem : item.getAsJsonArray("availableCoupons")) {
                        if (couponElem.isJsonObject()) {
                            JsonObject coupon = couponElem.getAsJsonObject();
                            JsonObject nuevoCoupon = new JsonObject();

                            // Código del cupon
                            if (coupon.has("redemptionCode")) {
                                nuevoCoupon.addProperty("redemptionCode", coupon.get("redemptionCode").getAsString());
                            }

                            // Descuento
                            if (coupon.has("discountAmount") && coupon.get("discountAmount").isJsonObject()) {
                                JsonObject discount = coupon.getAsJsonObject("discountAmount");
                                String valor = discount.has("value") ? discount.get("value").getAsString() : "0.00";
                                nuevoCoupon.addProperty("discountAmount", valor);
                            }


                            // Restricciones (fecha de expiración)
                            if (coupon.has("constraint") && coupon.get("constraint").isJsonObject()) {
                                JsonObject constraint = coupon.getAsJsonObject("constraint");
                                if (constraint.has("expirationDate")) {
                                    nuevoCoupon.addProperty("expirationDate", constraint.get("expirationDate").getAsString());
                                }
                            }

                            couponsArray.add(nuevoCoupon);
                        }
                    }

                    nuevo.add("availableCoupons", couponsArray);

                }

                // Estado de disponibilidad
                if (item.has("estimatedAvailabilities")) {
                    JsonArray availArray = item.getAsJsonArray("estimatedAvailabilities");

                    if (!availArray.isEmpty()) {
                        JsonObject first = availArray.get(0).getAsJsonObject(); // Tomamos el primero
                        if (first.has("estimatedAvailabilityStatus")) {
                            nuevo.addProperty("status", first.get("estimatedAvailabilityStatus").getAsString());
                        }
                    }
                }
                // Guardar los atributos del producto
                if (item.has("localizedAspects") && item.get("localizedAspects").isJsonArray()) {
                    JsonArray aspectosLimpios = new JsonArray();

                    for (JsonElement aspectElem : item.getAsJsonArray("localizedAspects")) {
                        if (aspectElem.isJsonObject()) {
                            JsonObject aspect = aspectElem.getAsJsonObject();
                            JsonObject atributo = new JsonObject();

                            // Solo guarda name y value
                            if (aspect.has("name")) {
                                atributo.addProperty("name", aspect.get("name").getAsString());
                            }

                            if (aspect.has("value")) {
                                JsonElement valor = aspect.get("value");
                                // Si no es string, conviértelo a string de forma segura
                                if (valor.isJsonPrimitive()) {
                                    atributo.addProperty("value", valor.getAsString());
                                } else {
                                    atributo.addProperty("value", valor.toString());
                                }
                            }

                            // Solo agregar si tiene nombre
                            if (atributo.has("name")) {
                                aspectosLimpios.add(atributo);
                            }
                        }
                    }

                    nuevo.add("localizedAspects", aspectosLimpios);
                } else {
                    JsonArray vacio = new JsonArray();
                    vacio.add("Sin atributos disponibles");
                    nuevo.add("localizedAspects", vacio);
                }

                // Guardar todos los atributos
                resultados.add(nuevo);
            } else {
                System.err.println("Error HTTP: " + response.statusCode());
                System.err.println("Detalles: " + response.body());
            }

        } catch (
                Exception e) {
            e.printStackTrace();
        }

        return resultados; // Devuelve la información detallada del producto
    }

    public JsonArray buscarProductosPorVendedor(String token, String sellerUsername, int limit) {
        JsonArray resultados = new JsonArray();
        try {
            // ✅ Se usa "q" obligatorio para evitar 400 Bad Request
            String endpoint = "https://api.ebay.com/buy/browse/v1/item_summary/search";
            String url = endpoint + "?q=" + URLEncoder.encode(sellerUsername, StandardCharsets.UTF_8)
                    + "&limit=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .header("X-EBAY-C-MARKETPLACE-ID", "EBAY_US")
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                if (json.has("itemSummaries")) {
                    resultados = json.getAsJsonArray("itemSummaries");
                } else {
                    System.out.println("⚠️ No se encontraron itemSummaries para el vendedor: " + sellerUsername);
                }
            } else {
                System.err.println("❌ Error HTTP: " + response.statusCode());
                System.err.println("Detalles: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultados;
    }

    public static List<Double> obtenerPreciosDelMercado(String query, String token) {
        List<Double> precios = new ArrayList<>();

        try {
            JsonArray items = browseProducts(token, query, 50);

            for (JsonElement elem : items) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has("price")) {
                    String priceStr = obj.get("price").getAsString();

                    try {
                        double price = Double.parseDouble(priceStr);
                        if (price > 0) {
                            precios.add(price);
                        }
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return precios;
    }

}


