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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class GuiaPdfGenerator {

    private static final Font FONT_COMP_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
    private static final Font FONT_COMP_SUB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 5.2f, new Color(40, 40, 40));
    private static final Font FONT_COMP_INFO = FontFactory.getFont(FontFactory.HELVETICA, 5.0f, new Color(50, 50, 50));
    
    private static final Font FONT_RUC_BOX_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.8f, Color.BLACK);
    private static final Font FONT_RUC_BOX_TYPE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f, Color.BLACK);
    private static final Font FONT_RUC_BOX_NRO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);

    private static final Font FONT_FIELD_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.8f, Color.BLACK);
    private static final Font FONT_FIELD_VAL = FontFactory.getFont(FontFactory.HELVETICA, 6.8f, Color.BLACK);

    private static final Font FONT_BOX_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.BLACK);
    private static final Font FONT_BOX_TEXT = FontFactory.getFont(FontFactory.HELVETICA, 6f, Color.BLACK);

    private static final Font FONT_TABLE_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.BLACK);
    private static final Font FONT_TABLE_TD = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, Color.BLACK);
    private static final Font FONT_TABLE_TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.BLACK);

    private static final Font FONT_FOOTER_LEGEND = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, new Color(80, 80, 80));

    private static final Color COLOR_GREEN_HEADER = new Color(209, 231, 221); // Soft pastel green #d1e7dd
    private static final float BORDER_THIN = 0.5f;

    public byte[] generatePdfBytes(Map<String, Object> guia) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 Landscape document with compact margins
            Document document = new Document(PageSize.A4.rotate(), 14, 14, 14, 14);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Two-column layout: [Left Half 48.5%] [Dotted divider 3%] [Right Half 48.5%]
            PdfPTable containerTable = new PdfPTable(3);
            containerTable.setWidthPercentage(100);
            containerTable.setWidths(new float[]{48.5f, 3.0f, 48.5f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(buildSingleGuiaHalf(guia, "REMITENTE"));

            PdfPCell dividerCell = new PdfPCell();
            dividerCell.setBorder(Rectangle.NO_BORDER);
            dividerCell.setCellEvent(new DottedLineCellEvent());

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.addElement(buildSingleGuiaHalf(guia, "CLIENTE"));

            containerTable.addCell(leftCell);
            containerTable.addCell(dividerCell);
            containerTable.addCell(rightCell);

            document.add(containerTable);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de Guía: " + e.getMessage(), e);
        }
    }

    public String savePdfToDisk(Map<String, Object> guia, String baseDir, boolean useSubfolders) {
        try {
            byte[] bytes = generatePdfBytes(guia);
            String nroGuia = (String) guia.getOrDefault("nro_guia", "GR001-0001");
            
            String targetDir = (baseDir != null && !baseDir.trim().isEmpty()) ? baseDir.trim() : "C:\\Inplabel\\Guias";
            
            if (useSubfolders) {
                LocalDate now = LocalDate.now();
                targetDir = targetDir + File.separator + now.getYear() + File.separator + String.format("%02d", now.getMonthValue());
            }

            File dir = new File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "GUIA_" + nroGuia.replaceAll("[^a-zA-Z0-9-_]", "_") + ".pdf";
            File targetFile = new File(dir, filename);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(bytes);
            }

            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Advertencia al guardar PDF en disco: " + e.getMessage());
            return null;
        }
    }

    private PdfPTable buildSingleGuiaHalf(Map<String, Object> guia, String copyLegend) {
        PdfPTable half = new PdfPTable(1);
        half.setWidthPercentage(100);

        String nroGuia = (String) guia.getOrDefault("nro_guia", "GR001-0001");
        String cliente = (String) guia.getOrDefault("nombre_cliente", "Cliente General");
        String ruc = (String) guia.getOrDefault("nro_documento", "-");
        String fecha = formatFecha(guia.get("fecha_guia") != null ? guia.get("fecha_guia") : guia.get("fecha_emision"));
        String puntoPartida = (String) guia.getOrDefault("punto_partida", "C.P. Las Piedritas Av. Las Piedritas Mz D Lt 9 - CARABAYLLO - LIMA - LIMA");
        String puntoLlegada = (String) guia.getOrDefault("punto_llegada", guia.getOrDefault("direccion_destino", "Dirección del Cliente - LIMA - LIMA"));
        String obs = (String) guia.getOrDefault("observaciones", "");

        // -------------------------------------------------------------
        // 1. TOP HEADER: [Logo] [Company Details] [RUC Box]
        // -------------------------------------------------------------
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        try {
            headerTable.setWidths(new float[]{22f, 44f, 34f});
        } catch (Exception ignored) {}

        // 1.1 Logo Cell
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            InputStream is = getClass().getResourceAsStream("/inplabel-logo.png");
            if (is != null) {
                byte[] imgBytes = is.readAllBytes();
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(70, 42);
                logoCell.addElement(img);
            } else {
                Paragraph logoFallback = new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(16, 185, 129)));
                logoCell.addElement(logoFallback);
            }
        } catch (Exception e) {
            Paragraph logoFallback = new Paragraph("INPLABEL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(16, 185, 129)));
            logoCell.addElement(logoFallback);
        }
        headerTable.addCell(logoCell);

        // 1.2 Company Details Cell
        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.setPaddingLeft(2f);
        compCell.addElement(new Paragraph("INDUSTRIAS PLASTICOS BELSA S.A.C", FONT_COMP_TITLE));
        compCell.addElement(new Paragraph("DISEÑO, FABRICACIÓN Y COMERCIALIZACIÓN DE ENVASES PLÁSTICOS", FONT_COMP_SUB));
        compCell.addElement(new Paragraph("Contacto: 983 518 504 - 975 564 460", FONT_COMP_INFO));
        compCell.addElement(new Paragraph("www.inplabel.com.pe - ventas@inplabel.com.pe - inplabelsac@gmail.com", FONT_COMP_INFO));
        compCell.addElement(new Paragraph("Principal: Av. Maria Parado de Bellido Lt. 5 Lotizacion Chacra Cerro - Comas - Lima - Lima", FONT_COMP_INFO));
        compCell.addElement(new Paragraph("Sucursal: C.P. Las Piedritas Av. Las Piedritas Mz D Lt 9 - Carabayllo - Lima - Lima", FONT_COMP_INFO));
        headerTable.addCell(compCell);

        // 1.3 RUC Box Cell (Exact layout: RUC, GUIA DE REMISION DE CONTROL INTERNO, NRO_GUIA)
        PdfPCell rucCell = new PdfPCell();
        rucCell.setBorder(Rectangle.BOX);
        rucCell.setBorderWidth(BORDER_THIN);
        rucCell.setBorderColor(Color.BLACK);
        rucCell.setPadding(4f);
        rucCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rucCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pRuc = new Paragraph("RUC: 20544368827", FONT_RUC_BOX_TITLE);
        pRuc.setAlignment(Element.ALIGN_CENTER);
        Paragraph pGuia = new Paragraph("GUIA DE REMISION DE CONTROL INTERNO", FONT_RUC_BOX_TYPE);
        pGuia.setAlignment(Element.ALIGN_CENTER);
        Paragraph pNro = new Paragraph(nroGuia, FONT_RUC_BOX_NRO);
        pNro.setAlignment(Element.ALIGN_CENTER);

        rucCell.addElement(pRuc);
        rucCell.addElement(pGuia);
        rucCell.addElement(pNro);
        headerTable.addCell(rucCell);

        PdfPCell hContainer = new PdfPCell(headerTable);
        hContainer.setBorder(Rectangle.NO_BORDER);
        hContainer.setPaddingBottom(4f);
        half.addCell(hContainer);

        // -------------------------------------------------------------
        // 2. DATOS DEL CLIENTE:
        // Row 1: DESTINATARIO: [Nombre]
        // Row 2: RUC: [RUC]              FECHA: [Fecha]
        // -------------------------------------------------------------
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        try {
            clientTable.setWidths(new float[]{55f, 45f});
        } catch (Exception ignored) {}

        // DESTINATARIO (Full width)
        PdfPCell cDest = new PdfPCell();
        cDest.setColspan(2);
        cDest.setBorder(Rectangle.NO_BORDER);
        cDest.setPadding(1f);
        Paragraph pDest = new Paragraph();
        pDest.add(new Chunk("DESTINATARIO: ", FONT_FIELD_LABEL));
        pDest.add(new Chunk(cliente, FONT_FIELD_VAL));
        cDest.addElement(pDest);
        clientTable.addCell(cDest);

        // RUC
        PdfPCell cRuc = new PdfPCell();
        cRuc.setBorder(Rectangle.NO_BORDER);
        cRuc.setPadding(1f);
        Paragraph pRucCli = new Paragraph();
        pRucCli.add(new Chunk("RUC: ", FONT_FIELD_LABEL));
        pRucCli.add(new Chunk(ruc, FONT_FIELD_VAL));
        cRuc.addElement(pRucCli);
        clientTable.addCell(cRuc);

        // FECHA
        PdfPCell cFecha = new PdfPCell();
        cFecha.setBorder(Rectangle.NO_BORDER);
        cFecha.setPadding(1f);
        Paragraph pFecha = new Paragraph();
        pFecha.add(new Chunk("FECHA: ", FONT_FIELD_LABEL));
        pFecha.add(new Chunk(fecha, FONT_FIELD_VAL));
        cFecha.addElement(pFecha);
        clientTable.addCell(cFecha);

        PdfPCell clientContainer = new PdfPCell(clientTable);
        clientContainer.setBorder(Rectangle.NO_BORDER);
        clientContainer.setPaddingBottom(4f);
        half.addCell(clientContainer);

        // -------------------------------------------------------------
        // 3. PUNTO DE PARTIDA & PUNTO DE LLEGADA BOXES (Thin Borders)
        // -------------------------------------------------------------
        PdfPTable pointsTable = new PdfPTable(2);
        pointsTable.setWidthPercentage(100);
        try {
            pointsTable.setWidths(new float[]{50f, 50f});
        } catch (Exception ignored) {}

        // Punto de Partida Box
        PdfPCell partidaCell = new PdfPCell();
        partidaCell.setBorder(Rectangle.BOX);
        partidaCell.setBorderWidth(BORDER_THIN);
        partidaCell.setBorderColor(Color.BLACK);
        partidaCell.setPadding(3f);

        Paragraph pPartidaTitle = new Paragraph("Punto de partida", FONT_BOX_HEADER);
        pPartidaTitle.setAlignment(Element.ALIGN_CENTER);
        partidaCell.addElement(pPartidaTitle);

        Paragraph pPartidaDir = new Paragraph();
        pPartidaDir.add(new Chunk("DIRECCIÓN: ", FONT_FIELD_LABEL));
        pPartidaDir.add(new Chunk(puntoPartida, FONT_BOX_TEXT));
        partidaCell.addElement(pPartidaDir);

        Paragraph pPartidaUbigeo = new Paragraph("UBIGEO: - -", FONT_BOX_TEXT);
        partidaCell.addElement(pPartidaUbigeo);
        pointsTable.addCell(partidaCell);

        // Punto de Llegada Box
        PdfPCell llegadaCell = new PdfPCell();
        llegadaCell.setBorder(Rectangle.BOX);
        llegadaCell.setBorderWidth(BORDER_THIN);
        llegadaCell.setBorderColor(Color.BLACK);
        llegadaCell.setPadding(3f);

        Paragraph pLlegadaTitle = new Paragraph("Punto de Llegada", FONT_BOX_HEADER);
        pLlegadaTitle.setAlignment(Element.ALIGN_CENTER);
        llegadaCell.addElement(pLlegadaTitle);

        Paragraph pLlegadaDir = new Paragraph();
        pLlegadaDir.add(new Chunk("DIRECCIÓN: ", FONT_FIELD_LABEL));
        pLlegadaDir.add(new Chunk(puntoLlegada, FONT_BOX_TEXT));
        llegadaCell.addElement(pLlegadaDir);

        Paragraph pLlegadaUbigeo = new Paragraph("UBIGEO: - -", FONT_BOX_TEXT);
        llegadaCell.addElement(pLlegadaUbigeo);
        pointsTable.addCell(llegadaCell);

        PdfPCell pointsContainer = new PdfPCell(pointsTable);
        pointsContainer.setBorder(Rectangle.NO_BORDER);
        pointsContainer.setPaddingBottom(4f);
        half.addCell(pointsContainer);

        // -------------------------------------------------------------
        // 4. PRODUCTS TABLE (Green Header, NO PESO column: ITEM | DESCRIPCION | U.M. | CANTIDAD)
        // -------------------------------------------------------------
        PdfPTable prodTable = new PdfPTable(4);
        prodTable.setWidthPercentage(100);
        try {
            prodTable.setWidths(new float[]{8f, 68f, 10f, 14f});
        } catch (Exception ignored) {}

        prodTable.addCell(createThCell("ITEM", Element.ALIGN_CENTER));
        prodTable.addCell(createThCell("DESCRIPCION", Element.ALIGN_CENTER));
        prodTable.addCell(createThCell("U.M.", Element.ALIGN_CENTER));
        prodTable.addCell(createThCell("CANTIDAD", Element.ALIGN_CENTER));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detalles = (List<Map<String, Object>>) guia.get("detalles");
        if (detalles != null && !detalles.isEmpty()) {
            int idx = 1;
            for (Map<String, Object> d : detalles) {
                String nombre = (String) d.getOrDefault("nombre_producto", "Producto");
                String codigo = (String) d.getOrDefault("codigo_producto", "");
                String descripcion = (codigo != null && !codigo.isEmpty() ? "[" + codigo + "] " : "") + nombre;
                Object cantObj = d.getOrDefault("cantidad", 1);
                String cant = String.valueOf(cantObj);

                prodTable.addCell(createTdCell(String.valueOf(idx++), Element.ALIGN_CENTER, false));
                prodTable.addCell(createTdCell(descripcion, Element.ALIGN_LEFT, false));
                prodTable.addCell(createTdCell("UND", Element.ALIGN_CENTER, false));
                prodTable.addCell(createTdCell(cant, Element.ALIGN_CENTER, true));
            }
        } else {
            PdfPCell emptyC = new PdfPCell(new Phrase("Sin productos especificados", FONT_TABLE_TD));
            emptyC.setColspan(4);
            emptyC.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyC.setPadding(6f);
            emptyC.setBorder(Rectangle.BOX);
            emptyC.setBorderWidth(BORDER_THIN);
            emptyC.setBorderColor(Color.BLACK);
            prodTable.addCell(emptyC);
        }

        PdfPCell prodContainer = new PdfPCell(prodTable);
        prodContainer.setBorder(Rectangle.NO_BORDER);
        prodContainer.setPaddingBottom(4f);
        half.addCell(prodContainer);

        // -------------------------------------------------------------
        // 5. OBSERVACIONES BOX (Thin Borders)
        // -------------------------------------------------------------
        PdfPCell obsCell = new PdfPCell();
        obsCell.setBorder(Rectangle.BOX);
        obsCell.setBorderWidth(BORDER_THIN);
        obsCell.setBorderColor(Color.BLACK);
        obsCell.setPadding(3.5f);
        obsCell.setBackgroundColor(Color.WHITE);

        Paragraph obsTitle = new Paragraph("OBSERVACIONES:", FONT_FIELD_LABEL);
        obsCell.addElement(obsTitle);

        if (obs != null && !obs.trim().isEmpty() && !obs.equals("-")) {
            Paragraph obsText = new Paragraph(obs, FONT_FIELD_VAL);
            obsCell.addElement(obsText);
        }

        Paragraph disclaimer = new Paragraph("Este documento es de uso exclusivo para control interno y no tiene validez ante SUNAT.", FONT_BOX_TEXT);
        obsCell.addElement(disclaimer);

        half.addCell(obsCell);

        // -------------------------------------------------------------
        // 6. BOTTOM FOOTER LEGEND (Placed right at the bottom)
        // -------------------------------------------------------------
        PdfPCell footerCell = new PdfPCell();
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setPaddingTop(8f);
        footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pFooter = new Paragraph("[ COPIA: " + copyLegend + " ]", FONT_FOOTER_LEGEND);
        pFooter.setAlignment(Element.ALIGN_CENTER);
        footerCell.addElement(pFooter);

        half.addCell(footerCell);

        return half;
    }

    private PdfPCell createThCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_TH));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_THIN);
        cell.setBorderColor(Color.BLACK);
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(COLOR_GREEN_HEADER);
        cell.setPadding(2.5f);
        return cell;
    }

    private PdfPCell createTdCell(String text, int align, boolean bold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? FONT_TABLE_TD_BOLD : FONT_TABLE_TD));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(BORDER_THIN);
        cell.setBorderColor(Color.BLACK);
        cell.setHorizontalAlignment(align);
        cell.setPadding(2.5f);
        return cell;
    }

    private String formatFecha(Object obj) {
        if (obj == null) return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String str = String.valueOf(obj);
        if (str.contains("-") && str.length() >= 10) {
            try {
                String[] p = str.substring(0, 10).split("-");
                return p[2] + "/" + p[1] + "/" + p[0];
            } catch (Exception ignored) {}
        }
        return str;
    }

    // Event to draw a vertical dotted dividing line in the middle of the A4 page
    private static class DottedLineCellEvent implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
            cb.saveState();
            cb.setLineDash(3f, 3f);
            cb.setColorStroke(Color.GRAY);
            cb.setLineWidth(0.6f);
            float middleX = (position.getLeft() + position.getRight()) / 2.0f;
            cb.moveTo(middleX, position.getBottom());
            cb.lineTo(middleX, position.getTop());
            cb.stroke();
            cb.restoreState();
        }
    }
}
