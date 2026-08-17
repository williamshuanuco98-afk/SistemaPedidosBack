package com.inplabel.pedidos.util;

import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.*;
import java.time.LocalDate;
import java.util.Map;

@Component
public class LetraPdfGenerator {

    private static final Color COLOR_TEXT = new Color(20, 20, 20);

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try {
            byte[] templateBytes = loadTemplatePdfBytes();
            if (templateBytes == null || templateBytes.length == 0) {
                throw new IllegalStateException("No se pudo cargar la plantilla base 'Plantilla excel.pdf'");
            }

            PdfReader reader = new PdfReader(templateBytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, baos);
            PdfContentByte cb = stamper.getOverContent(1);

            renderDynamicValuesOnTemplate(cb, letra);

            stamper.close();
            reader.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de Letra de Cambio con plantilla: " + e.getMessage(), e);
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

    private byte[] loadTemplatePdfBytes() {
        // 1. Intentar desde recurso classpath
        try (InputStream is = getClass().getResourceAsStream("/templates/Plantilla excel.pdf")) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception ignored) {}

        // 2. Intentar desde disco local
        File f1 = new File("Plantilla excel.pdf");
        if (f1.exists()) {
            try (FileInputStream fis = new FileInputStream(f1)) {
                return fis.readAllBytes();
            } catch (Exception ignored) {}
        }

        File f2 = new File("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        if (f2.exists()) {
            try (FileInputStream fis = new FileInputStream(f2)) {
                return fis.readAllBytes();
            } catch (Exception ignored) {}
        }

        return null;
    }

    private void renderDynamicValuesOnTemplate(PdfContentByte cb, Map<String, Object> letra) throws Exception {
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

        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        BaseFont bfNorm = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        cb.beginText();
        cb.setColorFill(COLOR_TEXT);

        // --- 1. CABECERA SUPERIOR ---
        // Para columnas de cuadro completo (NUMERO DE LETRA, REF. GIRADOR, LUGAR, MONEDA E IMPORTE):
        // Se centran verticalmente en medio de la caja total (Y = 741.0)
        float yMergedRow = 741.0f;
        // Para fechas (que van en el sub-cajón inferior bajo DIA/MES/AÑO):
        float yDateRow = 736.0f;

        // 1. NUMERO DE LETRA (Centro vertical y horizontal)
        cb.setFontAndSize(bfBold, 12.0f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, nroLetra, 136.5f, yMergedRow, 0);

        // 2. REF. DEL GIRADOR (Centro vertical y horizontal)
        float refSize = 10.0f;
        if (refGirador.length() > 14) refSize = 8.8f;
        cb.setFontAndSize(bfBold, refSize);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, refGirador, 210.5f, yMergedRow, 0);

        // 3. LUGAR DE GIRO (Centro vertical y horizontal)
        float lugSize = 12.0f;
        if (lugarGiro.length() > 8) lugSize = 9.5f;
        cb.setFontAndSize(bfBold, lugSize);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, lugarGiro, 276.5f, yMergedRow, 0);

        // 4. FECHA DE GIRO (En su sub-cajón inferior)
        cb.setFontAndSize(bfBold, 9.5f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, giroParts[0], 322.2f, yDateRow, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, giroParts[1], 352.8f, yDateRow, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, giroParts[2], 383.4f, yDateRow, 0);

        // 5. FECHA DE VENCIMIENTO (En su sub-cajón inferior)
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, vencParts[0], 415.2f, yDateRow, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, vencParts[1], 449.5f, yDateRow, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, vencParts[2], 482.5f, yDateRow, 0);

        // 6. MONEDA E IMPORTE (Centro vertical y horizontal)
        cb.setFontAndSize(bfBold, 12.5f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, montoFormatted, 539.8f, yMergedRow, 0);

        // --- 2. MONTO EN LETRAS ---
        float montoFontSize = 9.2f;
        if (montoLetras.length() > 65) montoFontSize = 8.0f;
        if (montoLetras.length() > 80) montoFontSize = 7.0f;
        cb.setFontAndSize(bfBold, montoFontSize);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, montoLetras, 104.0f, 703.0f, 0);

        // --- 3. DATOS DEL CLIENTE (CALIBRI 8pt REGULAR) ---
        cb.setFontAndSize(bfNorm, 8.0f);

        // GIRADO A (Línea base: 672.17)
        if (cliente.length() > 40) {
            String line1 = cliente;
            String line2 = "";
            int splitIdx = cliente.lastIndexOf(' ', 38);
            if (splitIdx > 0) {
                line1 = cliente.substring(0, splitIdx).trim();
                line2 = cliente.substring(splitIdx).trim();
            }
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, line1, 150.0f, 672.17f, 0);
            if (!line2.isEmpty()) {
                cb.showTextAligned(PdfContentByte.ALIGN_LEFT, line2, 150.0f, 660.0f, 0);
            }
        } else {
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, cliente, 150.0f, 672.17f, 0);
        }

        // RUC (Línea base: 641.95)
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, ruc, 150.0f, 641.95f, 0);

        // DIRECCION (Línea base: 630.75)
        if (direccion.length() > 48) {
            cb.setFontAndSize(bfNorm, 7.2f);
            String d1 = direccion;
            String d2 = "";
            int splitD = direccion.lastIndexOf(' ', 46);
            if (splitD > 0) {
                d1 = direccion.substring(0, splitD).trim();
                d2 = direccion.substring(splitD).trim();
            }
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, d1, 150.0f, 630.75f, 0);
            if (!d2.isEmpty()) {
                cb.showTextAligned(PdfContentByte.ALIGN_LEFT, d2, 150.0f, 620.0f, 0);
            }
        } else {
            cb.setFontAndSize(bfNorm, 7.8f);
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, direccion, 150.0f, 630.75f, 0);
        }

        cb.endText();
    }

    public String generateHtml(Map<String, Object> letra) {
        return "<!DOCTYPE html><html><head><title>Letra</title></head><body><p>Generando vista...</p></body></html>";
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
}
