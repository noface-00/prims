package utils;

import api.conect_API_eBay;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Utilidad para realizar análisis de precios de productos similares
 * obtenidos desde la API de eBay.
 *
 * @author Kevin
 */
public class ProductAnalysisUtils {

    /**
     * Analiza el mercado de productos similares obtenidos por palabra clave
     * y devuelve métricas estadísticas (promedio, mínimo, máximo y desviación estándar).
     *
     * @param token   Token de acceso OAuth2
     * @param keyword Palabra clave del producto (por ejemplo, título o modelo)
     * @param limit   Cantidad de resultados a analizar (recomendado: 20–50)
     * @return JsonObject con las métricas:
     * {
     *   "average": 299.75,
     *   "min": 249.99,
     *   "max": 349.99,
     *   "std": 25.10,
     *   "count": 30
     * }
     * @throws Exception Si ocurre un error al conectar o procesar los datos.
     */
    public static JsonObject analizarMercado(String token, String keyword, int limit) throws Exception {
        conect_API_eBay api = new conect_API_eBay();
        JsonArray productos = api.browseProducts(token, keyword, limit,null,null,null,null);

        if (productos == null || productos.size() == 0) {
            System.out.println("⚠️ No se encontraron productos similares para: " + keyword);
            JsonObject vacio = new JsonObject();
            vacio.addProperty("average", 0.0);
            vacio.addProperty("min", 0.0);
            vacio.addProperty("max", 0.0);
            vacio.addProperty("std", 0.0);
            vacio.addProperty("count", 0);
            return vacio;
        }

        List<Double> precios = new ArrayList<>();

        for (JsonElement elem : productos) {
            JsonObject prod = elem.getAsJsonObject();
            if (prod.has("price")) {
                try {
                    double price = Double.parseDouble(prod.get("price").getAsString());
                    if (price > 0) precios.add(price);
                } catch (Exception ignored) {}
            }
        }

        if (precios.isEmpty()) {
            System.out.println("⚠️ No se pudieron leer precios válidos para: " + keyword);
            JsonObject vacio = new JsonObject();
            vacio.addProperty("average", 0.0);
            vacio.addProperty("min", 0.0);
            vacio.addProperty("max", 0.0);
            vacio.addProperty("std", 0.0);
            vacio.addProperty("count", 0);
            return vacio;
        }

        // 📊 Calcular estadísticas básicas
        DoubleSummaryStatistics stats = precios.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        double average = stats.getAverage();
        double min = stats.getMin();
        double max = stats.getMax();

        // 📈 Calcular desviación estándar
        double sumSquaredDiffs = precios.stream()
                .mapToDouble(p -> Math.pow(p - average, 2))
                .sum();
        double std = Math.sqrt(sumSquaredDiffs / precios.size());

        // 🧾 Crear el JSON de resultado
        JsonObject resultado = new JsonObject();
        resultado.addProperty("average", Math.round(average * 100.0) / 100.0);
        resultado.addProperty("min", Math.round(min * 100.0) / 100.0);
        resultado.addProperty("max", Math.round(max * 100.0) / 100.0);
        resultado.addProperty("std", Math.round(std * 100.0) / 100.0);
        resultado.addProperty("count", precios.size());

        System.out.println("📊 Análisis de mercado (" + keyword + "):");
        System.out.println("  Promedio: $" + resultado.get("average").getAsDouble());
        System.out.println("  Mínimo: $" + resultado.get("min").getAsDouble());
        System.out.println("  Máximo: $" + resultado.get("max").getAsDouble());
        System.out.println("  Desviación estándar: $" + resultado.get("std").getAsDouble());
        System.out.println("  Total analizados: " + precios.size());

        return resultado;
    }
}
