package utils;


import api.conect_API_eBay;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entities.*;
import keys.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class cls_browseEBAY {

    private static conect_API_eBay api = new conect_API_eBay();
    private static final Map<String, Map<String, String>> cacheAntiguedad = new HashMap<>();

    public cls_browseEBAY() {
    }

    public List<Producto> obtenerProductos(String token,String palabra) throws Exception {


        List<Producto> listaProductos = new ArrayList<>();

        JsonArray productos = api.browseProducts(token, palabra, 15);

        if (productos != null) {
            for (JsonElement elem : productos) {
                JsonObject producto = elem.getAsJsonObject();

                // =========================
                // DATOS BASE DEL PRODUCTO
                // =========================
                String idProducto = producto.has("itemId")
                        ? producto.get("itemId").getAsString().replace("|", "%7C")
                        : null;
                if (idProducto == null) continue;

                // ================
                // VENDEDOR
                // ================
                Seller vendedor = new Seller();
                if (producto.has("seller")) {
                    JsonObject seller = producto.getAsJsonObject("seller");
                    vendedor.setUsername(seller.has("username") ? seller.get("username").getAsString() : "Desconocido");
                    vendedor.setFeedbackScore(seller.has("feedbackScore") ? seller.get("feedbackScore").getAsInt() : 0);
                    vendedor.setFeedbackPorcentage(seller.has("feedbackPercentage")
                            ? Double.parseDouble(seller.get("feedbackPercentage").getAsString()) : 0.0);
                }

                // ==================
                // CATEGORÍA
                // ==================
                CategoryProduct categoria = new CategoryProduct();
                if (producto.has("categories")) {
                    JsonArray categorias = producto.getAsJsonArray("categories");
                    if (!categorias.isEmpty()) {
                        JsonObject cat = categorias.get(0).getAsJsonObject();
                        categoria.setIdCategory(cat.has("categoryId") ? Integer.parseInt(cat.get("categoryId").getAsString()) : 0);
                        categoria.setCategoryPath(cat.has("categoryName") ? cat.get("categoryName").getAsString() : "Sin categoría");
                    }
                } else {
                    categoria.setIdCategory(0);
                    categoria.setCategoryPath("Sin categoría");
                }

                // ==================
                // CONDICIÓN
                // ==================
                ConditionProduct conditionEntity = new ConditionProduct();
                if (producto.has("condition") && producto.get("condition").isJsonObject()) {
                    JsonObject cond = producto.getAsJsonObject("condition");
                    conditionEntity.setIdCondition(cond.has("conditionId") ? Integer.parseInt(cond.get("conditionId").getAsString()) : 0);
                    conditionEntity.setConditionPath(cond.has("condition") ? cond.get("condition").getAsString() : "Sin especificar");
                } else {
                    conditionEntity.setIdCondition(0);
                    conditionEntity.setConditionPath("Sin especificar");
                }

                // ===========================
                // PRODUCTO PRINCIPAL
                // ===========================
                boolean topRated = producto.has("topRatedBuyingExperience")
                        && producto.get("topRatedBuyingExperience").getAsBoolean();

                String url = producto.has("itemWebUrl") ? producto.get("itemWebUrl").getAsString() : "";
                String dateCreated = producto.has("itemCreationDate")
                        ? producto.get("itemCreationDate").getAsString().substring(0, 10)
                        : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                byte ratedValue = (byte) (topRated ? 1 : 0);

                Producto nuevo = new Producto(
                        idProducto,
                        producto.has("title") ? producto.get("title").getAsString() : "Sin título",
                        vendedor,
                        categoria,
                        conditionEntity,
                        ratedValue,
                        url,
                        dateCreated
                );

                // =====================
                // PRECIO
                // =====================
                if (producto.has("price")) {
                    JsonElement priceElement = producto.get("price");

                    double value = 0.0;
                    String currency = "USD";

                    if (priceElement.isJsonObject()) {
                        JsonObject priceObj = priceElement.getAsJsonObject();
                        if (priceObj.has("value")) value = priceObj.get("value").getAsDouble();
                        if (priceObj.has("currency")) currency = priceObj.get("currency").getAsString();
                    } else if (priceElement.isJsonPrimitive()) {
                        try {
                            value = priceElement.getAsDouble();
                        } catch (Exception ignored) {}
                    }

                    if (value > 0.0) {
                        // Asignamos el PriceHistory al producto
                        PriceHistory priceH = new PriceHistory(idProducto, value, currency, dateCreated);
                        nuevo.setPriceHistory(priceH);
                    }
                }


                // =====================
                // IMÁGENES
                // =====================
                // =====================
                // IMÁGENES (todas las URLs)
                // =====================
                List<String> urls = new ArrayList<>();

                if (producto.has("images") && producto.get("images").isJsonArray()) {
                    JsonArray imagesArray = producto.getAsJsonArray("images");
                    for (JsonElement imgElem : imagesArray) {
                        if (imgElem.isJsonPrimitive()) {
                            String urlImg = imgElem.getAsString();
                            if (!urlImg.equalsIgnoreCase("Sin imágenes disponibles") && !urlImg.isBlank()) {
                                urls.add(urlImg);
                            }
                        }
                    }
                }

                // Si no tiene imágenes, usa una por defecto
                if (urls.isEmpty()) {
                    urls.add(getClass().getResource("/interfaz/recursos/imagen-rota.png").toExternalForm());
                }

// ✅ No tocar `urlProduct`, ya viene desde itemWebUrl
// Guarda solo las imágenes asociadas en memoria
                nuevo.setImageUrls(urls);


                // =====================
                // RELACIONAR Y AGREGAR
                // =====================
                listaProductos.add(nuevo);
            }
        }
        return listaProductos;
    }

    public void mtd_informationAditional(String token, Producto producto) {
        try {
            JsonArray detallesArray = conect_API_eBay.aditional_info_pro(token, producto.getItemId());
            if (detallesArray == null || detallesArray.isEmpty()) return;

            JsonObject detalles = detallesArray.get(0).getAsJsonObject();

            // ============================
            // 1️⃣ Descripción corta
            // ============================
            if (detalles.has("shortDescription") && !detalles.get("shortDescription").isJsonNull()) {
                producto.setShortDescription(detalles.get("shortDescription").getAsString());
                System.out.println("📝 Descripción cargada: " + producto.getShortDescription());

            }

            // ============================
            // 2️⃣ Atributos (localizedAspects)
            // ============================
            List<AtributtesProduct> atributos = new ArrayList<>();

            if (detalles.has("localizedAspects") && detalles.get("localizedAspects").isJsonArray()) {
                JsonArray aspects = detalles.getAsJsonArray("localizedAspects");

                for (JsonElement elem : aspects) {
                    if (!elem.isJsonObject()) continue;

                    JsonObject attr = elem.getAsJsonObject();
                    String name = attr.has("name") ? attr.get("name").getAsString().trim() : "";
                    if (name.isEmpty()) continue;

                    String value = null;
                    if (attr.has("value")) {
                        JsonElement v = attr.get("value");
                        value = v.isJsonPrimitive() ? v.getAsString() : v.toString();
                        value = value.trim().isEmpty() ? null : value;
                    }

                    AtributtesProduct atributo = new AtributtesProduct(producto, name, value);
                    atributos.add(atributo);
                }
            }
            // 🔹 Guardar los atributos en el producto (para mostrarlos en ProductController)
            producto.setAtributos(atributos);



            // Puedes guardar temporalmente la lista en memoria (si tu clase Producto no la tiene como campo)
            // o crear un campo `List<AtributtesProduct>` si deseas mantenerlos enlazados.

            // ============================
            // 3️⃣ Opciones de envío
            // ============================
            List<ShippingProduct> envios = new ArrayList<>();

            if (detalles.has("shippingOptions") && detalles.get("shippingOptions").isJsonArray()) {
                JsonArray shippingArray = detalles.getAsJsonArray("shippingOptions");

                for (JsonElement elem : shippingArray) {
                    JsonObject ship = elem.getAsJsonObject();

                    String type = ship.has("type") ? ship.get("type").getAsString() : "N/A";
                    String carrier = ship.has("shippingCarrierCode") ? ship.get("shippingCarrierCode").getAsString() : "N/A";
                    double cost = 0.0;

                    if (ship.has("shippingCost")) {
                        JsonObject costObj = ship.getAsJsonObject("shippingCost");
                        cost = costObj.has("value") ? costObj.get("value").getAsDouble() : 0.0;
                    }

                    ShippingProduct envio = new ShippingProduct(producto, type, carrier, cost);
                    envios.add(envio);
                }
            }

            producto.setEnvios(envios);

            // ============================
            // 4️⃣ Política de devoluciones
            // ============================
            if (detalles.has("returnsAccepted")) {
                boolean returnsAccepted = detalles.get("returnsAccepted").getAsBoolean();
                producto.setReturns(returnsAccepted ? (byte) 1 : (byte) 0);
            }

            // ============================
            // 5️⃣ Cupones disponibles
            // ============================
            List<CouponPro> cupones = new ArrayList<>();

            if (detalles.has("availableCoupons") && detalles.get("availableCoupons").isJsonArray()) {
                JsonArray couponsArray = detalles.getAsJsonArray("availableCoupons");

                for (JsonElement elem : couponsArray) {
                    JsonObject cpn = elem.getAsJsonObject();

                    String code = cpn.has("redemptionCode") ? cpn.get("redemptionCode").getAsString() : "N/A";
                    String expiration = cpn.has("expirationDate") ? cpn.get("expirationDate").getAsString() : "N/A";

                    CouponPro cupon = new CouponPro(producto.getItemId(), code, expiration);
                    cupones.add(cupon);

                    // Solo enlazamos el primero directamente si aplica
                    producto.setIdCoupon(cupon);
                }
            }

            // ============================
            // 6️⃣ Disponibilidad
            // ============================
            if (detalles.has("status")) {
                String status = detalles.get("status").getAsString();
                producto.setAvailable(status.equalsIgnoreCase("IN_STOCK") ? (byte) 1 : (byte) 0);
            }

            // 🔹 Ahora el objeto `producto` ya tiene todos los datos adicionales cargados en memoria
            // No se guarda en BD ni se actualiza todavía.
            System.out.println("✅ Información adicional cargada para: " + producto.getItemId());

        } catch (Exception e) {
            System.err.println("⚠️ Error procesando información adicional: " + e.getMessage());
        }
    }

    public static Map<String, String> calcularAccountAge(String token, String sellerUsername) {
        Map<String, String> resultado = new HashMap<>();
        conect_API_eBay api = new conect_API_eBay();

        try {
            if (sellerUsername == null || sellerUsername.trim().isEmpty()) {
                System.err.println("⚠️ Nombre de vendedor vacío. No se puede calcular antigüedad.");
                resultado.put("fecha", "N/A");
                resultado.put("antiguedad", "Usuario desconocido");
                return resultado;
            }

            // 🧠 Revisar si ya está cacheado
            if (cacheAntiguedad.containsKey(sellerUsername)) {
                System.out.println("♻️ Antigüedad obtenida desde caché para: " + sellerUsername);
                return cacheAntiguedad.get(sellerUsername);
            }

            // 🔹 Llamada a la API: se usa "q" para evitar error 400
            JsonArray productos = api.buscarProductosPorVendedor(token, sellerUsername, 50);

            if (productos == null || productos.size() == 0) {
                System.out.println("📭 El vendedor " + sellerUsername + " no tiene productos activos.");
                resultado.put("fecha", "N/A");
                resultado.put("antiguedad", "Sin publicaciones recientes");
                cacheAntiguedad.put(sellerUsername, resultado);
                return resultado;
            }

            DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;

            // Buscar la fecha más antigua
            Optional<LocalDate> fechaMasAntigua = productos.asList().stream()
                    .map(e -> {
                        JsonObject obj = e.getAsJsonObject();
                        if (obj.has("itemCreationDate")) {
                            try {
                                return LocalDate.parse(obj.get("itemCreationDate").getAsString(), fmt);
                            } catch (Exception ex) {
                                return null;
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo);

            if (fechaMasAntigua.isEmpty()) {
                resultado.put("fecha", "Desconocida");
                resultado.put("antiguedad", "No disponible");
                cacheAntiguedad.put(sellerUsername, resultado);
                return resultado;
            }

            LocalDate inicio = fechaMasAntigua.get();
            LocalDate hoy = LocalDate.now();
            long años = ChronoUnit.YEARS.between(inicio, hoy);
            long meses = ChronoUnit.MONTHS.between(inicio, hoy) % 12;

            String fechaFormateada = inicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String textoAntiguedad = (años > 0 ? años + " años " : "") + meses + " meses";

            resultado.put("fecha", fechaFormateada);
            resultado.put("antiguedad", textoAntiguedad);
            cacheAntiguedad.put(sellerUsername, resultado);

            System.out.println("📊 Antigüedad calculada para " + sellerUsername +
                    ": " + textoAntiguedad + " (desde " + fechaFormateada + ")");

        } catch (Exception e) {
            System.err.println("❌ Error al calcular antigüedad del vendedor " + sellerUsername);
            e.printStackTrace();
            resultado.put("fecha", "Error");
            resultado.put("antiguedad", "Error de conexión o formato");
        }

        return resultado;
    }
}


