package com.inplabel.pedidos.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

@Component
public class LetraPdfGenerator {

    private static final Font FONT_COMP_ADDR = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, Color.BLACK);
    private static final Font FONT_TD_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font FONT_TD_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    
    private static final Font FONT_TEXT_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, Color.BLACK);
    private static final Font FONT_TEXT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font FONT_TEXT_GREEN_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7.8f, new Color(16, 140, 80));
    
    private static final Font FONT_MONTO_LETRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, Color.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(60, 60, 60));
    private static final Font FONT_BOTTOM_NOTE = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, new Color(40, 40, 40));

    private static final Color HEADER_BG_GREEN = new Color(226, 235, 216);
    private static final float BORDER_THIN = 0.6f;

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 Portrait format (595.28 x 841.89 pt) - 1 Sola Página garantizada
            Document document = new Document(PageSize.A4, 25, 25, 20, 20);
            PdfWriter.getInstance(document, baos);
            document.open();

            renderLetraCard(document, letra);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de Letra de Cambio: " + e.getMessage(), e);
        }
    }

    public String savePdfToDisk(Map<String, Object> letra, String baseDir, boolean useSubfolders) {
        try {
            byte[] bytes = generatePdfBytes(letra);
            String nroLetra = (String) letra.getOrDefault("nro_letra", "261-2026");
            
            String targetDir = (baseDir != null && !baseDir.trim().isEmpty()) ? baseDir.trim() : "C:\\Inplabel\\Letras";
            
            if (useSubfolders) {
                LocalDate now = LocalDate.now();
                targetDir = targetDir + File.separator + now.getYear() + File.separator + String.format("%02d", now.getMonthValue());
            }

            File dir = new File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = nroLetra.replaceAll("[^a-zA-Z0-9-_]", "_") + ".pdf";
            File targetFile = new File(dir, filename);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(bytes);
            }

            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Advertencia al guardar PDF de Letra en disco: " + e.getMessage());
            return null;
        }
    }

    private void renderLetraCard(Document document, Map<String, Object> letra) throws Exception {
        String nroLetra = (String) letra.getOrDefault("nro_letra", "261-2026");
        String refGirador = (String) letra.getOrDefault("ref_girador", "FF02 - 630");
        String lugarGiro = (String) letra.getOrDefault("lugar_giro", "LIMA");
        
        String fechaGiroStr = String.valueOf(letra.getOrDefault("fecha_giro", LocalDate.now().toString()));
        String[] giroParts = parseDateToParts(fechaGiroStr);
        
        String fechaVencStr = String.valueOf(letra.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(30).toString()));
        String[] vencParts = parseDateToParts(fechaVencStr);

        Number montoNum = (Number) letra.getOrDefault("monto", 0.0);
        String montoFormatted = String.format("S/ %,.2f", montoNum.doubleValue()).replace(',', 'X').replace('.', ',').replace('X', '.');
        if (!montoFormatted.contains("S/")) montoFormatted = "S/ " + montoFormatted;

        String montoLetras = (String) letra.getOrDefault("monto_letras", "CERO CON 00/100 SOLES");
        String cliente = (String) letra.getOrDefault("nombre_cliente", "CLIENTE S.A.C.");
        String ruc = (String) letra.getOrDefault("nro_documento", "-");
        String direccion = (String) letra.getOrDefault("direccion_cliente", "LIMA - LIMA");

        // 1. TOP HEADER: [Logo Left] [Company Info Right]
        PdfPTable topHeader = new PdfPTable(2);
        topHeader.setWidthPercentage(100);
        topHeader.setWidths(new float[]{40f, 60f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        try {
            InputStream is = getClass().getResourceAsStream("/inplabel-logo.png");
            if (is != null) {
                byte[] imgBytes = is.readAllBytes();
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(140, 44);
                logoCell.addElement(img);
            } else {
                logoCell.addElement(new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(16, 185, 129))));
            }
        } catch (Exception e) {
            logoCell.addElement(new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(16, 185, 129))));
        }
        topHeader.addCell(logoCell);

        PdfPCell addrCell = new PdfPCell();
        addrCell.setBorder(Rectangle.NO_BORDER);
        addrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Paragraph pAddr1 = new Paragraph("Av. María Parado de Bellido Lte. 5", FONT_COMP_ADDR);
        pAddr1.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pAddr2 = new Paragraph("Lotización Chacra Cerro - Comas - Lima - Lima", FONT_COMP_ADDR);
        pAddr2.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pAddr3 = new Paragraph("Telf.: (01)557-1526 Claro: 975 564 460 / 983 518 504", FONT_COMP_ADDR);
        pAddr3.setAlignment(Element.ALIGN_RIGHT);

        addrCell.addElement(pAddr1);
        addrCell.addElement(pAddr2);
        addrCell.addElement(pAddr3);
        topHeader.addCell(addrCell);

        document.add(topHeader);

        // Spacer
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(2f);
        document.add(spacer);

        // 2. MAIN TABLE (2 Columns: Col 1: Clauses 4.2%, Col 2: Body 95.8%)
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{4.2f, 95.8f});

        // Left Clauses using PdfPCellEvent to prevent table pagination splits
        PdfPCell clausesCell = new PdfPCell();
        clausesCell.setBorder(Rectangle.BOX);
        clausesCell.setBorderWidth(BORDER_THIN);
        clausesCell.setCellEvent(new PdfPCellEvent() {
            @Override
            public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
                PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
                cb.saveState();
                cb.beginText();
                try {
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    cb.setFontAndSize(bf, 4.6f);
                    cb.setColorFill(Color.BLACK);
                    
                    String text = "CLÁUSULAS ESPECIALES: (1) En caso de mora , esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor. (2) El plazo de su vencimiento podrá ser prorrogrado por el tenedor, por el plazo que este señale, sin que sea necesaria la intervención del obligado principal ni de los solidarios. (3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias. (4) Las partes se someten a la competencia de los jueces del DIstrito Judicial de Lima.";
                    
                    float x = rect.getLeft() + (rect.getWidth() / 2f) + 1.5f;
                    float y = rect.getBottom() + 4f;
                    cb.showTextAligned(Element.ALIGN_LEFT, text, x, y, 90f);
                } catch (Exception ignored) {}
                cb.endText();
                cb.restoreState();
            }
        });
        mainTable.addCell(clausesCell);

        PdfPTable rightBody = new PdfPTable(1);
        rightBody.setWidthPercentage(100);

        // Upper Section: Tag + Grid
        PdfPTable upperSection = new PdfPTable(7);
        upperSection.setWidthPercentage(100);
        upperSection.setWidths(new float[]{2.2f, 16.3f, 16.3f, 13.5f, 17.5f, 18.0f, 16.2f});

        PdfPCell tagCell = new PdfPCell();
        tagCell.setBorder(Rectangle.BOX);
        tagCell.setBorderWidth(BORDER_THIN);
        tagCell.setRowspan(5);
        tagCell.setCellEvent(new PdfPCellEvent() {
            @Override
            public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
                PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
                cb.saveState();
                cb.beginText();
                try {
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    cb.setFontAndSize(bf, 5.8f);
                    cb.setColorFill(Color.BLACK);
                    float x = rect.getLeft() + (rect.getWidth() / 2f) + 1.8f;
                    float y = rect.getBottom() + (rect.getHeight() / 2f) - 15f;
                    cb.showTextAligned(Element.ALIGN_LEFT, "Aceptante(s)", x, y, 90f);
                } catch (Exception ignored) {}
                cb.endText();
                cb.restoreState();
            }
        });
        upperSection.addCell(tagCell);

        upperSection.addCell(createThCell("NUMERO DE LETRA"));
        upperSection.addCell(createThCell("REF. DEL GIRADOR"));
        upperSection.addCell(createThCell("LUGAR DE GIRO"));
        upperSection.addCell(createDateThCell("FECHA DE GIRO"));
        upperSection.addCell(createDateThCell("FECHA DE VENCIMIENTO"));
        upperSection.addCell(createThCell("MONEDA E IMPORTE"));

        upperSection.addCell(createTdCell(nroLetra, Element.ALIGN_CENTER, FONT_TD_VAL));
        upperSection.addCell(createTdCell(refGirador, Element.ALIGN_CENTER, FONT_TD_VAL));
        upperSection.addCell(createTdCell(lugarGiro, Element.ALIGN_CENTER, FONT_TD_VAL));
        upperSection.addCell(createDateValuesCell(giroParts[0], giroParts[1], giroParts[2]));
        upperSection.addCell(createDateValuesCell(vencParts[0], vencParts[1], vencParts[2]));
        upperSection.addCell(createTdCell(montoFormatted, Element.ALIGN_CENTER, FONT_TD_VAL));

        PdfPCell bannerCell = new PdfPCell(new Phrase("Por esta LETRA DE CAMBIO, se servirá(n) pagar a la orden de INDUSTRIAS PLASTICOS BELSA S.A.C. la cantidad de:", FONT_TEXT_REGULAR));
        bannerCell.setColspan(6);
        bannerCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
        bannerCell.setBorderWidth(BORDER_THIN);
        bannerCell.setPadding(2f);
        upperSection.addCell(bannerCell);

        PdfPCell montoLetrasCell = new PdfPCell(new Phrase(montoLetras.toUpperCase(), FONT_MONTO_LETRAS));
        montoLetrasCell.setColspan(6);
        montoLetrasCell.setBorder(Rectangle.BOX);
        montoLetrasCell.setBorderWidth(BORDER_THIN);
        montoLetrasCell.setPadding(3f);
        upperSection.addCell(montoLetrasCell);

        PdfPCell subCell = new PdfPCell(new Phrase("Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiete lugar de pago:", FONT_TEXT_REGULAR));
        subCell.setColspan(6);
        subCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        subCell.setBorderWidth(BORDER_THIN);
        subCell.setPadding(1.5f);
        upperSection.addCell(subCell);

        rightBody.addCell(new PdfPCell(upperSection));

        // Lower Section: [Girado/Avalista Left 55%] [Banco/Firma Right 45%]
        PdfPTable lowerSection = new PdfPTable(2);
        lowerSection.setWidthPercentage(100);
        lowerSection.setWidths(new float[]{55f, 45f});

        // --- LOWER LEFT ---
        PdfPTable leftGrid = new PdfPTable(3);
        leftGrid.setWidthPercentage(100);
        leftGrid.setWidths(new float[]{4.0f, 18.0f, 78.0f});

        PdfPCell d1 = new PdfPCell(new Phrase("")); d1.setBorder(Rectangle.NO_BORDER); leftGrid.addCell(d1);
        leftGrid.addCell(createLabelCell("GIRADO A:"));
        leftGrid.addCell(createValueCell(cliente));

        PdfPCell d2 = new PdfPCell(new Phrase("")); d2.setBorder(Rectangle.NO_BORDER); leftGrid.addCell(d2);
        leftGrid.addCell(createLabelCell("RUC:"));
        leftGrid.addCell(createValueCell(ruc));

        PdfPCell d3 = new PdfPCell(new Phrase("")); d3.setBorder(Rectangle.NO_BORDER); leftGrid.addCell(d3);
        leftGrid.addCell(createLabelCell("DIRECCION:"));
        leftGrid.addCell(createValueCell(direccion));

        PdfPCell porAvalCell = new PdfPCell();
        porAvalCell.setBorder(Rectangle.TOP | Rectangle.RIGHT);
        porAvalCell.setBorderWidth(BORDER_THIN);
        porAvalCell.setRowspan(3);
        porAvalCell.setCellEvent(new PdfPCellEvent() {
            @Override
            public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
                PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
                cb.saveState();
                cb.beginText();
                try {
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    cb.setFontAndSize(bf, 5.8f);
                    cb.setColorFill(Color.BLACK);
                    float x = rect.getLeft() + (rect.getWidth() / 2f) + 1.8f;
                    float y = rect.getBottom() + (rect.getHeight() / 2f) - 10f;
                    cb.showTextAligned(Element.ALIGN_LEFT, "Por Aval", x, y, 90f);
                } catch (Exception ignored) {}
                cb.endText();
                cb.restoreState();
            }
        });
        leftGrid.addCell(porAvalCell);

        PdfPCell cAvLabel = createLabelCell("AVALISTA:"); cAvLabel.setBorder(Rectangle.TOP); cAvLabel.setBorderWidth(BORDER_THIN); leftGrid.addCell(cAvLabel);
        PdfPCell cAvVal = createValueCell("....................................................................................................."); cAvVal.setBorder(Rectangle.TOP); cAvVal.setBorderWidth(BORDER_THIN); leftGrid.addCell(cAvVal);

        leftGrid.addCell(createLabelCell("D.I/R.U.C:"));
        leftGrid.addCell(createValueCell(".................................... TELEFONO: ...................."));

        leftGrid.addCell(createLabelCell("DIRECCION:"));
        leftGrid.addCell(createValueCell("....................................................................................................."));

        PdfPCell lowerLeftCell = new PdfPCell(leftGrid);
        lowerLeftCell.setBorder(Rectangle.BOX);
        lowerLeftCell.setBorderWidth(BORDER_THIN);
        lowerSection.addCell(lowerLeftCell);

        // --- LOWER RIGHT ---
        PdfPTable rightGrid = new PdfPTable(1);
        rightGrid.setWidthPercentage(100);

        PdfPCell cDebit = new PdfPCell(new Phrase("Importe a debitar en cuenta del Aceptante del Banco:....................................", FONT_TEXT_REGULAR));
        cDebit.setBorder(Rectangle.NO_BORDER);
        rightGrid.addCell(cDebit);

        PdfPTable bankGrid = new PdfPTable(4);
        bankGrid.setWidthPercentage(100);
        bankGrid.setWidths(new float[]{25f, 25f, 40f, 10f});
        bankGrid.addCell(createThCell("BANCO"));
        bankGrid.addCell(createThCell("OFICINA"));
        bankGrid.addCell(createThCell("NUMERO DE CUENTA"));
        bankGrid.addCell(createThCell("D.C."));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL));
        rightGrid.addCell(new PdfPCell(bankGrid));

        // Clean signature table
        PdfPTable signTable = new PdfPTable(1);
        signTable.setWidthPercentage(100);
        
        PdfPCell s1 = new PdfPCell(new Phrase("INDUSTRIAS PLASTICOS BELSA S.A.C.", FONT_TEXT_GREEN_BOLD));
        s1.setBorder(Rectangle.NO_BORDER); s1.setHorizontalAlignment(Element.ALIGN_CENTER);
        signTable.addCell(s1);
        
        PdfPCell s2 = new PdfPCell(new Phrase("RUC: 20544368827", FONT_TEXT_BOLD));
        s2.setBorder(Rectangle.NO_BORDER); s2.setHorizontalAlignment(Element.ALIGN_CENTER);
        signTable.addCell(s2);
        
        PdfPCell s3 = new PdfPCell(new Phrase("....................................................................................................", FontFactory.getFont(FontFactory.HELVETICA, 5.5f)));
        s3.setBorder(Rectangle.NO_BORDER); s3.setHorizontalAlignment(Element.ALIGN_CENTER);
        s3.setPaddingTop(8f);
        signTable.addCell(s3);
        
        PdfPCell s4 = new PdfPCell(new Phrase("FIRMA", FONT_TEXT_BOLD));
        s4.setBorder(Rectangle.NO_BORDER); s4.setHorizontalAlignment(Element.ALIGN_CENTER);
        signTable.addCell(s4);
        
        PdfPCell s5 = new PdfPCell(new Phrase("Nombre del representante(S)\nD.O.I", FONT_SUBTITLE));
        s5.setBorder(Rectangle.NO_BORDER);
        signTable.addCell(s5);

        rightGrid.addCell(new PdfPCell(signTable));

        PdfPCell lowerRightCell = new PdfPCell(rightGrid);
        lowerRightCell.setBorder(Rectangle.BOX);
        lowerRightCell.setBorderWidth(BORDER_THIN);
        lowerRightCell.setPadding(2f);
        lowerSection.addCell(lowerRightCell);

        rightBody.addCell(new PdfPCell(lowerSection));

        mainTable.addCell(new PdfPCell(rightBody));
        document.add(mainTable);

        // Footnote
        Paragraph pBottom = new Paragraph("No escribir ni firmar debajo de esta linea", FONT_BOTTOM_NOTE);
        pBottom.setSpacingBefore(2f);
        document.add(pBottom);
    }

    private PdfPCell createLabelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_TEXT_BOLD));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);
        return c;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_TEXT_REGULAR));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);
        return c;
    }

    private PdfPCell createThCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_THIN);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2f);
        cell.setBackgroundColor(HEADER_BG_GREEN);
        return cell;
    }

    private PdfPCell createTdCell(String text, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_THIN);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2f);
        return cell;
    }

    private PdfPCell createDateThCell(String title) {
        PdfPTable nested = new PdfPTable(3);
        nested.setWidthPercentage(100);
        
        PdfPCell top = new PdfPCell(new Phrase(title, FONT_TH));
        top.setColspan(3);
        top.setHorizontalAlignment(Element.ALIGN_CENTER);
        top.setBorder(Rectangle.NO_BORDER);
        top.setPaddingBottom(1.0f);
        top.setBackgroundColor(HEADER_BG_GREEN);
        nested.addCell(top);

        PdfPCell d1 = new PdfPCell(new Phrase("DIA", FONT_TH));
        d1.setHorizontalAlignment(Element.ALIGN_CENTER);
        d1.setBorder(Rectangle.TOP);
        d1.setBorderWidth(BORDER_THIN);
        d1.setBackgroundColor(HEADER_BG_GREEN);
        nested.addCell(d1);

        PdfPCell d2 = new PdfPCell(new Phrase("MES", FONT_TH));
        d2.setHorizontalAlignment(Element.ALIGN_CENTER);
        d2.setBorder(Rectangle.TOP);
        d2.setBorderWidth(BORDER_THIN);
        d2.setBackgroundColor(HEADER_BG_GREEN);
        nested.addCell(d2);

        PdfPCell d3 = new PdfPCell(new Phrase("AÑO", FONT_TH));
        d3.setHorizontalAlignment(Element.ALIGN_CENTER);
        d3.setBorder(Rectangle.TOP);
        d3.setBorderWidth(BORDER_THIN);
        d3.setBackgroundColor(HEADER_BG_GREEN);
        nested.addCell(d3);

        PdfPCell container = new PdfPCell(nested);
        container.setBorder(Rectangle.BOX);
        container.setBorderWidth(BORDER_THIN);
        container.setPadding(0.5f);
        container.setBackgroundColor(HEADER_BG_GREEN);
        return container;
    }

    private PdfPCell createDateValuesCell(String dd, String mm, String yy) {
        PdfPTable nested = new PdfPTable(3);
        nested.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell(new Phrase(dd, FONT_TD_VAL));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c1.setBorder(Rectangle.NO_BORDER);
        nested.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(mm, FONT_TD_VAL));
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c2.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
        c2.setBorderWidth(BORDER_THIN);
        nested.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(yy, FONT_TD_VAL));
        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        c3.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c3.setBorder(Rectangle.NO_BORDER);
        nested.addCell(c3);

        PdfPCell container = new PdfPCell(nested);
        container.setBorder(Rectangle.BOX);
        container.setBorderWidth(BORDER_THIN);
        container.setPadding(1f);
        return container;
    }

    private String[] parseDateToParts(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            LocalDate now = LocalDate.now();
            return new String[]{
                String.format("%02d", now.getDayOfMonth()),
                String.format("%02d", now.getMonthValue()),
                String.valueOf(now.getYear()).substring(2)
            };
        }
        try {
            if (dateStr.contains("-") && dateStr.length() >= 10) {
                String[] p = dateStr.substring(0, 10).split("-");
                return new String[]{p[2], p[1], p[0].substring(2)};
            }
            if (dateStr.contains("/")) {
                String[] p = dateStr.split("/");
                return new String[]{
                    p[0].length() == 1 ? "0" + p[0] : p[0],
                    p[1].length() == 1 ? "0" + p[1] : p[1],
                    p[2].length() == 4 ? p[2].substring(2) : p[2]
                };
            }
        } catch (Exception ignored) {}
        LocalDate now = LocalDate.now();
        return new String[]{
            String.format("%02d", now.getDayOfMonth()),
            String.format("%02d", now.getMonthValue()),
            String.valueOf(now.getYear()).substring(2)
        };
    }
}
