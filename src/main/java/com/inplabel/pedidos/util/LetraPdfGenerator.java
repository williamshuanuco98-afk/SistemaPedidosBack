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

    private static final Font FONT_COMP_ADDR = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, Color.BLACK);
    private static final Font FONT_COMP_ADDR_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, Color.BLACK);
    private static final Font FONT_TD_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font FONT_TD_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, Color.BLACK);

    private static final Font FONT_TEXT_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 7.2f, Color.BLACK);
    private static final Font FONT_TEXT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font FONT_TEXT_GREEN_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7.8f, new Color(0, 140, 90));

    private static final Font FONT_MONTO_LETRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, Color.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(60, 60, 60));
    private static final Font FONT_BOTTOM_NOTE = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, new Color(40, 40, 40));

    private static final Color HEADER_BG_GREEN = new Color(226, 235, 216);
    private static final float BORDER_THIN = 0.6f;

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 Portrait format (595.28 x 841.89 pt) - 1 Sola Página garantizada con texto vectorial puro
            Document document = new Document(PageSize.A4, 25, 25, 20, 20);
            PdfWriter.getInstance(document, baos);
            document.open();

            renderLetraVector(document, letra);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de Letra de Cambio: " + e.getMessage(), e);
        }
    }

    public String savePdfToDisk(Map<String, Object> letra, String baseDir, boolean useSubfolders) {
        try {
            byte[] bytes = generatePdfBytes(letra);
            String nroLetra = (String) letra.getOrDefault("nro_letra", "261-2025");

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

    private void renderLetraVector(Document document, Map<String, Object> letra) throws Exception {
        String nroLetra = (String) letra.getOrDefault("nro_letra", "261 - 2025");
        String refGirador = (String) letra.getOrDefault("ref_girador", "FF02 - 630");
        String lugarGiro = (String) letra.getOrDefault("lugar_giro", "LIMA");

        String fechaGiroStr = String.valueOf(letra.getOrDefault("fecha_giro", LocalDate.now().toString()));
        String[] giroParts = parseDateToParts(fechaGiroStr);

        String fechaVencStr = String.valueOf(letra.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(30).toString()));
        String[] vencParts = parseDateToParts(fechaVencStr);

        Number montoNum = (Number) letra.getOrDefault("monto", 0.0);
        String montoFormatted = String.format("S/ %,.2f", montoNum.doubleValue()).replace(',', 'X').replace('.', ',').replace('X', '.');
        if (!montoFormatted.contains("S/")) montoFormatted = "S/ " + montoFormatted;

        String montoLetras = ((String) letra.getOrDefault("monto_letras", "CERO CON 00 / 100 SOLES")).toUpperCase();
        String cliente = (String) letra.getOrDefault("nombre_cliente", "CLIENTE S.A.C.");
        String ruc = (String) letra.getOrDefault("nro_documento", "-");
        String direccion = (String) letra.getOrDefault("direccion_cliente", "LIMA - LIMA");

        // 1. TOP HEADER: [inplabel-logo.png Left] [Company Info Right]
        PdfPTable topHeader = new PdfPTable(2);
        topHeader.setWidthPercentage(100);
        topHeader.setWidths(new float[]{45f, 55f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(0);

        try {
            InputStream is = getClass().getResourceAsStream("/inplabel-logo.png");
            if (is != null) {
                byte[] imgBytes = is.readAllBytes();
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(145, 42);
                logoCell.addElement(img);
            } else {
                File localLogo = new File("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosFront/img/inplabel-logo.png");
                if (localLogo.exists()) {
                    Image img = Image.getInstance(localLogo.getAbsolutePath());
                    img.scaleToFit(145, 42);
                    logoCell.addElement(img);
                } else {
                    logoCell.addElement(new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0, 140, 90))));
                }
            }
        } catch (Exception e) {
            logoCell.addElement(new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0, 140, 90))));
        }
        topHeader.addCell(logoCell);

        PdfPCell addrCell = new PdfPCell();
        addrCell.setBorder(Rectangle.NO_BORDER);
        addrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addrCell.setPadding(0);

        Paragraph pTopLine = new Paragraph("____________________________________________", FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(60, 60, 60)));
        pTopLine.setAlignment(Element.ALIGN_RIGHT);
        pTopLine.setSpacingAfter(2f);
        addrCell.addElement(pTopLine);

        Paragraph pAddr1 = new Paragraph("Av. María Parado de Bellido Lte. 5", FONT_COMP_ADDR_BOLD);
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

        // 2. MAIN TABLE (2 Columns: Col 1: Clauses 4.8%, Col 2: Body 95.2%)
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{4.8f, 95.2f});

        // Left Clauses using vector text wrapped inside the cell
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
                    BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

                    float fontSize = 4.2f;
                    cb.setFontAndSize(bf, fontSize);
                    cb.setColorFill(Color.BLACK);

                    float maxLineLength = rect.getHeight() - 10f; // fits strictly inside cell height

                    String fullText = "CLÁUSULAS ESPECIALES: (1) En caso de mora, esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor. " +
                            "(2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale, sin que sea necesaria la intervención del obligado principal ni de los solidarios. " +
                            "(3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias. " +
                            "(4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima.";

                    java.util.List<String> wrappedLines = wrapTextForPdf(fullText, bf, fontSize, maxLineLength);

                    float lineStep = (rect.getWidth() - 4f) / (float) Math.max(1, wrappedLines.size());
                    float startX = rect.getLeft() + 3.5f;

                    for (int i = 0; i < wrappedLines.size(); i++) {
                        String line = wrappedLines.get(i);
                        float x = startX + (i * lineStep);
                        float y = rect.getBottom() + 5f;

                        if (i == 0 && line.startsWith("CLÁUSULAS ESPECIALES:")) {
                            cb.setFontAndSize(bfBold, fontSize);
                            cb.showTextAligned(Element.ALIGN_LEFT, "CLÁUSULAS ESPECIALES: ", x, y, 90f);
                            float titleW = bfBold.getWidthPoint("CLÁUSULAS ESPECIALES: ", fontSize);
                            cb.setFontAndSize(bf, fontSize);
                            cb.showTextAligned(Element.ALIGN_LEFT, line.substring("CLÁUSULAS ESPECIALES:".length()).trim(), x, y + titleW, 90f);
                        } else {
                            cb.setFontAndSize(bf, fontSize);
                            cb.showTextAligned(Element.ALIGN_LEFT, line, x, y, 90f);
                        }
                    }
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
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
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

        Paragraph pBanner = new Paragraph();
        pBanner.add(new Chunk("Por esta ", FONT_TEXT_REGULAR));
        pBanner.add(new Chunk("LETRA DE CAMBIO", FONT_TEXT_BOLD));
        pBanner.add(new Chunk(", se servirá(n) pagar a la orden de ", FONT_TEXT_REGULAR));
        pBanner.add(new Chunk("INDUSTRIAS PLASTICOS BELSA S.A.C.", FONT_TEXT_GREEN_BOLD));
        pBanner.add(new Chunk(" la cantidad de:", FONT_TEXT_REGULAR));

        PdfPCell bannerCell = new PdfPCell(pBanner);
        bannerCell.setColspan(6);
        bannerCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
        bannerCell.setBorderWidth(BORDER_THIN);
        bannerCell.setPadding(2f);
        upperSection.addCell(bannerCell);

        PdfPCell montoLetrasCell = new PdfPCell(new Phrase(montoLetras, FONT_MONTO_LETRAS));
        montoLetrasCell.setColspan(6);
        montoLetrasCell.setBorder(Rectangle.BOX);
        montoLetrasCell.setBorderWidth(BORDER_THIN);
        montoLetrasCell.setPadding(3f);
        upperSection.addCell(montoLetrasCell);

        PdfPCell subCell = new PdfPCell(new Phrase("Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:", FONT_TEXT_REGULAR));
        subCell.setColspan(6);
        subCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        subCell.setBorderWidth(BORDER_THIN);
        subCell.setPadding(1.5f);
        upperSection.addCell(subCell);

        rightBody.addCell(new PdfPCell(upperSection));

        // Lower Section: [Girado/Avalista Left 56%] [Banco/Firma Right 44%]
        PdfPTable lowerSection = new PdfPTable(2);
        lowerSection.setWidthPercentage(100);
        lowerSection.setWidths(new float[]{56f, 44f});

        // --- LOWER LEFT ---
        PdfPTable leftGrid = new PdfPTable(3);
        leftGrid.setWidthPercentage(100);
        leftGrid.setWidths(new float[]{4.0f, 18.0f, 78.0f});

        PdfPCell d1 = new PdfPCell(new Phrase("")); d1.setBorder(Rectangle.NO_BORDER); leftGrid.addCell(d1);
        leftGrid.addCell(createLabelCell("GIRADO A:"));
        leftGrid.addCell(createValueBoldCell(cliente));

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
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
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

        // Signature table
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

        PdfPCell s5 = new PdfPCell(new Phrase("Nombre del representante(S)", FONT_SUBTITLE));
        s5.setBorder(Rectangle.NO_BORDER); s5.setHorizontalAlignment(Element.ALIGN_CENTER);
        signTable.addCell(s5);

        PdfPCell s6 = new PdfPCell(new Phrase("D.O.I", FONT_TEXT_BOLD));
        s6.setBorder(Rectangle.NO_BORDER); s6.setHorizontalAlignment(Element.ALIGN_LEFT);
        signTable.addCell(s6);

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

    public String generateHtml(Map<String, Object> letra) {
        String nroLetra = (String) letra.getOrDefault("nro_letra", "261 - 2025");
        String refGirador = (String) letra.getOrDefault("ref_girador", "FF02 - 630");
        String lugarGiro = (String) letra.getOrDefault("lugar_giro", "LIMA");

        String fechaGiroStr = String.valueOf(letra.getOrDefault("fecha_giro", LocalDate.now().toString()));
        String[] fgVals = parseDateToParts(fechaGiroStr);

        String fechaVencStr = String.valueOf(letra.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(30).toString()));
        String[] fvVals = parseDateToParts(fechaVencStr);

        Number montoNum = (Number) letra.getOrDefault("monto", 0.0);
        String montoFormatted = String.format("S/ %,.2f", montoNum.doubleValue()).replace(',', 'X').replace('.', ',').replace('X', '.');
        if (!montoFormatted.contains("S/")) montoFormatted = "S/ " + montoFormatted;

        String montoLetras = ((String) letra.getOrDefault("monto_letras", "CERO CON 00 / 100 SOLES")).toUpperCase();
        String cliente = (String) letra.getOrDefault("nombre_cliente", "CLIENTE S.A.C.");
        String ruc = (String) letra.getOrDefault("nro_documento", "-");
        String direccion = (String) letra.getOrDefault("direccion_cliente", "LIMA - LIMA");

        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<title>Letra de Cambio - " + nroLetra + "</title>\n" +
                "<style>\n" +
                "  :root{\n" +
                "    --green:#008c5a;\n" +
                "    --black:#141414;\n" +
                "    --lightgreen:#deebde;\n" +
                "    --border:#141414;\n" +
                "  }\n" +
                "  @page { size: A4 portrait; margin: 10mm 12mm; }\n" +
                "  *{ box-sizing:border-box; }\n" +
                "  body{\n" +
                "    margin:0; padding:0; background:#fff;\n" +
                "    font-family:\"DejaVu Sans\",\"Segoe UI\",Arial,sans-serif;\n" +
                "    color:var(--black); display:flex; justify-content:center;\n" +
                "  }\n" +
                "  .sheet{ background:#fff; width:100%; max-width:820px; padding:10px 15px; position:relative; }\n" +
                "  .header{ display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:14px; }\n" +
                "  .logo-block{ display:flex; align-items:center; }\n" +
                "  .logo-img{ height:50px; object-fit:contain; }\n" +
                "  .company-address{ text-align:right; font-size:11px; line-height:1.45; padding-top:4px; border-top:2px solid #444; min-width:340px; }\n" +
                "  .doc-body{ display:flex; }\n" +
                "  .side-label-wrap{ position:relative; width:24px; flex-shrink:0; border:2px solid var(--border); border-right:none; display:flex; align-items:center; justify-content:center; background:#fafafa; }\n" +
                "  .side-label-wrap .txt{ writing-mode: vertical-rl; transform: rotate(180deg); white-space:nowrap; font-size:9.5px; font-weight:700; text-align:center; }\n" +
                "  .clauses-outer{ position:relative; width:38px; flex-shrink:0; border:2px solid var(--border); border-right:none; padding:6px 2px; display:flex; align-items:center; justify-content:center; }\n" +
                "  .clauses-inner{ writing-mode: vertical-rl; transform: rotate(180deg); font-size:7.2px; line-height:1.15; color:#111; text-align:left; }\n" +
                "  .clauses-inner b{ font-size:7.5px; }\n" +
                "  .main-col{ flex:1; min-width:0; }\n" +
                "  table.topinfo{ border-collapse:collapse; width:100%; table-layout:fixed; }\n" +
                "  table.topinfo th, table.topinfo td{ border:2px solid var(--border); text-align:center; padding:4px 2px; }\n" +
                "  table.topinfo th{ font-size:10px; font-weight:700; background:#f0f5ee; }\n" +
                "  table.topinfo td.value{ font-size:13.5px; font-weight:700; padding:8px 2px; }\n" +
                "  table.topinfo td.value.amount{ font-size:15px; }\n" +
                "  table.topinfo .subhead{ font-size:9px; font-weight:700; background:#f0f5ee; padding:2px 1px; }\n" +
                "  .pay-phrase{ margin:10px 0 6px 0; font-size:12px; padding-left:4px; }\n" +
                "  .pay-phrase b{ font-weight:700; }\n" +
                "  .pay-phrase .beneficiary{ color:var(--green); font-weight:700; font-style:italic; }\n" +
                "  .amount-words{ border:2px solid var(--border); padding:8px 10px; font-size:12.5px; font-weight:700; margin-bottom:8px; background:#fff; }\n" +
                "  .payplace-note{ font-size:11px; margin-bottom:8px; padding-left:4px; }\n" +
                "  .bottom-table{ display:grid; grid-template-columns: 1fr 310px; border:2px solid var(--border); }\n" +
                "  .bt-left{ border-right:2px solid var(--border); padding:10px 12px; position:relative; }\n" +
                "  .bt-row{ display:flex; gap:10px; margin-bottom:8px; font-size:11px; }\n" +
                "  .bt-row .lbl{ font-weight:700; min-width:75px; flex-shrink:0; }\n" +
                "  .bt-divider{ border-top:1px solid #333; margin:8px 0 10px 0; }\n" +
                "  .dots{ border-bottom:1px dotted #333; flex:1; display:inline-block; min-width:40px; margin-left:4px; height:13px; }\n" +
                "  .bt-row.inline{ align-items:flex-end; }\n" +
                "  .bt-row.avalista .dots{ margin-left:8px; }\n" +
                "  .bt-right{ padding:8px 10px; display:flex; flex-direction:column; }\n" +
                "  .debit-note{ font-size:9.5px; margin-bottom:6px; }\n" +
                "  table.banktable{ border-collapse:collapse; width:100%; margin-bottom:6px; }\n" +
                "  table.banktable th, table.banktable td{ border:1.5px solid var(--border); font-size:9px; text-align:center; padding:3px 1px; font-weight:700; }\n" +
                "  table.banktable th{ background:#f0f5ee; }\n" +
                "  table.banktable td.blank{ height:20px; }\n" +
                "  .beneficiary-name{ color:var(--green); font-weight:700; font-style:italic; text-align:center; font-size:12px; margin-top:6px; }\n" +
                "  .beneficiary-ruc{ text-align:center; font-weight:700; font-size:11px; margin-bottom:14px; }\n" +
                "  .firma{ text-align:center; margin-top:auto; }\n" +
                "  .firma .line{ border-bottom:1px dotted #333; width:80%; margin:0 auto 3px auto; }\n" +
                "  .firma .caption{ font-weight:700; font-size:10.5px; }\n" +
                "  .firma .rep{ font-size:9.5px; margin-top:1px; }\n" +
                "  .doi{ font-weight:700; font-size:10px; margin-top:4px; text-align:left; }\n" +
                "  .footer-line{ border-top:1.5px solid var(--border); margin-top:12px; padding-top:4px; font-size:10.5px; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"sheet\">\n" +
                "  <div class=\"header\">\n" +
                "    <div class=\"logo-block\"><img src=\"img/inplabel-logo.png\" alt=\"Inplabel\" class=\"logo-img\" onerror=\"this.style.display='none'\"></div>\n" +
                "    <div class=\"company-address\">\n" +
                "      <b>Av. María Parado de Bellido Lte. 5</b><br>\n" +
                "      Lotización Chacra Cerro - Comas - Lima - Lima<br>\n" +
                "      Telf.: (01)557-1526 Claro: 975 564 460 / 983 518 504\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"doc-body\">\n" +
                "    <div class=\"clauses-outer\">\n" +
                "      <div class=\"clauses-inner\">\n" +
                "        <b>CLÁUSULAS ESPECIALES:</b>\n" +
                "        (1) En caso de mora, esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor.\n" +
                "        (2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale, sin que sea necesario la intervención del obligado principal ni de los solidarios.\n" +
                "        (3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias.\n" +
                "        (4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima.\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"main-col\">\n" +
                "      <div style=\"display:flex;\">\n" +
                "        <div class=\"side-label-wrap\"><span class=\"txt\">Aceptante(s)</span></div>\n" +
                "        <table class=\"topinfo\">\n" +
                "          <colgroup>\n" +
                "            <col style=\"width:17%\"><col style=\"width:16%\"><col style=\"width:13%\">\n" +
                "            <col style=\"width:7.5%\"><col style=\"width:7.5%\"><col style=\"width:7.5%\">\n" +
                "            <col style=\"width:7.5%\"><col style=\"width:7.5%\"><col style=\"width:7.5%\">\n" +
                "            <col style=\"width:16.5%\">\n" +
                "          </colgroup>\n" +
                "          <tr>\n" +
                "            <th rowspan=\"2\">NUMERO DE LETRA</th><th rowspan=\"2\">REF. DEL GIRADOR</th><th rowspan=\"2\">LUGAR DE GIRO</th>\n" +
                "            <th colspan=\"3\">FECHA DE GIRO</th><th colspan=\"3\">FECHA DE VENCIMIENTO</th><th rowspan=\"2\">MONEDA E IMPORTE</th>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <th class=\"subhead\">DIA</th><th class=\"subhead\">MES</th><th class=\"subhead\">AÑO</th>\n" +
                "            <th class=\"subhead\">DIA</th><th class=\"subhead\">MES</th><th class=\"subhead\">AÑO</th>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td class=\"value\">" + nroLetra + "</td><td class=\"value\">" + refGirador + "</td><td class=\"value\">" + lugarGiro + "</td>\n" +
                "            <td class=\"value\">" + fgVals[0] + "</td><td class=\"value\">" + fgVals[1] + "</td><td class=\"value\">" + fgVals[2] + "</td>\n" +
                "            <td class=\"value\">" + fvVals[0] + "</td><td class=\"value\">" + fvVals[1] + "</td><td class=\"value\">" + fvVals[2] + "</td>\n" +
                "            <td class=\"value amount\">" + montoFormatted + "</td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </div>\n" +
                "      <div class=\"pay-phrase\">\n" +
                "        Por esta <b>LETRA DE CAMBIO</b>, se servirá(n) pagar a la orden de <span class=\"beneficiary\">INDUSTRIAS PLASTICOS BELSA S.A.C.</span> la cantidad de:\n" +
                "      </div>\n" +
                "      <div class=\"amount-words\">" + montoLetras + "</div>\n" +
                "      <div class=\"payplace-note\">\n" +
                "        Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:\n" +
                "      </div>\n" +
                "      <div style=\"display:flex;\">\n" +
                "        <div class=\"side-label-wrap\"><span class=\"txt\">Por Aval</span></div>\n" +
                "        <div class=\"bottom-table\" style=\"flex:1;\">\n" +
                "          <div class=\"bt-left\">\n" +
                "            <div class=\"bt-row\"><span class=\"lbl\">GIRADO A:</span><span><b>" + cliente + "</b></span></div>\n" +
                "            <div class=\"bt-row\"><span class=\"lbl\">RUC:</span><span>" + ruc + "</span></div>\n" +
                "            <div class=\"bt-row\"><span class=\"lbl\">DIRECCION:</span><span>" + direccion + "</span></div>\n" +
                "            <div class=\"bt-divider\"></div>\n" +
                "            <div class=\"bt-row inline avalista\"><span class=\"lbl\">AVALISTA:</span><span class=\"dots\"></span></div>\n" +
                "            <div class=\"bt-row inline avalista\">\n" +
                "              <span class=\"lbl\" style=\"min-width:60px;\">D.I/R.U.C:</span><span class=\"dots\" style=\"max-width:110px;\"></span>\n" +
                "              <span class=\"lbl\" style=\"min-width:65px; margin-left:10px;\">TELEFONO:</span><span class=\"dots\"></span>\n" +
                "            </div>\n" +
                "            <div class=\"bt-row inline avalista\"><span class=\"lbl\">DIRECCION:</span><span class=\"dots\"></span></div>\n" +
                "          </div>\n" +
                "          <div class=\"bt-right\">\n" +
                "            <div class=\"debit-note\">Importe a debitar en cuenta del Aceptante del Banco: ..............................</div>\n" +
                "            <table class=\"banktable\">\n" +
                "              <tr><th>BANCO</th><th>OFICINA</th><th>NUMERO DE CUENTA</th><th>D.C.</th></tr>\n" +
                "              <tr><td class=\"blank\"></td><td class=\"blank\"></td><td class=\"blank\"></td><td class=\"blank\"></td></tr>\n" +
                "            </table>\n" +
                "            <div class=\"beneficiary-name\">INDUSTRIAS PLASTICOS BELSA S.A.C.</div>\n" +
                "            <div class=\"beneficiary-ruc\">RUC: 20544368827</div>\n" +
                "            <div class=\"firma\">\n" +
                "              <div class=\"line\"></div>\n" +
                "              <div class=\"caption\">FIRMA</div>\n" +
                "              <div class=\"rep\">Nombre del representante(S)</div>\n" +
                "            </div>\n" +
                "            <div class=\"doi\">D.O.I</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div class=\"footer-line\">No escribir ni firmar debajo de esta linea</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";
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

    private PdfPCell createValueBoldCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_TEXT_BOLD));
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

    private java.util.List<String> wrapTextForPdf(String text, BaseFont bf, float fontSize, float maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();

        for (String word : words) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            float w = bf.getWidthPoint(test, fontSize);
            if (w <= maxWidth) {
                cur.append(cur.length() == 0 ? "" : " ").append(word);
            } else {
                if (cur.length() > 0) lines.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
