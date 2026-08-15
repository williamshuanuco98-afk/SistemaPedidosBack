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
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, Color.BLACK);
    private static final Font FONT_TD_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
    private static final Font FONT_TD_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
    
    private static final Font FONT_TEXT_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 7.0f, Color.BLACK);
    private static final Font FONT_TEXT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, Color.BLACK);
    private static final Font FONT_TEXT_GREEN_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7.5f, new Color(16, 140, 80));
    
    private static final Font FONT_MONTO_LETRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font FONT_CLAUSULAS = FontFactory.getFont(FontFactory.HELVETICA, 4.8f, Color.BLACK);
    private static final Font FONT_CLAUSULAS_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.2f, Color.BLACK);
    private static final Font FONT_SIDE_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 5.8f, Color.BLACK);
    
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 6.2f, new Color(60, 60, 60));
    private static final Font FONT_BOTTOM_NOTE = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(40, 40, 40));

    private static final Color HEADER_BG_GREEN = new Color(226, 235, 216); // Verde salvia exacto del modelo
    private static final float BORDER_THIN = 0.55f;

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 Portrait format (595.28 x 841.89 pt) - Vertical exacto como en la foto
            Document document = new Document(PageSize.A4, 20, 20, 20, 20);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            PdfContentByte cb = writer.getDirectContent();

            renderLetraCambioCard(document, cb, letra);

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

            // Nombre del PDF: exactamente el número de letra (ej: 226-2026.pdf)
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

    private void renderLetraCambioCard(Document document, PdfContentByte cb, Map<String, Object> letra) throws Exception {
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

        // -------------------------------------------------------------
        // TOP HEADER: [Logo Left] [Company Info Right]
        // -------------------------------------------------------------
        PdfPTable topHeader = new PdfPTable(2);
        topHeader.setWidthPercentage(100);
        topHeader.setWidths(new float[]{40f, 60f});

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        try {
            InputStream is = getClass().getResourceAsStream("/inplabel-logo.png");
            if (is != null) {
                byte[] imgBytes = is.readAllBytes();
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(140, 48);
                logoCell.addElement(img);
            } else {
                logoCell.addElement(new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(16, 185, 129))));
            }
        } catch (Exception e) {
            logoCell.addElement(new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(16, 185, 129))));
        }
        topHeader.addCell(logoCell);

        // Address & Phones
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

        // Vertical space
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(2f);
        document.add(spacer);

        // -------------------------------------------------------------
        // MAIN BOX: [Clauses Left (Rotated 90°)] [Right Body]
        // -------------------------------------------------------------
        PdfPTable mainFrame = new PdfPTable(2);
        mainFrame.setWidthPercentage(100);
        mainFrame.setWidths(new float[]{4.2f, 95.8f});

        // 1. LEFT CLAUSES (Texto vertical de abajo hacia arriba)
        PdfPCell clausesCell = new PdfPCell();
        clausesCell.setBorder(Rectangle.BOX);
        clausesCell.setBorderWidth(BORDER_THIN);
        clausesCell.setRotation(90); // Rotado 90°: corre de abajo hacia arriba
        clausesCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        clausesCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        clausesCell.setPadding(2f);

        Paragraph pClaus = new Paragraph();
        pClaus.add(new Chunk("CLÁUSULAS ESPECIALES: ", FONT_CLAUSULAS_BOLD));
        pClaus.add(new Chunk(
            "(1) En caso de mora , esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor. " +
            "(2) El plazo de su vencimiento podrá ser prorrogrado por el tenedor, por el plazo que este señale, sin que sea necesaria la intervención del obligado principal ni de los solidarios. " +
            "(3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias. " +
            "(4) Las partes se someten a la competencia de los jueces del DIstrito Judicial de Lima.",
            FONT_CLAUSULAS
        ));
        clausesCell.addElement(pClaus);
        mainFrame.addCell(clausesCell);

        // 2. RIGHT BODY CONTAINER
        PdfPCell rightBodyCell = new PdfPCell();
        rightBodyCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable bodyInner = new PdfPTable(1);
        bodyInner.setWidthPercentage(100);

        // 2.1 UPPER SECTION WITH "Aceptante(s)" SIDE TAG
        PdfPTable upperWithTag = new PdfPTable(2);
        upperWithTag.setWidthPercentage(100);
        upperWithTag.setWidths(new float[]{2.0f, 98.0f});

        // "Aceptante(s)" vertical cell
        PdfPCell aceptanteTag = new PdfPCell();
        aceptanteTag.setBorder(Rectangle.BOX);
        aceptanteTag.setBorderWidth(BORDER_THIN);
        aceptanteTag.setRotation(90);
        aceptanteTag.setVerticalAlignment(Element.ALIGN_MIDDLE);
        aceptanteTag.setHorizontalAlignment(Element.ALIGN_CENTER);
        aceptanteTag.setPadding(1f);
        aceptanteTag.addElement(new Paragraph("Aceptante(s)", FONT_SIDE_LABEL));
        upperWithTag.addCell(aceptanteTag);

        // Right side of upper part: Grid + Banner + MontoLetras + LugarPago
        PdfPCell upperRightCell = new PdfPCell();
        upperRightCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable upperContent = new PdfPTable(1);
        upperContent.setWidthPercentage(100);

        // 2.1.1 UPPER GRID: NUMERO DE LETRA | REF GIRADOR | LUGAR | FECHA GIRO | FECHA VENC | MONEDA E IMPORTE
        PdfPTable gridTable = new PdfPTable(6);
        gridTable.setWidthPercentage(100);
        gridTable.setWidths(new float[]{16.5f, 16.5f, 13.5f, 18f, 18.5f, 17f});

        gridTable.addCell(createThCell("NUMERO DE LETRA"));
        gridTable.addCell(createThCell("REF. DEL GIRADOR"));
        gridTable.addCell(createThCell("LUGAR DE GIRO"));
        gridTable.addCell(createDateThCell("FECHA DE GIRO"));
        gridTable.addCell(createDateThCell("FECHA DE VENCIMIENTO"));
        gridTable.addCell(createThCell("MONEDA E IMPORTE"));

        gridTable.addCell(createTdCell(nroLetra, Element.ALIGN_CENTER, FONT_TD_VAL, 16f));
        gridTable.addCell(createTdCell(refGirador, Element.ALIGN_CENTER, FONT_TD_VAL, 16f));
        gridTable.addCell(createTdCell(lugarGiro, Element.ALIGN_CENTER, FONT_TD_VAL, 16f));
        gridTable.addCell(createDateValuesCell(giroParts[0], giroParts[1], giroParts[2]));
        gridTable.addCell(createDateValuesCell(vencParts[0], vencParts[1], vencParts[2]));
        gridTable.addCell(createTdCell(montoFormatted, Element.ALIGN_CENTER, FONT_TD_VAL, 16f));

        PdfPCell gridContainer = new PdfPCell(gridTable);
        gridContainer.setBorder(Rectangle.NO_BORDER);
        upperContent.addCell(gridContainer);

        // 2.1.2 ORDER OF PAYMENT BANNER
        Paragraph pBanner = new Paragraph();
        pBanner.add(new Chunk("Por esta ", FONT_TEXT_REGULAR));
        pBanner.add(new Chunk("LETRA DE CAMBIO", FONT_TEXT_BOLD));
        pBanner.add(new Chunk(", se serivá(n) pagar a la orden de ", FONT_TEXT_REGULAR));
        pBanner.add(new Chunk("INDUSTRIAS PLASTICOS BELSA S.A.C.", FONT_TEXT_GREEN_BOLD));
        pBanner.add(new Chunk(" la cantidad de:", FONT_TEXT_REGULAR));
        
        PdfPCell bannerCell = new PdfPCell(pBanner);
        bannerCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
        bannerCell.setBorderWidth(BORDER_THIN);
        bannerCell.setPadding(2.5f);
        upperContent.addCell(bannerCell);

        // 2.1.3 AMOUNT IN WORDS BOX
        PdfPCell montoLetrasCell = new PdfPCell(new Phrase(montoLetras.toUpperCase(), FONT_MONTO_LETRAS));
        montoLetrasCell.setBorder(Rectangle.BOX);
        montoLetrasCell.setBorderWidth(BORDER_THIN);
        montoLetrasCell.setPadding(3.5f);
        montoLetrasCell.setMinimumHeight(16f);
        upperContent.addCell(montoLetrasCell);

        // 2.1.4 PLACE OF PAYMENT NOTE
        Paragraph pSub = new Paragraph("Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiete lugar de pago:", FONT_TEXT_REGULAR);
        PdfPCell subCell = new PdfPCell(pSub);
        subCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        subCell.setBorderWidth(BORDER_THIN);
        subCell.setPadding(2.0f);
        upperContent.addCell(subCell);

        upperRightCell.addElement(upperContent);
        upperWithTag.addCell(upperRightCell);

        PdfPCell upperTotalCell = new PdfPCell(upperWithTag);
        upperTotalCell.setBorder(Rectangle.NO_BORDER);
        bodyInner.addCell(upperTotalCell);

        // 2.2 LOWER SECTION: [Girado/Avalista Left] [Banco/Firma Right]
        PdfPTable lowerTable = new PdfPTable(2);
        lowerTable.setWidthPercentage(100);
        lowerTable.setWidths(new float[]{55f, 45f});

        // --- LOWER LEFT BLOCK (Girado A + Por Aval + Avalista) ---
        PdfPCell lowerLeftCell = new PdfPCell();
        lowerLeftCell.setBorder(Rectangle.BOX);
        lowerLeftCell.setBorderWidth(BORDER_THIN);
        lowerLeftCell.setPadding(0);

        PdfPTable leftInner = new PdfPTable(1);
        leftInner.setWidthPercentage(100);

        // Girado A (Cliente)
        PdfPCell giradoCell = new PdfPCell();
        giradoCell.setBorder(Rectangle.NO_BORDER);
        giradoCell.setPadding(3.0f);

        PdfPTable clientDetails = new PdfPTable(2);
        clientDetails.setWidthPercentage(100);
        clientDetails.setWidths(new float[]{18f, 82f});
        addDetailRow(clientDetails, "GIRADO A:", cliente);
        addDetailRow(clientDetails, "RUC:", ruc);
        addDetailRow(clientDetails, "DIRECCION:", direccion);
        giradoCell.addElement(clientDetails);
        leftInner.addCell(giradoCell);

        // Horizontal divider between Girado and Avalista
        PdfPCell sepCell = new PdfPCell();
        sepCell.setBorder(Rectangle.TOP);
        sepCell.setBorderWidth(BORDER_THIN);
        sepCell.setPadding(0);

        // Avalista with "Por Aval" tag
        PdfPTable avalWithTag = new PdfPTable(2);
        avalWithTag.setWidthPercentage(100);
        avalWithTag.setWidths(new float[]{3.8f, 96.2f});

        PdfPCell porAvalTag = new PdfPCell();
        porAvalTag.setBorder(Rectangle.RIGHT);
        porAvalTag.setBorderWidth(BORDER_THIN);
        porAvalTag.setRotation(90);
        porAvalTag.setVerticalAlignment(Element.ALIGN_MIDDLE);
        porAvalTag.setHorizontalAlignment(Element.ALIGN_CENTER);
        porAvalTag.setPadding(1f);
        porAvalTag.addElement(new Paragraph("Por Aval", FONT_SIDE_LABEL));
        avalWithTag.addCell(porAvalTag);

        PdfPCell avalContent = new PdfPCell();
        avalContent.setBorder(Rectangle.NO_BORDER);
        avalContent.setPadding(2.5f);

        PdfPTable avalDetails = new PdfPTable(2);
        avalDetails.setWidthPercentage(100);
        avalDetails.setWidths(new float[]{18f, 82f});
        addDetailRow(avalDetails, "AVALISTA:", ".....................................................................................................");
        
        PdfPCell docTelCell = new PdfPCell();
        docTelCell.setBorder(Rectangle.NO_BORDER);
        docTelCell.setPadding(1.0f);
        PdfPTable docTelTable = new PdfPTable(4);
        docTelTable.setWidthPercentage(100);
        docTelTable.setWidths(new float[]{18f, 32f, 18f, 32f});
        docTelTable.addCell(createLabelCell("D.I/R.U.C:"));
        docTelTable.addCell(createValueCell("...................................................."));
        docTelTable.addCell(createLabelCell("TELEFONO:"));
        docTelTable.addCell(createValueCell("...................................................."));
        
        PdfPCell dtCont = new PdfPCell(docTelTable);
        dtCont.setColspan(2);
        dtCont.setBorder(Rectangle.NO_BORDER);
        avalDetails.addCell(dtCont);

        addDetailRow(avalDetails, "DIRECCION:", ".....................................................................................................");

        avalContent.addElement(avalDetails);
        avalWithTag.addCell(avalContent);

        sepCell.addElement(avalWithTag);
        leftInner.addCell(sepCell);

        lowerLeftCell.addElement(leftInner);
        lowerTable.addCell(lowerLeftCell);

        // --- LOWER RIGHT BLOCK (Banco, Empresa, Firma) ---
        PdfPCell lowerRightCell = new PdfPCell();
        lowerRightCell.setBorder(Rectangle.BOX);
        lowerRightCell.setBorderWidth(BORDER_THIN);
        lowerRightCell.setPadding(3.0f);

        // Bank Line
        Paragraph pBankNote = new Paragraph("Importe a debitar en cuenta del Aceptante del Banco:.................................................", FONT_TEXT_REGULAR);
        lowerRightCell.addElement(pBankNote);

        // Bank Grid
        PdfPTable bankGrid = new PdfPTable(4);
        bankGrid.setWidthPercentage(100);
        bankGrid.setWidths(new float[]{25f, 25f, 40f, 10f});
        bankGrid.addCell(createThCell("BANCO"));
        bankGrid.addCell(createThCell("OFICINA"));
        bankGrid.addCell(createThCell("NUMERO DE CUENTA"));
        bankGrid.addCell(createThCell("D.C."));

        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL, 10f));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL, 10f));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL, 10f));
        bankGrid.addCell(createTdCell(" ", Element.ALIGN_CENTER, FONT_TD_SMALL, 10f));
        lowerRightCell.addElement(bankGrid);

        // Company Banner
        Paragraph pCompName = new Paragraph("INDUSTRIAS PLASTICOS BELSA S.A.C.", FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 8.5f, new Color(16, 140, 80)));
        pCompName.setAlignment(Element.ALIGN_CENTER);
        pCompName.setSpacingBefore(2f);
        Paragraph pCompRuc = new Paragraph("RUC: 20544368827", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.0f, Color.BLACK));
        pCompRuc.setAlignment(Element.ALIGN_CENTER);
        lowerRightCell.addElement(pCompName);
        lowerRightCell.addElement(pCompRuc);

        // Signature Line
        Paragraph pSigLine = new Paragraph("....................................................................................................", FontFactory.getFont(FontFactory.HELVETICA, 6.0f, Color.BLACK));
        pSigLine.setAlignment(Element.ALIGN_CENTER);
        pSigLine.setSpacingBefore(10f);
        Paragraph pSigLabel = new Paragraph("FIRMA", FONT_TEXT_BOLD);
        pSigLabel.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph pRepLabel = new Paragraph("Nombre del representante(S)", FONT_SUBTITLE);
        Paragraph pDoiLabel = new Paragraph("D.O.I", FONT_SUBTITLE);

        lowerRightCell.addElement(pSigLine);
        lowerRightCell.addElement(pSigLabel);
        lowerRightCell.addElement(pRepLabel);
        lowerRightCell.addElement(pDoiLabel);

        lowerTable.addCell(lowerRightCell);

        PdfPCell lowerTotalCell = new PdfPCell(lowerTable);
        lowerTotalCell.setBorder(Rectangle.NO_BORDER);
        bodyInner.addCell(lowerTotalCell);

        rightBodyCell.addElement(bodyInner);
        mainFrame.addCell(rightBodyCell);

        document.add(mainFrame);

        // Bottom Note
        Paragraph pBottom = new Paragraph("No escribir ni firmar debajo de esta linea", FONT_BOTTOM_NOTE);
        pBottom.setSpacingBefore(2f);
        document.add(pBottom);
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        table.addCell(createLabelCell(label));
        table.addCell(createValueCell(value));
    }

    private PdfPCell createLabelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_TEXT_BOLD));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1.2f);
        return c;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_TEXT_REGULAR));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1.2f);
        return c;
    }

    private PdfPCell createThCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_THIN);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2.5f);
        cell.setBackgroundColor(HEADER_BG_GREEN);
        return cell;
    }

    private PdfPCell createTdCell(String text, int align, Font font, float minHeight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_THIN);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2.5f);
        if (minHeight > 0) cell.setMinimumHeight(minHeight);
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
        container.setPadding(0.8f);
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
        container.setPadding(1.5f);
        container.setMinimumHeight(16f);
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
