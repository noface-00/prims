package utils;

import dao.ProductAnalysisDAO;
import entities.ProductAnalysis;
import entities.Producto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    // ✅ Fuentes reutilizables (PDFBox 3.x)
    private static final PDFont FONT_TITLE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_TEXT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Genera reporte para un único producto
     */
    public static void generarReporteUnico(Producto p, ProductAnalysis analisis) {
        generarReporteUnificado(List.of(p), List.of(analisis));
    }

    /**
     * Genera reporte unificado para múltiples productos con sus análisis
     */
    public static void generarReporteUnificado(List<Producto> productos, List<ProductAnalysis> analisisList) {
        if (productos == null || productos.isEmpty()) {
            System.out.println("⚠️ No hay productos para generar reporte");
            return;
        }

        String rutaPDF = null;

        try (PDDocument doc = new PDDocument()) {

            // ═══════════════════════════════════════
            // 📄 PÁGINA 1 → PORTADA
            // ═══════════════════════════════════════
            PDPage portada = new PDPage(PDRectangle.LETTER);
            doc.addPage(portada);

            try (PDPageContentStream port = new PDPageContentStream(doc, portada)) {
                // Título principal
                port.beginText();
                port.setFont(FONT_TITLE, 24);
                port.newLineAtOffset(50, 720);
                port.showText("PRIMS - Reporte de Analisis");
                port.endText();

                // Fecha de generación
                port.beginText();
                port.setFont(FONT_TEXT, 14);
                port.newLineAtOffset(50, 685);
                port.showText("Generado: " + LocalDateTime.now().format(formatter));
                port.endText();

                // Cantidad de productos
                port.beginText();
                port.setFont(FONT_TEXT, 12);
                port.newLineAtOffset(50, 660);
                port.showText("Total de productos analizados: " + productos.size());
                port.endText();

                // Línea divisoria
                port.moveTo(50, 640);
                port.lineTo(550, 640);
                port.stroke();
            }

            // ═══════════════════════════════════════
            // 📦 SECCIONES INDIVIDUALES POR PRODUCTO
            // ═══════════════════════════════════════
            for (int i = 0; i < productos.size(); i++) {
                Producto p = productos.get(i);
                ProductAnalysis analisis = i < analisisList.size() ? analisisList.get(i) : null;
                agregarSeccionProducto(doc, p, analisis);
            }

            // ═══════════════════════════════════════
            // 📊 COMPARACIÓN (solo si hay más de 1)
            // ═══════════════════════════════════════
            if (productos.size() > 1) {
                agregarSeccionComparacion(doc, productos, analisisList);
            }

            // ═══════════════════════════════════════
            // 💾 GUARDAR PDF
            // ═══════════════════════════════════════
            rutaPDF = "Reporte_PRIMS_" + System.currentTimeMillis() + ".pdf";
            doc.save(rutaPDF);

            System.out.println("📄 Reporte PDF generado: " + rutaPDF);

        } catch (IOException e) {
            System.err.println("❌ Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
        }

        // ═══════════════════════════════════════
        // 💾 GUARDAR/ACTUALIZAR ANÁLISIS EN BD
        // ═══════════════════════════════════════
        if (analisisList != null && !analisisList.isEmpty()) {
            guardarAnalisisEnBD(analisisList, rutaPDF);
        }
    }

    /**
     * Agrega una página con información detallada del producto y su análisis
     */
    private static void agregarSeccionProducto(PDDocument doc, Producto p, ProductAnalysis analisis) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream s = new PDPageContentStream(doc, page)) {

            // Título del producto
            s.beginText();
            s.setFont(FONT_TITLE, 18);
            s.newLineAtOffset(50, 740);
            String titulo = p.getName() != null ? p.getName() : "Sin título";
            s.showText("Producto: " + truncate(titulo, 50));
            s.endText();

            // Línea divisoria
            s.moveTo(50, 730);
            s.lineTo(550, 730);
            s.stroke();

            float yPos = 705;

            // ═══════════════════════════════════════
            // INFORMACIÓN GENERAL
            // ═══════════════════════════════════════
            s.beginText();
            s.setFont(FONT_TITLE, 12);
            s.newLineAtOffset(50, yPos);
            s.showText("Informacion General");
            s.endText();
            yPos -= 20;

            s.beginText();
            s.setFont(FONT_TEXT, 11);
            s.newLineAtOffset(50, yPos);
            s.showText("Item ID: " + (p.getItemId() != null ? p.getItemId() : "N/A"));
            s.newLineAtOffset(0, -15);
            s.showText("Condicion: " + (p.getIdCondition() != null ? p.getIdCondition().getConditionPath() : "N/A"));
            s.newLineAtOffset(0, -15);
            s.showText("Categoria: " + (p.getIdCategory() != null ? p.getIdCategory().getCategoryPath() : "N/A"));
            s.endText();
            yPos -= 60;

            // ═══════════════════════════════════════
            // ANÁLISIS DE PRECIOS
            // ═══════════════════════════════════════
            if (analisis != null) {
                s.beginText();
                s.setFont(FONT_TITLE, 12);
                s.newLineAtOffset(50, yPos);
                s.showText("Analisis de Precios");
                s.endText();
                yPos -= 20;

                s.beginText();
                s.setFont(FONT_TEXT, 11);
                s.newLineAtOffset(50, yPos);

                if (analisis.getPriceActual() != null) {
                    s.showText("Precio actual: $" + String.format("%.2f", analisis.getPriceActual()));
                    s.newLineAtOffset(0, -15);
                }

                if (analisis.getMarketAverage() != null) {
                    s.showText("Promedio mercado: $" + String.format("%.2f", analisis.getMarketAverage()));
                    s.newLineAtOffset(0, -15);
                }

                if (analisis.getPriceDifference() != null) {
                    s.showText("Diferencia: $" + String.format("%.2f", analisis.getPriceDifference()));
                    s.newLineAtOffset(0, -15);
                }

                if (analisis.getMarketMin() != null && analisis.getMarketMax() != null) {
                    s.showText("Rango mercado: $" + String.format("%.2f", analisis.getMarketMin()) +
                            " - $" + String.format("%.2f", analisis.getMarketMax()));
                    s.newLineAtOffset(0, -15);
                }

                if (analisis.getStdDeviation() != null) {
                    s.showText("Desviacion estandar: $" + String.format("%.2f", analisis.getStdDeviation()));
                    s.newLineAtOffset(0, -15);
                }

                if (analisis.getTrustScore() != null) {
                    s.showText("Trust Score: " + String.format("%.1f", analisis.getTrustScore()) + " / 100");
                }

                s.endText();
                yPos -= 110;
            } else {
                s.beginText();
                s.setFont(FONT_TEXT, 11);
                s.newLineAtOffset(50, yPos);
                s.showText("No hay analisis disponible para este producto");
                s.endText();
                yPos -= 20;
            }

            // ═══════════════════════════════════════
            // INFORMACIÓN DEL VENDEDOR
            // ═══════════════════════════════════════
            s.beginText();
            s.setFont(FONT_TITLE, 12);
            s.newLineAtOffset(50, yPos);
            s.showText("Informacion del Vendedor");
            s.endText();
            yPos -= 20;

            s.beginText();
            s.setFont(FONT_TEXT, 11);
            s.newLineAtOffset(50, yPos);

            if (p.getIdSeller() != null) {
                s.showText("Vendedor: " + p.getIdSeller().getUsername());
                s.newLineAtOffset(0, -15);
                s.showText("Feedback: " + String.format("%.2f", p.getIdSeller().getFeedbackPorcentage()) + "%");
                s.newLineAtOffset(0, -15);
                s.showText("Puntuacion: " + p.getIdSeller().getFeedbackScore());
            } else {
                s.showText("Informacion del vendedor no disponible");
            }

            s.endText();
        }
    }

    /**
     * Agrega página de comparación entre productos
     */
    private static void agregarSeccionComparacion(PDDocument doc, List<Producto> productos, List<ProductAnalysis> analisisList) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream s = new PDPageContentStream(doc, page)) {

            // Título
            s.beginText();
            s.setFont(FONT_TITLE, 18);
            s.newLineAtOffset(50, 740);
            s.showText("Comparacion de Productos");
            s.endText();

            // Línea divisoria
            s.moveTo(50, 730);
            s.lineTo(550, 730);
            s.stroke();

            float yPos = 705;

            for (int i = 0; i < productos.size(); i++) {
                Producto p = productos.get(i);
                ProductAnalysis analisis = i < analisisList.size() ? analisisList.get(i) : null;

                s.beginText();
                s.setFont(FONT_TEXT, 11);
                s.newLineAtOffset(50, yPos);

                String nombre = truncate(p.getName(), 35);
                String precio = "N/A";
                String trustScore = "N/A";

                if (analisis != null) {
                    if (analisis.getPriceActual() != null) {
                        precio = String.format("$%.2f", analisis.getPriceActual());
                    }
                    if (analisis.getTrustScore() != null) {
                        trustScore = String.format("%.1f", analisis.getTrustScore());
                    }
                }

                s.showText("- " + nombre);
                s.newLineAtOffset(0, -15);
                s.showText("  Precio: " + precio + " | Trust Score: " + trustScore);
                s.endText();

                yPos -= 35;

                // Si llegamos al final de la página, crear una nueva
                if (yPos < 50) {
                    break;
                }
            }
        }
    }

    /**
     * Guarda o actualiza los análisis en la base de datos
     */
    private static void guardarAnalisisEnBD(List<ProductAnalysis> analisisList, String rutaPDF) {
        ProductAnalysisDAO dao = new ProductAnalysisDAO();

        try {
            for (ProductAnalysis analisis : analisisList) {
                // Verificar si ya existe un análisis para este producto
                if (analisis.getId() == null) {
                    // Nuevo análisis
                    dao.create(analisis);
                    System.out.println("✅ Análisis creado para producto: " + analisis.getItem().getItemId());
                } else {
                    // Actualizar análisis existente
                    dao.update(analisis);
                    System.out.println("♻️ Análisis actualizado para producto: " + analisis.getItem().getItemId());
                }
            }

            System.out.println("💾 Análisis guardado exitosamente en la BD");
            System.out.println("📄 Reporte PDF: " + rutaPDF);

        } catch (Exception e) {
            System.err.println("❌ Error al guardar análisis en BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Trunca texto para evitar desbordamiento en PDF
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}