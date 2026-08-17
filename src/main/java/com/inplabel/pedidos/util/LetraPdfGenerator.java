package com.inplabel.pedidos.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
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

    private static final Color COLOR_BELSA_GREEN = new Color(0, 175, 80);
    private static final Color COLOR_HEADER_BG = new Color(226, 239, 218); // #E2EFDA
    private static final Color COLOR_TEXT_DARK = new Color(20, 20, 20);

    private static final Font FONT_COMP_ADDR_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_DARK);
    private static final Font FONT_COMP_ADDR = FontFactory.getFont(FontFactory.HELVETICA, 7.0f, COLOR_TEXT_DARK);

    private static final Font FONT_HEADER_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, COLOR_TEXT_DARK);
    private static final Font FONT_HEADER_SUB_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.2f, COLOR_TEXT_DARK);
    private static final Font FONT_VAL_BIG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.0f, COLOR_TEXT_DARK);
    private static final Font FONT_VAL_AMOUNT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12.5f, COLOR_TEXT_DARK);
    private static final Font FONT_VAL_DATE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_TEXT_DARK);

    private static final Font FONT_PHRASE_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_DARK);
    private static final Font FONT_PHRASE_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_DARK);
    private static final Font FONT_PHRASE_GREEN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, COLOR_BELSA_GREEN);

    private static final Font FONT_MONTO_LETRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_TEXT_DARK);
    private static final Font FONT_LABEL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.0f, COLOR_TEXT_DARK);
    private static final Font FONT_CLIENT_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_DARK);
    private static final Font FONT_CLIENT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 7.0f, COLOR_TEXT_DARK);
    private static final Font FONT_DOTS = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(110, 110, 110));

    private static final Font FONT_BELSA_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.8f, COLOR_BELSA_GREEN);
    private static final Font FONT_BELSA_RUC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, COLOR_TEXT_DARK);
    private static final Font FONT_SIGN_TEXT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, COLOR_TEXT_DARK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 6.2f, new Color(80, 80, 80));
    private static final Font FONT_FOOTNOTE = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(60, 60, 60));

    private static final float BORDER_WIDTH = 0.75f;

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 12, 12, 12, 12);
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

        // 1. ENCABEZADO SUPERIOR (LOGO INPLABEL + DIRECCIÓN DERECHA)
        PdfPTable topHeader = new PdfPTable(2);
        topHeader.setWidthPercentage(100);
        topHeader.setWidths(new float[]{35f, 65f});

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(0);
        try {
            InputStream is = getClass().getResourceAsStream("/inplabel-logo.png");
            if (is != null) {
                byte[] imgBytes = is.readAllBytes();
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(120, 32);
                logoCell.addElement(img);
            } else {
                File localLogo = new File("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosFront/img/inplabel-logo.png");
                if (localLogo.exists()) {
                    Image img = Image.getInstance(localLogo.getAbsolutePath());
                    img.scaleToFit(120, 32);
                    logoCell.addElement(img);
                }
            }
        } catch (Exception ignored) {}
        topHeader.addCell(logoCell);

        // Dirección
        PdfPCell addrCell = new PdfPCell();
        addrCell.setBorder(Rectangle.NO_BORDER);
        addrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addrCell.setPadding(0);

        Paragraph pTopLine = new Paragraph("____________________________________________", FontFactory.getFont(FontFactory.HELVETICA, 6.0f, new Color(50, 50, 50)));
        pTopLine.setAlignment(Element.ALIGN_RIGHT);
        pTopLine.setSpacingAfter(1f);
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

        // Espacio pequeño entre encabezado y letra
        Paragraph pSpacer = new Paragraph(" ");
        pSpacer.setFont(FontFactory.getFont(FontFactory.HELVETICA, 3f));
        document.add(pSpacer);

        // 2. TABLA CONTENEDORA PRINCIPAL (3 COLUMNAS):
        // Col 1: Cláusulas Especiales exactas en Calibri 7pt (8.8%)
        // Col 2: Separador con líneas discontinuas Aceptante(s) / Por Aval (2.2%)
        // Col 3: Cuerpo de la Letra de Cambio (89.0%)
        PdfPTable mainContainer = new PdfPTable(3);
        mainContainer.setWidthPercentage(100);
        mainContainer.setWidths(new float[]{8.8f, 2.2f, 89.0f});

        // COLUMNA 1: CLÁUSULAS ESPECIALES
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

                    // Alineado exactamente desde el fondo del cajón hacia el tope del cajón
                    float baseY = rect.getBottom() + 1.0f;
                    float startX = rect.getLeft() + 1.0f;
                    float lineSpacing = 5.6f;

                    for (int i = 0; i < lines.length; i++) {
                        float x = startX + (i * lineSpacing);
                        if (i == 0) {
                            cb.setFontAndSize(bfBold, 6.8f);
                        } else {
                            cb.setFontAndSize(bfNorm, 6.5f);
                        }
                        cb.showTextAligned(Element.ALIGN_LEFT, lines[i], x, baseY, 90f);
                    }
                } catch (Exception ignored) {}
                cb.endText();
                cb.restoreState();
            }
        });
        mainContainer.addCell(clausesCell);

        // COLUMNA 2: LÍNEAS DISCONTINUAS ACEPTANTE Y POR AVAL
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

                    // Tag Aceptante(s)
                    float yAceptCenter = botY + (totalH * 0.77f);
                    cb.beginText();
                    cb.setFontAndSize(bfBold, 6.8f);
                    cb.setColorFill(COLOR_TEXT_DARK);
                    cb.showTextAligned(Element.ALIGN_CENTER, "Aceptante(s)", midX + 1f, yAceptCenter, 90f);
                    cb.endText();

                    float tagTextHalfH = 18f;
                    cb.setLineDash(2.5f, 2.0f);
                    cb.setLineWidth(0.8f);
                    cb.setColorStroke(COLOR_TEXT_DARK);

                    // Línea discontinua que nace en el tope de la tabla
                    cb.moveTo(midX, topY);
                    cb.lineTo(midX, yAceptCenter + tagTextHalfH);
                    cb.stroke();

                    // Línea discontinua debajo de Aceptante(s)
                    cb.moveTo(midX, yAceptCenter - tagTextHalfH);
                    cb.lineTo(midX, botY + (totalH * 0.52f));
                    cb.stroke();

                    // Tag Por Aval
                    float yAvalCenter = botY + (totalH * 0.22f);
                    cb.beginText();
                    cb.setFontAndSize(bfBold, 6.8f);
                    cb.setColorFill(COLOR_TEXT_DARK);
                    cb.showTextAligned(Element.ALIGN_CENTER, "Por Aval", midX + 1f, yAvalCenter, 90f);
                    cb.endText();

                    float tagAvalHalfH = 15f;
                    cb.moveTo(midX, botY + (totalH * 0.42f));
                    cb.lineTo(midX, yAvalCenter + tagAvalHalfH);
                    cb.stroke();

                    // Línea discontinua que termina en la base de la tabla
                    cb.moveTo(midX, yAvalCenter - tagAvalHalfH);
                    cb.lineTo(midX, botY);
                    cb.stroke();

                } catch (Exception ignored) {}
                cb.restoreState();
            }
        });
        mainContainer.addCell(tagsColCell);

        // COLUMNA 3: CUERPO PRINCIPAL DE LA LETRA
        PdfPTable bodyTable = new PdfPTable(1);
        bodyTable.setWidthPercentage(100);

        // --- A. CUADRÍCULA SUPERIOR (10 COLUMNAS: ROWSPAN=2 EN COLUMNAS DIRECTAS) ---
        PdfPTable gridTop = new PdfPTable(10);
        gridTop.setWidthPercentage(100);
        gridTop.setWidths(new float[]{18.0f, 18.0f, 14.0f, 5.7f, 5.7f, 5.7f, 5.7f, 5.7f, 5.7f, 16.1f});

        // Fila 1: Encabezados
        gridTop.addCell(createThCell("NUMERO DE LETRA", 1, 2));
        gridTop.addCell(createThCell("REF. DEL GIRADOR", 1, 2));
        gridTop.addCell(createThCell("LUGAR DE GIRO", 1, 2));
        gridTop.addCell(createThCell("FECHA DE GIRO", 3, 1));
        gridTop.addCell(createThCell("FECHA DE VENCIMIENTO", 3, 1));
        gridTop.addCell(createThCell("MONEDA E IMPORTE", 1, 2));

        // Fila 2: Sub-encabezados únicamente para las fechas
        gridTop.addCell(createSubThCell("DIA"));
        gridTop.addCell(createSubThCell("MES"));
        gridTop.addCell(createSubThCell("AÑO"));
        gridTop.addCell(createSubThCell("DIA"));
        gridTop.addCell(createSubThCell("MES"));
        gridTop.addCell(createSubThCell("AÑO"));

        // Fila 3: Valores
        gridTop.addCell(createTdCell(nroLetra, FONT_VAL_BIG, Element.ALIGN_CENTER, 22f));
        gridTop.addCell(createTdCell(refGirador, FONT_VAL_BIG, Element.ALIGN_CENTER, 22f));
        gridTop.addCell(createTdCell(lugarGiro, FONT_VAL_BIG, Element.ALIGN_CENTER, 22f));

        gridTop.addCell(createTdCell(giroParts[0], FONT_VAL_DATE, Element.ALIGN_CENTER, 22f));
        gridTop.addCell(createTdCell(giroParts[1], FONT_VAL_DATE, Element.ALIGN_CENTER, 22f));
        gridTop.addCell(createTdCell(giroParts[2], FONT_VAL_DATE, Element.ALIGN_CENTER, 22f));

        gridTop.addCell(createTdCell(vencParts[0], FONT_VAL_DATE, Element.ALIGN_CENTER, 22f));
        gridTop.addCell(createTdCell(vencParts[1], FONT_VAL_DATE, Element.ALIGN_CENTER, 22f));
        gridTop.addCell(createTdCell(vencParts[2], FONT_VAL_DATE, Element.ALIGN_CENTER, 22f));

        gridTop.addCell(createTdCell(montoFormatted, FONT_VAL_AMOUNT, Element.ALIGN_CENTER, 22f));

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
        montoLetrasBox.setPadding(3.5f);
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

        // --- E. BLOQUE INFERIOR SIN LÍNEA VERTICAL INTERMEDIA (UN SOLO RECUADRO) ---
        PdfPTable lowerSection = new PdfPTable(2);
        lowerSection.setWidthPercentage(100);
        lowerSection.setWidths(new float[]{55f, 45f});

        // Lado Izquierdo: GIRADO A (CLIENTE) Y AVALISTA (Sin cajas ni bordes internos)
        PdfPTable leftSection = new PdfPTable(2);
        leftSection.setWidthPercentage(100);
        leftSection.setWidths(new float[]{22f, 78f});

        leftSection.addCell(createLabelCell("GIRADO A:"));
        leftSection.addCell(createValueBoldCell(cliente));

        leftSection.addCell(createLabelCell("RUC:"));
        leftSection.addCell(createValueCell(ruc));

        leftSection.addCell(createLabelCell("DIRECCION:"));
        leftSection.addCell(createValueCell(direccion));

        // Separador sutil sin línea gruesa
        PdfPCell sep1 = new PdfPCell(); sep1.setBorder(Rectangle.NO_BORDER); sep1.setColspan(2); sep1.setFixedHeight(4f);
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
        leftColCell.setBorder(Rectangle.NO_BORDER); // SIN LÍNEA DIVISORIA A LA DERECHA
        leftColCell.setPadding(3.5f);
        lowerSection.addCell(leftColCell);

        // Lado Derecho: BANCO, BELSA Y FIRMA
        PdfPTable rightSection = new PdfPTable(1);
        rightSection.setWidthPercentage(100);

        PdfPCell debitText = new PdfPCell(new Phrase("Importe a debitar en cuenta del Aceptante del Banco:..................................", FONT_PHRASE_REGULAR));
        debitText.setBorder(Rectangle.NO_BORDER);
        debitText.setPaddingBottom(2.0f);
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
        pBelsaTitle.setSpacingBefore(2.5f);
        rightSection.addCell(createBorderlessCell(pBelsaTitle));

        Paragraph pBelsaRuc = new Paragraph("RUC: 20544368827", FONT_BELSA_RUC);
        pBelsaRuc.setAlignment(Element.ALIGN_CENTER);
        rightSection.addCell(createBorderlessCell(pBelsaRuc));

        Paragraph pSignLine = new Paragraph(".....................................................................................................", FONT_DOTS);
        pSignLine.setAlignment(Element.ALIGN_CENTER);
        pSignLine.setSpacingBefore(4f);
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
        rightColCell.setBorder(Rectangle.NO_BORDER);
        rightColCell.setPadding(3.5f);
        lowerSection.addCell(rightColCell);

        // Borde exterior del bloque inferior
        PdfPCell lowerContainerCell = new PdfPCell(lowerSection);
        lowerContainerCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        lowerContainerCell.setBorderWidth(BORDER_WIDTH);
        bodyTable.addCell(lowerContainerCell);

        mainContainer.addCell(new PdfPCell(bodyTable));
        document.add(mainContainer);

        // Pie de página
        Paragraph pFooter = new Paragraph("No escribir ni firmar debajo de esta linea", FONT_FOOTNOTE);
        pFooter.setSpacingBefore(2.5f);
        pFooter.setIndentationLeft(document.getPageSize().getWidth() * 0.11f);
        document.add(pFooter);
    }

    private PdfPCell createThCell(String text, int colspan, int rowspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_TH));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_WIDTH);
        cell.setPadding(2.0f);
        return cell;
    }

    private PdfPCell createSubThCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_SUB_TH));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_HEADER_BG);
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
        cell.setPadding(1.2f);
        return cell;
    }

    private PdfPCell createValueBoldCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_CLIENT_VAL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(1.2f);
        return cell;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_CLIENT_NORMAL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(1.2f);
        return cell;
    }

    private PdfPCell createDotsCell(String dots) {
        PdfPCell cell = new PdfPCell(new Phrase(dots, FONT_DOTS));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1.2f);
        return cell;
    }

    private PdfPCell createBankThCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER_SUB_TH));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_HEADER_BG);
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
        return "";
    }
}
