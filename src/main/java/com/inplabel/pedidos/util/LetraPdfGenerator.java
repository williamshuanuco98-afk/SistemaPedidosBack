package com.inplabel.pedidos.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Map;

@Component
public class LetraPdfGenerator {

    private static final Color COLOR_BELSA_GREEN = new Color(0, 176, 80);
    private static final Color COLOR_TEXT_DARK = new Color(20, 20, 20);

    // Tipografías ajustadas y proporcionadas (un poco más reducidas y legibles)
    private static final Font FONT_HEADER_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_TEXT_DARK);
    private static final Font FONT_HEADER_SUB_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_TEXT_DARK);
    private static final Font FONT_VAL_BIG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, COLOR_TEXT_DARK);
    private static final Font FONT_VAL_AMOUNT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13.0f, COLOR_TEXT_DARK);
    private static final Font FONT_VAL_DATE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.8f, COLOR_TEXT_DARK);

    private static final Font FONT_PHRASE_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 7.8f, COLOR_TEXT_DARK);
    private static final Font FONT_PHRASE_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, COLOR_TEXT_DARK);
    private static final Font FONT_PHRASE_GREEN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.2f, COLOR_BELSA_GREEN);

    private static final Font FONT_MONTO_LETRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.6f, COLOR_TEXT_DARK);
    private static final Font FONT_LABEL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_TEXT_DARK);
    private static final Font FONT_CLIENT_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, COLOR_TEXT_DARK);
    private static final Font FONT_CLIENT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 7.4f, COLOR_TEXT_DARK);
    private static final Font FONT_DOTS = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(110, 110, 110));

    private static final Font FONT_BELSA_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.0f, COLOR_BELSA_GREEN);
    private static final Font FONT_BELSA_RUC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, COLOR_TEXT_DARK);
    private static final Font FONT_SIGN_TEXT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_DARK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(80, 80, 80));
    private static final Font FONT_FOOTNOTE = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, new Color(50, 50, 50));

    private static final float BORDER_WIDTH = 0.75f;

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 10, 10, 12, 12);
            PdfWriter.getInstance(document, baos);
            document.open();

            renderLetraExcelReplica(document, letra);

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

    private void renderLetraExcelReplica(Document document, Map<String, Object> letra) throws Exception {
        String nroLetra = String.valueOf(letra.getOrDefault("nro_letra", "261-2025"));
        String refGirador = String.valueOf(letra.getOrDefault("ref_girador", "FF02 - 630"));
        String lugarGiro = String.valueOf(letra.getOrDefault("lugar_giro", "LIMA")).toUpperCase();

        String fechaGiroStr = String.valueOf(letra.getOrDefault("fecha_giro", LocalDate.now().toString()));
        String[] giroParts = parseDateToParts(fechaGiroStr);

        String fechaVencStr = String.valueOf(letra.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(30).toString()));
        String[] vencParts = parseDateToParts(fechaVencStr);

        Number montoNum = (Number) letra.getOrDefault("monto", 0.0);
        String moneda = String.valueOf(letra.getOrDefault("moneda", "SOLES")).toUpperCase();
        String symbol = moneda.contains("DOLAR") || moneda.contains("USD") ? "$" : "S/";
        String montoFormatted = String.format("%s %,.2f", symbol, montoNum.doubleValue()).replace(',', 'X').replace('.', ',').replace('X', '.');

        String montoLetras = String.valueOf(letra.getOrDefault("monto_letras", "CERO CON 00 / 100 SOLES")).toUpperCase();
        String cliente = String.valueOf(letra.getOrDefault("nombre_cliente", "CLIENTE S.A.C.")).toUpperCase();
        String ruc = String.valueOf(letra.getOrDefault("nro_documento", "-"));
        String direccion = String.valueOf(letra.getOrDefault("direccion_cliente", "LIMA - LIMA")).toUpperCase();

        // TABLA CONTENEDORA GENERAL (3 COLUMNAS):
        // Col 1: Cláusulas Especiales completas tamaño 7 (9.0%)
        // Col 2: Separador con líneas discontinuas Aceptante(s) / Por Aval (2.2%)
        // Col 3: Cuerpo de la Letra de Cambio (88.8%)
        PdfPTable mainContainer = new PdfPTable(3);
        mainContainer.setWidthPercentage(100);
        mainContainer.setWidths(new float[]{9.0f, 2.2f, 88.8f});

        // 1. COLUMNA DE CLÁUSULAS ESPECIALES EXACTAS (CALIBRI/HELVETICA 7)
        PdfPCell clausesCell = new PdfPCell();
        clausesCell.setBorder(Rectangle.NO_BORDER);
        clausesCell.setPadding(0);
        clausesCell.setCellEvent(new PdfPCellEvent() {
            @Override
            public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
                PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
                cb.saveState();
                cb.beginText();
                try {
                    BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    BaseFont bfNorm = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    cb.setColorFill(COLOR_TEXT_DARK);

                    String[] lines = {
                            "CLÁUSULAS ESPECIALES:",
                            "(1) En caso de mora , esta letra de cambio generará las tasas de interés compensatorio y",
                            "moratorio más altas que la ley permita a su último Tenedor.",
                            "(2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale,",
                            "sin que sea necesario la intervención del obligado principal ni de los solidarios.",
                            "(3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta",
                            "diligencia para el ejercicio de las acciones cambiarias.",
                            "(4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima."
                    };

                    float baseY = rect.getBottom() + 1.5f;
                    float startX = rect.getLeft() + 1.5f;
                    float lineSpacing = 5.8f;

                    for (int i = 0; i < lines.length; i++) {
                        float x = startX + (i * lineSpacing);
                        if (i == 0) {
                            cb.setFontAndSize(bfBold, 7.0f);
                        } else {
                            cb.setFontAndSize(bfNorm, 6.8f);
                        }
                        cb.showTextAligned(Element.ALIGN_LEFT, lines[i], x, baseY, 90f);
                    }
                } catch (Exception ignored) {}
                cb.endText();
                cb.restoreState();
            }
        });
        mainContainer.addCell(clausesCell);

        // 2. COLUMNA DE SEPARADORES CON LÍNEAS DISCONTINUAS "Aceptante(s)" y "Por Aval"
        PdfPCell tagsColCell = new PdfPCell();
        tagsColCell.setBorder(Rectangle.NO_BORDER);
        tagsColCell.setPadding(0);
        tagsColCell.setCellEvent(new PdfPCellEvent() {
            @Override
            public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
                PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
                cb.saveState();
                try {
                    BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                    float midX = rect.getLeft() + (rect.getWidth() / 2f);
                    float totalH = rect.getHeight();
                    float topY = rect.getTop();
                    float botY = rect.getBottom();

                    // Tag Aceptante(s) (Arriba)
                    float yAceptCenter = botY + (totalH * 0.77f);
                    cb.beginText();
                    cb.setFontAndSize(bfBold, 6.8f);
                    cb.setColorFill(COLOR_TEXT_DARK);
                    cb.showTextAligned(Element.ALIGN_CENTER, "Aceptante(s)", midX + 1f, yAceptCenter, 90f);
                    cb.endText();

                    float tagTextHalfH = 20f;
                    cb.setLineDash(2.5f, 2.0f);
                    cb.setLineWidth(0.8f);
                    cb.setColorStroke(COLOR_TEXT_DARK);

                    // Línea discontinua que nace en el tope del primer cajón
                    cb.moveTo(midX, topY);
                    cb.lineTo(midX, yAceptCenter + tagTextHalfH);
                    cb.stroke();

                    // Línea discontinua debajo de Aceptante(s)
                    cb.moveTo(midX, yAceptCenter - tagTextHalfH);
                    cb.lineTo(midX, botY + (totalH * 0.54f));
                    cb.stroke();

                    // Tag Por Aval (Abajo)
                    float yAvalCenter = botY + (totalH * 0.23f);
                    cb.beginText();
                    cb.setFontAndSize(bfBold, 6.8f);
                    cb.setColorFill(COLOR_TEXT_DARK);
                    cb.showTextAligned(Element.ALIGN_CENTER, "Por Aval", midX + 1f, yAvalCenter, 90f);
                    cb.endText();

                    float tagAvalHalfH = 16f;
                    cb.moveTo(midX, botY + (totalH * 0.44f));
                    cb.lineTo(midX, yAvalCenter + tagAvalHalfH);
                    cb.stroke();

                    // Línea discontinua que termina exactamente en la base del último cajón
                    cb.moveTo(midX, yAvalCenter - tagAvalHalfH);
                    cb.lineTo(midX, botY);
                    cb.stroke();

                } catch (Exception ignored) {}
                cb.restoreState();
            }
        });
        mainContainer.addCell(tagsColCell);

        // 3. CUERPO PRINCIPAL DE LA LETRA (Columna 3)
        PdfPTable bodyTable = new PdfPTable(1);
        bodyTable.setWidthPercentage(100);

        // --- A. CUADRÍCULA SUPERIOR DE EMISIÓN (10 COLUMNAS: SIN CELDAS VACÍAS INTERMEDIAS) ---
        PdfPTable gridTop = new PdfPTable(10);
        gridTop.setWidthPercentage(100);
        gridTop.setWidths(new float[]{18.0f, 18.0f, 14.0f, 5.7f, 5.7f, 5.7f, 5.7f, 5.7f, 5.7f, 16.1f});

        // Fila 1: Encabezados (con rowspan=2 para las 4 columnas directas)
        gridTop.addCell(createThCell("NUMERO DE LETRA", 1, 2));
        gridTop.addCell(createThCell("REF. DEL GIRADOR", 1, 2));
        gridTop.addCell(createThCell("LUGAR DE GIRO", 1, 2));
        gridTop.addCell(createThCell("FECHA DE GIRO", 3, 1));
        gridTop.addCell(createThCell("FECHA DE VENCIMIENTO", 3, 1));
        gridTop.addCell(createThCell("MONEDA E IMPORTE", 1, 2));

        // Fila 2: Sub-encabezados DIA | MES | AÑO únicamente para las fechas
        gridTop.addCell(createSubThCell("DIA"));
        gridTop.addCell(createSubThCell("MES"));
        gridTop.addCell(createSubThCell("AÑO"));
        gridTop.addCell(createSubThCell("DIA"));
        gridTop.addCell(createSubThCell("MES"));
        gridTop.addCell(createSubThCell("AÑO"));

        // Fila 3: Valores
        gridTop.addCell(createTdCell(nroLetra, FONT_VAL_BIG, Element.ALIGN_CENTER, 23f));
        gridTop.addCell(createTdCell(refGirador, FONT_VAL_BIG, Element.ALIGN_CENTER, 23f));
        gridTop.addCell(createTdCell(lugarGiro, FONT_VAL_BIG, Element.ALIGN_CENTER, 23f));

        gridTop.addCell(createTdCell(giroParts[0], FONT_VAL_DATE, Element.ALIGN_CENTER, 23f));
        gridTop.addCell(createTdCell(giroParts[1], FONT_VAL_DATE, Element.ALIGN_CENTER, 23f));
        gridTop.addCell(createTdCell(giroParts[2], FONT_VAL_DATE, Element.ALIGN_CENTER, 23f));

        gridTop.addCell(createTdCell(vencParts[0], FONT_VAL_DATE, Element.ALIGN_CENTER, 23f));
        gridTop.addCell(createTdCell(vencParts[1], FONT_VAL_DATE, Element.ALIGN_CENTER, 23f));
        gridTop.addCell(createTdCell(vencParts[2], FONT_VAL_DATE, Element.ALIGN_CENTER, 23f));

        gridTop.addCell(createTdCell(montoFormatted, FONT_VAL_AMOUNT, Element.ALIGN_CENTER, 23f));

        bodyTable.addCell(new PdfPCell(gridTop));

        // --- B. TEXTO LEGAL "Por esta LETRA DE CAMBIO..." ---
        Paragraph pPay = new Paragraph();
        pPay.add(new Chunk("Por esta ", FONT_PHRASE_REGULAR));
        pPay.add(new Chunk("LETRA DE CAMBIO", FONT_PHRASE_BOLD));
        pPay.add(new Chunk(", se servirá(n) pagar a la orden de ", FONT_PHRASE_REGULAR));
        pPay.add(new Chunk("INDUSTRIAS PLASTICOS BELSA S.A.C.", FONT_PHRASE_GREEN));
        pPay.add(new Chunk(" la cantidad de:", FONT_PHRASE_REGULAR));

        PdfPCell payCell = new PdfPCell(pPay);
        payCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
        payCell.setBorderWidth(BORDER_WIDTH);
        payCell.setPadding(3.0f);
        payCell.setPaddingLeft(6f);
        bodyTable.addCell(payCell);

        // --- C. RECUADRO DE MONTO EN LETRAS ---
        PdfPCell montoLetrasBox = new PdfPCell(new Phrase(montoLetras, FONT_MONTO_LETRAS));
        montoLetrasBox.setBorder(Rectangle.BOX);
        montoLetrasBox.setBorderWidth(BORDER_WIDTH);
        montoLetrasBox.setPadding(4.0f);
        montoLetrasBox.setPaddingLeft(8f);
        montoLetrasBox.setBackgroundColor(new Color(250, 250, 250));
        bodyTable.addCell(montoLetrasBox);

        // --- D. TEXTO "Valor que sentará(n) en cuenta..." ---
        PdfPCell subTextCell = new PdfPCell(new Phrase("Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:", FONT_PHRASE_REGULAR));
        subTextCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        subTextCell.setBorderWidth(BORDER_WIDTH);
        subTextCell.setPadding(2.0f);
        subTextCell.setPaddingLeft(6f);
        bodyTable.addCell(subTextCell);

        // --- E. BLOQUE INFERIOR DIVIDIDO EN 2 COLUMNAS (Izquierda 55%, Derecha 45%) ---
        PdfPTable lowerSection = new PdfPTable(2);
        lowerSection.setWidthPercentage(100);
        lowerSection.setWidths(new float[]{55f, 45f});

        // Lado Izquierdo: GIRADO A (CLIENTE) Y AVALISTA
        PdfPTable leftSection = new PdfPTable(2);
        leftSection.setWidthPercentage(100);
        leftSection.setWidths(new float[]{22f, 78f});

        leftSection.addCell(createLabelCell("GIRADO A:"));
        leftSection.addCell(createValueBoldCell(cliente));

        leftSection.addCell(createLabelCell("RUC:"));
        leftSection.addCell(createValueCell(ruc));

        leftSection.addCell(createLabelCell("DIRECCION:"));
        leftSection.addCell(createValueCell(direccion));

        // Separador limpio
        PdfPCell sep1 = new PdfPCell(); sep1.setBorder(Rectangle.TOP); sep1.setBorderWidth(BORDER_WIDTH); sep1.setColspan(2); sep1.setFixedHeight(2f);
        leftSection.addCell(sep1);

        leftSection.addCell(createLabelCell("AVALISTA:"));
        leftSection.addCell(createDotsCell("........................................................................................"));

        leftSection.addCell(createLabelCell("D.I/R.U.C:"));
        PdfPTable subDoiPhone = new PdfPTable(3);
        subDoiPhone.setWidthPercentage(100);
        subDoiPhone.setWidths(new float[]{42f, 22f, 36f});
        subDoiPhone.addCell(createDotsCell("...................................."));
        subDoiPhone.addCell(createLabelCell("TELEFONO:"));
        subDoiPhone.addCell(createDotsCell("................................"));
        leftSection.addCell(new PdfPCell(subDoiPhone));

        leftSection.addCell(createLabelCell("DIRECCION:"));
        leftSection.addCell(createDotsCell("........................................................................................"));

        PdfPCell leftColCell = new PdfPCell(leftSection);
        leftColCell.setBorder(Rectangle.BOX);
        leftColCell.setBorderWidth(BORDER_WIDTH);
        leftColCell.setPadding(3.5f);
        lowerSection.addCell(leftColCell);

        // Lado Derecho: BANCO, BELSA Y FIRMA
        PdfPTable rightSection = new PdfPTable(1);
        rightSection.setWidthPercentage(100);

        PdfPCell debitText = new PdfPCell(new Phrase("Importe a debitar en cuenta del Aceptante del Banco:..................................", FONT_PHRASE_REGULAR));
        debitText.setBorder(Rectangle.NO_BORDER);
        debitText.setPaddingBottom(2.5f);
        rightSection.addCell(debitText);

        PdfPTable bankTable = new PdfPTable(4);
        bankTable.setWidthPercentage(100);
        bankTable.setWidths(new float[]{25f, 25f, 38f, 12f});
        bankTable.addCell(createBankThCell("BANCO"));
        bankTable.addCell(createBankThCell("OFICINA"));
        bankTable.addCell(createBankThCell("NUMERO DE CUENTA"));
        bankTable.addCell(createBankThCell("D.C."));
        bankTable.addCell(createBankTdCell());
        bankTable.addCell(createBankTdCell());
        bankTable.addCell(createBankTdCell());
        bankTable.addCell(createBankTdCell());
        rightSection.addCell(new PdfPCell(bankTable));

        Paragraph pBelsaTitle = new Paragraph("INDUSTRIAS PLASTICOS BELSA S.A.C.", FONT_BELSA_TITLE);
        pBelsaTitle.setAlignment(Element.ALIGN_CENTER);
        pBelsaTitle.setSpacingBefore(3f);
        rightSection.addCell(createBorderlessCell(pBelsaTitle));

        Paragraph pBelsaRuc = new Paragraph("RUC: 20544368827", FONT_BELSA_RUC);
        pBelsaRuc.setAlignment(Element.ALIGN_CENTER);
        rightSection.addCell(createBorderlessCell(pBelsaRuc));

        Paragraph pSignLine = new Paragraph(".....................................................................................................", FONT_DOTS);
        pSignLine.setAlignment(Element.ALIGN_CENTER);
        pSignLine.setSpacingBefore(5f);
        rightSection.addCell(createBorderlessCell(pSignLine));

        Paragraph pFirma = new Paragraph("FIRMA", FONT_SIGN_TEXT);
        pFirma.setAlignment(Element.ALIGN_CENTER);
        rightSection.addCell(createBorderlessCell(pFirma));

        Paragraph pRep = new Paragraph("Nombre del representante(S)", FONT_SUBTITLE);
        pRep.setAlignment(Element.ALIGN_CENTER);
        rightSection.addCell(createBorderlessCell(pRep));

        Paragraph pDoi = new Paragraph("D.O.I", FONT_LABEL_BOLD);
        pDoi.setAlignment(Element.ALIGN_LEFT);
        rightSection.addCell(createBorderlessCell(pDoi));

        PdfPCell rightColCell = new PdfPCell(rightSection);
        rightColCell.setBorder(Rectangle.BOX);
        rightColCell.setBorderWidth(BORDER_WIDTH);
        rightColCell.setPadding(3.5f);
        lowerSection.addCell(rightColCell);

        bodyTable.addCell(new PdfPCell(lowerSection));

        mainContainer.addCell(new PdfPCell(bodyTable));
        document.add(mainContainer);

        // Pie de página
        Paragraph pFooter = new Paragraph("No escribir ni firmar debajo de esta linea", FONT_FOOTNOTE);
        pFooter.setSpacingBefore(3f);
        pFooter.setIndentationLeft(document.getPageSize().getWidth() * 0.11f);
        document.add(pFooter);
    }

    private PdfPCell createThCell(String text, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_TH));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_WIDTH);
        cell.setPadding(2.0f);
        return cell;
    }

    private PdfPCell createSubThCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_SUB_TH));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_WIDTH);
        cell.setPadding(1.0f);
        return cell;
    }

    private PdfPCell createTdCell(String text, Font font, int align, float height) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_WIDTH);
        cell.setFixedHeight(height);
        cell.setPadding(1.5f);
        return cell;
    }

    private PdfPCell createLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_LABEL_BOLD));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(1.5f);
        return cell;
    }

    private PdfPCell createValueBoldCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_CLIENT_VAL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(1.5f);
        return cell;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_CLIENT_NORMAL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(1.5f);
        return cell;
    }

    private PdfPCell createDotsCell(String dots) {
        PdfPCell cell = new PdfPCell(new Phrase(dots, FONT_DOTS));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1.5f);
        return cell;
    }

    private PdfPCell createBankThCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_SUB_TH));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_WIDTH);
        cell.setPadding(1.2f);
        return cell;
    }

    private PdfPCell createBankTdCell() {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_WIDTH);
        cell.setFixedHeight(12f);
        return cell;
    }

    private PdfPCell createBorderlessCell(Paragraph p) {
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private String[] parseDateToParts(String dateStr) {
        try {
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                String clean = dateStr.trim().split("T")[0];
                String[] parts = clean.split("-");
                if (parts.length == 3) {
                    return new String[]{
                            String.format("%02d", Integer.parseInt(parts[2])),
                            String.format("%02d", Integer.parseInt(parts[1])),
                            parts[0]
                    };
                }
            }
        } catch (Exception ignored) {}
        LocalDate now = LocalDate.now();
        return new String[]{
                String.format("%02d", now.getDayOfMonth()),
                String.format("%02d", now.getMonthValue()),
                String.valueOf(now.getYear())
        };
    }

    public String generateHtml(Map<String, Object> letra) {
        String nroLetra = String.valueOf(letra.getOrDefault("nro_letra", "261-2025"));
        String refGirador = String.valueOf(letra.getOrDefault("ref_girador", "FF02 - 630"));
        String lugarGiro = String.valueOf(letra.getOrDefault("lugar_giro", "LIMA")).toUpperCase();

        String fechaGiroStr = String.valueOf(letra.getOrDefault("fecha_giro", LocalDate.now().toString()));
        String[] fgVals = parseDateToParts(fechaGiroStr);

        String fechaVencStr = String.valueOf(letra.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(30).toString()));
        String[] fvVals = parseDateToParts(fechaVencStr);

        Number montoNum = (Number) letra.getOrDefault("monto", 0.0);
        String moneda = String.valueOf(letra.getOrDefault("moneda", "SOLES")).toUpperCase();
        String symbol = moneda.contains("DOLAR") || moneda.contains("USD") ? "$" : "S/";
        String montoFormatted = String.format("%s %,.2f", symbol, montoNum.doubleValue()).replace(',', 'X').replace('.', ',').replace('X', '.');

        String montoLetras = String.valueOf(letra.getOrDefault("monto_letras", "CERO CON 00 / 100 SOLES")).toUpperCase();
        String cliente = String.valueOf(letra.getOrDefault("nombre_cliente", "CLIENTE S.A.C.")).toUpperCase();
        String ruc = String.valueOf(letra.getOrDefault("nro_documento", "-"));
        String direccion = String.valueOf(letra.getOrDefault("direccion_cliente", "LIMA - LIMA")).toUpperCase();

        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<title>Letra de Cambio - " + nroLetra + "</title>\n" +
                "<style>\n" +
                "  @page { size: A4 portrait; margin: 3mm 4mm; }\n" +
                "  * { box-sizing: border-box; -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }\n" +
                "  html, body { margin: 0; padding: 0; background: #fff; font-family: Calibri, 'Segoe UI', Arial, sans-serif; color: #141414; font-size: 9.5px; }\n" +
                "  .sheet { width: 100%; max-width: 820px; margin: 0 auto; padding: 2px; }\n" +
                "  .letra-layout { display: flex; align-items: stretch; gap: 4px; }\n" +
                "  .clauses-col { width: 88px; min-width: 88px; max-width: 88px; display: flex; flex-direction: column; justify-content: flex-end; padding: 1.5px 0; }\n" +
                "  .clauses-content { writing-mode: vertical-rl; transform: rotate(180deg); font-family: Calibri, sans-serif; font-size: 7pt; line-height: 1.25; color: #111; text-align: left; }\n" +
                "  .clauses-title { font-weight: bold; font-size: 7.2pt; margin-bottom: 2px; }\n" +
                "  .clause-p { margin-bottom: 2px; text-align: justify; }\n" +
                "  .tags-col { width: 18px; min-width: 18px; max-width: 18px; display: flex; flex-direction: column; justify-content: space-between; align-items: center; padding: 0; }\n" +
                "  .tag-group { display: flex; flex-direction: column; align-items: center; width: 100%; }\n" +
                "  .tag-line { border-left: 1.2px dashed #141414; width: 1px; flex: 1; min-height: 35px; }\n" +
                "  .tag-text { writing-mode: vertical-rl; transform: rotate(180deg); font-size: 7.5pt; font-weight: bold; white-space: nowrap; margin: 3px 0; }\n" +
                "  .main-col { flex: 1; display: flex; flex-direction: column; min-width: 0; }\n" +
                "  .top-table { border-collapse: collapse; width: 100%; table-layout: fixed; margin-bottom: 0; }\n" +
                "  .top-table th, .top-table td { border: 1px solid #141414; text-align: center; padding: 1.5px; }\n" +
                "  .top-table th { font-size: 7.2pt; font-weight: bold; line-height: 1.15; background: #fff; }\n" +
                "  .top-table .subhead { font-size: 6.5pt; font-weight: bold; padding: 1px; }\n" +
                "  .top-table td.val { font-size: 10.5pt; font-weight: bold; height: 28px; padding: 1px; }\n" +
                "  .top-table td.val.amount { font-size: 13pt; }\n" +
                "  .top-table td.val.date-val { font-size: 8.8pt; }\n" +
                "  .pay-phrase { border-left: 1px solid #141414; border-right: 1px solid #141414; padding: 3px 6px; font-size: 7.8pt; }\n" +
                "  .pay-phrase .beneficiary { color: #00B050; font-weight: bold; }\n" +
                "  .amount-box { border: 1px solid #141414; padding: 3.5px 6px; font-size: 8.6pt; font-weight: bold; background: #fafafa; }\n" +
                "  .payplace-note { border-left: 1px solid #141414; border-right: 1px solid #141414; border-bottom: 1px solid #141414; padding: 2px 6px; font-size: 7.2pt; }\n" +
                "  .lower-box { display: flex; border: 1px solid #141414; border-top: none; }\n" +
                "  .lower-left { width: 55%; border-right: 1px solid #141414; padding: 4px 7px; display: flex; flex-direction: column; justify-content: space-between; font-size: 7.5pt; }\n" +
                "  .girado-row { margin-bottom: 2.5px; line-height: 1.3; }\n" +
                "  .girado-row span.lbl { display: inline-block; min-width: 60px; font-weight: bold; }\n" +
                "  .girado-divider { border-top: 1px solid #333; margin: 3px 0; }\n" +
                "  .aval-row { margin-bottom: 2.5px; display: flex; align-items: flex-end; }\n" +
                "  .aval-row span.lbl { min-width: 60px; flex-shrink: 0; font-weight: bold; }\n" +
                "  .aval-dots { border-bottom: 1px dotted #333; flex: 1; height: 10px; margin-left: 2px; }\n" +
                "  .lower-right { width: 45%; padding: 4px 7px; display: flex; flex-direction: column; text-align: center; }\n" +
                "  .debit-line { font-size: 7.2pt; text-align: left; margin-bottom: 3px; }\n" +
                "  .bank-table { width: 100%; border-collapse: collapse; margin-bottom: 3px; }\n" +
                "  .bank-table th, .bank-table td { border: 1px solid #141414; padding: 1.5px; font-size: 6.8pt; }\n" +
                "  .bank-table th { font-weight: bold; }\n" +
                "  .bank-table td { height: 12px; }\n" +
                "  .company-title { color: #00B050; font-weight: bold; font-size: 9pt; margin-top: 2px; }\n" +
                "  .company-ruc { font-weight: bold; font-size: 8pt; margin-bottom: 4px; }\n" +
                "  .firma-block { margin-top: auto; text-align: center; }\n" +
                "  .sign-line { border-bottom: 1px dotted #333; width: 75%; margin: 4px auto 2px auto; }\n" +
                "  .sign-text { font-size: 7.5pt; font-weight: bold; }\n" +
                "  .rep-text { font-size: 6.5pt; color: #333; }\n" +
                "  .doi-text { font-size: 7.2pt; font-weight: bold; text-align: left; margin-top: 2px; }\n" +
                "  .footer-line { margin-top: 3px; font-size: 6.8pt; text-align: left; color: #444; margin-left: 11%; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"sheet\">\n" +
                "  <div class=\"letra-layout\">\n" +
                "    <div class=\"clauses-col\">\n" +
                "      <div class=\"clauses-content\">\n" +
                "        <div class=\"clauses-title\">CLÁUSULAS ESPECIALES:</div>\n" +
                "        <div class=\"clause-p\">(1) En caso de mora , esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor.</div>\n" +
                "        <div class=\"clause-p\">(2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale, sin que sea necesario la intervención del obligado principal ni de los solidarios.</div>\n" +
                "        <div class=\"clause-p\">(3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias.</div>\n" +
                "        <div class=\"clause-p\">(4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima.</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"tags-col\">\n" +
                "      <div class=\"tag-group\" style=\"height: 52%;\">\n" +
                "        <div class=\"tag-line\"></div>\n" +
                "        <span class=\"tag-text\">Aceptante(s)</span>\n" +
                "        <div class=\"tag-line\"></div>\n" +
                "      </div>\n" +
                "      <div class=\"tag-group\" style=\"height: 44%;\">\n" +
                "        <div class=\"tag-line\"></div>\n" +
                "        <span class=\"tag-text\">Por Aval</span>\n" +
                "        <div class=\"tag-line\"></div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"main-col\">\n" +
                "      <table class=\"top-table\">\n" +
                "        <colgroup>\n" +
                "          <col style=\"width: 18%;\"><col style=\"width: 18%;\"><col style=\"width: 14%;\">\n" +
                "          <col style=\"width: 5.7%;\"><col style=\"width: 5.7%;\"><col style=\"width: 5.7%;\">\n" +
                "          <col style=\"width: 5.7%;\"><col style=\"width: 5.7%;\"><col style=\"width: 5.7%;\">\n" +
                "          <col style=\"width: 16.1%;\">\n" +
                "        </colgroup>\n" +
                "        <tr>\n" +
                "          <th rowspan=\"2\">NUMERO DE LETRA</th>\n" +
                "          <th rowspan=\"2\">REF. DEL GIRADOR</th>\n" +
                "          <th rowspan=\"2\">LUGAR DE GIRO</th>\n" +
                "          <th colspan=\"3\">FECHA DE GIRO</th>\n" +
                "          <th colspan=\"3\">FECHA DE VENCIMIENTO</th>\n" +
                "          <th rowspan=\"2\">MONEDA E IMPORTE</th>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <th class=\"subhead\">DIA</th><th class=\"subhead\">MES</th><th class=\"subhead\">AÑO</th>\n" +
                "          <th class=\"subhead\">DIA</th><th class=\"subhead\">MES</th><th class=\"subhead\">AÑO</th>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <td class=\"val\">" + nroLetra + "</td>\n" +
                "          <td class=\"val\">" + refGirador + "</td>\n" +
                "          <td class=\"val\">" + lugarGiro + "</td>\n" +
                "          <td class=\"val date-val\">" + fgVals[0] + "</td><td class=\"val date-val\">" + fgVals[1] + "</td><td class=\"val date-val\">" + fgVals[2] + "</td>\n" +
                "          <td class=\"val date-val\">" + fvVals[0] + "</td><td class=\"val date-val\">" + fvVals[1] + "</td><td class=\"val date-val\">" + fvVals[2] + "</td>\n" +
                "          <td class=\"val amount\">" + montoFormatted + "</td>\n" +
                "        </tr>\n" +
                "      </table>\n" +
                "      <div class=\"pay-phrase\">\n" +
                "        Por esta <b>LETRA DE CAMBIO</b>, se servirá(n) pagar a la orden de <span class=\"beneficiary\">INDUSTRIAS PLASTICOS BELSA S.A.C.</span> la cantidad de:\n" +
                "      </div>\n" +
                "      <div class=\"amount-box\">" + montoLetras + "</div>\n" +
                "      <div class=\"payplace-note\">\n" +
                "        Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:\n" +
                "      </div>\n" +
                "      <div class=\"lower-box\">\n" +
                "        <div class=\"lower-left\">\n" +
                "          <div class=\"girado-row\"><span class=\"lbl\">GIRADO A:</span> <b>" + cliente + "</b></div>\n" +
                "          <div class=\"girado-row\"><span class=\"lbl\">RUC:</span> " + ruc + "</div>\n" +
                "          <div class=\"girado-row\"><span class=\"lbl\">DIRECCION:</span> " + direccion + "</div>\n" +
                "          <div class=\"girado-divider\"></div>\n" +
                "          <div class=\"aval-row\"><span class=\"lbl\">AVALISTA:</span> <div class=\"aval-dots\"></div></div>\n" +
                "          <div class=\"aval-row\"><span class=\"lbl\">D.I/R.U.C:</span> <div class=\"aval-dots\" style=\"max-width: 100px;\"></div><span style=\"margin: 0 4px; font-weight: bold;\">TELEFONO:</span> <div class=\"aval-dots\"></div></div>\n" +
                "          <div class=\"aval-row\"><span class=\"lbl\">DIRECCION:</span> <div class=\"aval-dots\"></div></div>\n" +
                "        </div>\n" +
                "        <div class=\"lower-right\">\n" +
                "          <div class=\"debit-line\">Importe a debitar en cuenta del Aceptante del Banco:..................................</div>\n" +
                "          <table class=\"bank-table\">\n" +
                "            <tr><th>BANCO</th><th>OFICINA</th><th>NUMERO DE CUENTA</th><th>D.C.</th></tr>\n" +
                "            <tr><td></td><td></td><td></td><td></td></tr>\n" +
                "          </table>\n" +
                "          <div class=\"company-title\">INDUSTRIAS PLASTICOS BELSA S.A.C.</div>\n" +
                "          <div class=\"company-ruc\">RUC: 20544368827</div>\n" +
                "          <div class=\"firma-block\">\n" +
                "            <div class=\"sign-line\"></div>\n" +
                "            <div class=\"sign-text\">FIRMA</div>\n" +
                "            <div class=\"rep-text\">Nombre del representante(S)</div>\n" +
                "            <div class=\"doi-text\">D.O.I</div>\n" +
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
}
