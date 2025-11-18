package utils;

import entities.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar reportes PDF de análisis de productos
 */
public class ReportService {

    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 18;
    private static final float FONT_SIZE_SUBTITLE = 14;
    private static final float FONT_SIZE_NORMAL = 11;
    private static final float LEADING = 14;

    /**
     * Genera un reporte PDF único para un producto analizado
     */
    public static void generarReporteUnico(Producto producto, ProductAnalysis analysis) {
        if (producto == null || analysis == null) {
            System.err.println("Producto o análisis nulo, no se puede generar reporte");
            return;
        }

        try {
            // Crear documento PDF
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            // ═══════════════════════════════════════════════════════
            // 📋 TÍTULO DEL REPORTE
            // ═══════════════════════════════════════════════════════
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("REPORTE DE ANALISIS DE PRODUCTO");
            contentStream.endText();

            yPosition -= 30;

            // ═══════════════════════════════════════════════════════
            // 📦 INFORMACIÓN DEL PRODUCTO
            // ═══════════════════════════════════════════════════════
            yPosition = addSection(contentStream, yPosition, "INFORMACION DEL PRODUCTO",
                    new String[]{
                            "Nombre: " + truncateText(producto.getName(), 70),
                            "Item ID: " + producto.getItemId(),
                            "Categoria: " + (producto.getIdCategory() != null ?
                                    producto.getIdCategory().getCategoryPath() : "N/A"),
                            "Condicion: " + (producto.getIdCondition() != null ?
                                    producto.getIdCondition().getConditionPath() : "N/A"),
                            "URL: " + truncateText(producto.getUrlProduct(), 60)
                    }
            );

            yPosition -= 20;

            // ═══════════════════════════════════════════════════════
            // 💰 ANÁLISIS DE PRECIO
            // ═══════════════════════════════════════════════════════
            yPosition = addSection(contentStream, yPosition, "ANALISIS DE PRECIO",
                    new String[]{
                            "Precio Actual: USD " + String.format("%.2f",
                                    analysis.getPriceActual() != null ? analysis.getPriceActual() : 0.0),
                            "Promedio de Mercado: USD " + String.format("%.2f",
                                    analysis.getMarketAverage() != null ? analysis.getMarketAverage() : 0.0),
                            "Precio Minimo: USD " + String.format("%.2f",
                                    analysis.getMarketMin() != null ? analysis.getMarketMin() : 0.0),
                            "Precio Maximo: USD " + String.format("%.2f",
                                    analysis.getMarketMax() != null ? analysis.getMarketMax() : 0.0),
                            "Diferencia vs Mercado: USD " + String.format("%.2f",
                                    analysis.getPriceDifference() != null ? analysis.getPriceDifference() : 0.0),
                            "Desviacion Estandar: " + String.format("%.2f",
                                    analysis.getStdDeviation() != null ? analysis.getStdDeviation() : 0.0)
                    }
            );

            yPosition -= 20;

            // ═══════════════════════════════════════════════════════
            // 👤 INFORMACIÓN DEL VENDEDOR
            // ═══════════════════════════════════════════════════════
            if (analysis.getIdSeller() != null) {
                Seller seller = analysis.getIdSeller();
                yPosition = addSection(contentStream, yPosition, "INFORMACION DEL VENDEDOR",
                        new String[]{
                                "Username: " + seller.getUsername(),
                                "Feedback Positivo: " + String.format("%.2f%%", seller.getFeedbackPorcentage()),
                                "Feedback Score: " + seller.getFeedbackScore(),
                                "Marketplace: " + (seller.getMarketplace() != null ?
                                        seller.getMarketplace().getNameMarketplace() : "N/A")
                        }
                );

                yPosition -= 20;
            }

            // ═══════════════════════════════════════════════════════
            // 🎯 TRUST SCORE
            // ═══════════════════════════════════════════════════════
            if (analysis.getTrustScore() != null) {
                String trustLevel = getTrustLevel(analysis.getTrustScore());
                yPosition = addSection(contentStream, yPosition, "EVALUACION DE CONFIANZA",
                        new String[]{
                                "TrustScore: " + String.format("%.1f", analysis.getTrustScore()) + " / 100",
                                "Nivel: " + trustLevel
                        }
                );

                yPosition -= 20;
            }

            // ═══════════════════════════════════════════════════════
            // 📊 RECOMENDACIONES
            // ═══════════════════════════════════════════════════════
            String recomendacion = generarRecomendacion(producto, analysis);
            yPosition = addSection(contentStream, yPosition, "RECOMENDACION",
                    new String[]{recomendacion}
            );

            yPosition -= 30;

            // ═══════════════════════════════════════════════════════
            // 📅 FOOTER
            // ═══════════════════════════════════════════════════════
            String fecha = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            );

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
            contentStream.newLineAtOffset(MARGIN, 50);
            contentStream.showText("Reporte generado por PRIMS - " + fecha);
            contentStream.endText();

            contentStream.close();

            // ═══════════════════════════════════════════════════════
            // 💾 GUARDAR ARCHIVO
            // ═══════════════════════════════════════════════════════
            String fileName = "Reporte_" + sanitizeFileName(producto.getName()) + "_" +
                    System.currentTimeMillis() + ".pdf";

            // Guardar en carpeta de descargas del usuario
            String userHome = System.getProperty("user.home");
            String downloadsPath = userHome + File.separator + "Downloads" + File.separator + fileName;

            File outputFile = new File(downloadsPath);
            document.save(outputFile);
            document.close();

            System.out.println("Reporte PDF generado exitosamente: " + downloadsPath);

        } catch (IOException e) {
            System.err.println("❌ Error generando reporte PDF: " + e.getMessage());
            e.printStackTrace();
            NotificationManager.error("❌ Error al generar el reporte PDF");
        }
    }

    /**
     * Agrega una sección al PDF
     */
    private static float addSection(PDPageContentStream contentStream, float yPosition,
                                    String title, String[] lines) throws IOException {

        // Título de sección
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBTITLE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(title);
        contentStream.endText();

        yPosition -= 20;

        // Contenido
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);

        for (String line : lines) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(line);
            contentStream.endText();
            yPosition -= LEADING;
        }

        return yPosition;
    }

    /**
     * Determina el nivel de confianza basado en TrustScore
     */
    private static String getTrustLevel(double trustScore) {
        if (trustScore >= 80) return "MUY ALTO - Producto confiable";
        else if (trustScore >= 60) return "ALTO - Producto aceptable";
        else if (trustScore >= 40) return "MEDIO - Revisar con cuidado";
        else return "BAJO - Alto riesgo";
    }

    /**
     * Genera recomendación basada en el análisis
     */
    private static String generarRecomendacion(Producto producto, ProductAnalysis analysis) {
        if (analysis.getPriceActual() == null || analysis.getMarketAverage() == null) {
            return "Datos insuficientes para generar recomendacion.";
        }

        double precio = analysis.getPriceActual();
        double promedio = analysis.getMarketAverage();
        double diferencia = (precio - promedio) / promedio * 100;

        if (diferencia < -20) {
            return "EXCELENTE OPORTUNIDAD: Precio muy por debajo del mercado. Verificar condiciones.";
        } else if (diferencia < -10) {
            return "BUENA COMPRA: Precio competitivo respecto al mercado.";
        } else if (diferencia < 10) {
            return "PRECIO JUSTO: Dentro del rango normal del mercado.";
        } else if (diferencia < 20) {
            return "PRECIO ELEVADO: Considerar otras opciones en el mercado.";
        } else {
            return "NO RECOMENDADO: Precio muy por encima del promedio del mercado.";
        }
    }

    /**
     * Trunca texto si es muy largo
     */
    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    /**
     * Limpia el nombre del archivo
     */
    private static String sanitizeFileName(String name) {
        if (name == null) return "producto";
        return name.replaceAll("[^a-zA-Z0-9-_]", "_").substring(0, Math.min(name.length(), 30));
    }
}