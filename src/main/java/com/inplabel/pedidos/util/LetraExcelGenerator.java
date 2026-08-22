package com.inplabel.pedidos.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class LetraExcelGenerator {

    private static final String TEMPLATE_PATH = "templates/plantilla_letra.xlsx";
    private static final String FALLBACK_DESKTOP_PATH = "C:\\Users\\User\\OneDrive\\Escritorio\\nueva letra.xlsx";

    private InputStream getTemplateInputStream() {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (Exception ignored) {
        }

        try {
            File fallback = new File(FALLBACK_DESKTOP_PATH);
            if (fallback.exists()) {
                return new FileInputStream(fallback);
            }
        } catch (Exception ignored) {
        }

        throw new RuntimeException(
                "No se encontró la plantilla de Excel en " + TEMPLATE_PATH + " ni en " + FALLBACK_DESKTOP_PATH);
    }

    public byte[] generateExcelBytes(Map<String, Object> letra) {
        try (InputStream is = getTemplateInputStream();
                Workbook workbook = new XSSFWorkbook(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);
            fillSheetWithLetra(sheet, letra);

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de Letra de Cambio: " + e.getMessage(), e);
        }
    }

    public byte[] generateExcelBatchBytes(List<Map<String, Object>> letras) {
        if (letras == null || letras.isEmpty()) {
            throw new IllegalArgumentException("La lista de letras está vacía");
        }

        try (InputStream is = getTemplateInputStream();
                Workbook templateWorkbook = new XSSFWorkbook(is);
                Workbook targetWorkbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet templateSheet = templateWorkbook.getSheetAt(0);

            for (int i = 0; i < letras.size(); i++) {
                Map<String, Object> letra = letras.get(i);
                String nroLetra = String.valueOf(letra.getOrDefault("nro_letra", "Letra_" + (i + 1)))
                        .replaceAll("[^a-zA-Z0-9-_]", "_");
                String sheetName = WorkbookUtil.createSafeSheetName((i + 1) + "_" + nroLetra);

                Sheet newSheet = targetWorkbook.createSheet(sheetName);
                copySheet(templateSheet, newSheet);
                fillSheetWithLetra(newSheet, letra);
            }

            targetWorkbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel por lote de Letras: " + e.getMessage(), e);
        }
    }

    public String saveExcelToDisk(Map<String, Object> letra, String baseDir, boolean useSubfolders) {
        try {
            byte[] bytes = generateExcelBytes(letra);
            String nroLetra = (String) letra.getOrDefault("nro_letra", "LETRA");

            String targetDir = (baseDir != null && !baseDir.trim().isEmpty()) ? baseDir.trim()
                    : "C:\\Inplabel\\Letras_Excel";

            if (useSubfolders) {
                LocalDate now = LocalDate.now();
                targetDir = targetDir + File.separator + now.getYear() + File.separator
                        + String.format("%02d", now.getMonthValue());
            }

            File dir = new File(targetDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = nroLetra.replaceAll("[^a-zA-Z0-9-_]", "_") + ".xlsx";
            File targetFile = new File(dir, filename);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(bytes);
            }

            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Advertencia al guardar Excel de Letra en disco: " + e.getMessage());
            return null;
        }
    }

    private void fillSheetWithLetra(Sheet sheet, Map<String, Object> letra) {
        String nroLetra = String.valueOf(letra.getOrDefault("nro_letra", ""));
        String refGirador = String.valueOf(letra.getOrDefault("ref_girador", "-"));
        String lugarGiro = String.valueOf(letra.getOrDefault("lugar_giro", "LIMA")).toUpperCase();

        String fechaGiroStr = String.valueOf(letra.getOrDefault("fecha_giro", LocalDate.now().toString()));
        String[] giroParts = parseDateToParts(fechaGiroStr);

        String fechaVencStr = String
                .valueOf(letra.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(30).toString()));
        String[] vencParts = parseDateToParts(fechaVencStr);

        Object montoObj = letra.get("monto");
        double montoVal = 0.0;
        if (montoObj instanceof Number) {
            montoVal = ((Number) montoObj).doubleValue();
        } else if (montoObj != null) {
            try {
                montoVal = Double.parseDouble(montoObj.toString().trim().replace(",", "."));
            } catch (Exception ignored) {}
        }

        String moneda = String.valueOf(letra.getOrDefault("moneda", "SOLES")).toUpperCase();
        String symbol = moneda.contains("DOLAR") || moneda.contains("USD") ? "$" : "S/";
        String montoFormatted = String.format("%s %,.2f", symbol, montoVal).replace(',', 'X')
                .replace('.', ',').replace('X', '.');

        String montoLetras = String.valueOf(letra.getOrDefault("monto_letras", "")).toUpperCase();
        String cliente = String.valueOf(letra.getOrDefault("nombre_cliente", "CLIENTE")).toUpperCase();
        String ruc = String.valueOf(letra.getOrDefault("nro_documento", "-"));
        String direccion = String.valueOf(letra.getOrDefault("direccion_cliente", "LIMA - PERU")).toUpperCase();

        // 1. NÚMERO DE LETRA -> Celda B3 (Fila 2, Columna 1 en índice 0)
        setCellValue(sheet, 2, 1, nroLetra);

        // 2. REF. DEL GIRADOR -> Celda D3 (Fila 2, Columna 3)
        setCellValue(sheet, 2, 3, refGirador);

        // 3. LUGAR DE GIRO -> Celda F3 (Fila 2, Columna 5)
        setCellValue(sheet, 2, 5, lugarGiro);

        // 4. FECHA DE GIRO (Día: H4, Mes: I4, Año: J4) -> Fila 3, Columnas 7, 8, 9
        setCellValue(sheet, 3, 7, giroParts[0]);
        setCellValue(sheet, 3, 8, giroParts[1]);
        setCellValue(sheet, 3, 9, giroParts[2]);

        // 5. FECHA DE VENCIMIENTO (Día: K4, Mes: L4, Año: M4) -> Fila 3, Columnas 10,
        // 11, 12
        setCellValue(sheet, 3, 10, vencParts[0]);
        setCellValue(sheet, 3, 11, vencParts[1]);
        setCellValue(sheet, 3, 12, vencParts[2]);

        // 6. MONEDA E IMPORTE -> Celda N3 (Fila 2, Columna 13)
        setCellValue(sheet, 2, 13, montoFormatted);

        // 7. IMPORTE EN LETRAS -> Celda B6 (Fila 5, Columna 1)
        setCellValue(sheet, 5, 1, montoLetras);

        // 8. CLIENTE / RAZÓN SOCIAL -> C8 (Fila 7, Columna 2) y si hace falta espacio
        // C9 (Fila 8, Columna 2)
        if (cliente.length() > 48) {
            int splitIdx = findBestSplitIndex(cliente, 48);
            String line1 = cliente.substring(0, splitIdx).trim();
            String line2 = cliente.substring(splitIdx).trim();
            setCellValue(sheet, 7, 2, line1);
            setCellValue(sheet, 8, 2, line2);
        } else {
            setCellValue(sheet, 7, 2, cliente);
        }

        // 9. RUC DEL CLIENTE -> Celda C10 (Fila 9, Columna 2)
        setCellValue(sheet, 9, 2, ruc);

        // 10. DIRECCIÓN FISCAL -> C11 (Fila 10, Columna 2) y si hace falta espacio C12
        // (Fila 11, Columna 2)
        if (direccion.length() > 48) {
            int splitIdx = findBestSplitIndex(direccion, 48);
            String line1 = direccion.substring(0, splitIdx).trim();
            String line2 = direccion.substring(splitIdx).trim();
            setCellValue(sheet, 10, 2, line1);
            setCellValue(sheet, 11, 2, line2);
        } else {
            setCellValue(sheet, 10, 2, direccion);
        }
    }

    private int findBestSplitIndex(String text, int target) {
        if (text.length() <= target)
            return text.length();
        int lastSpace = text.lastIndexOf(' ', target);
        if (lastSpace > 15)
            return lastSpace;
        int lastDash = text.lastIndexOf('-', target);
        if (lastDash > 15)
            return lastDash + 1;
        return target;
    }

    private void setCellValue(Sheet sheet, int rowIndex, int colIndex, String value) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value != null ? value : "");
    }

    private String[] parseDateToParts(String dateStr) {
        try {
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                String clean = dateStr.trim().split("T")[0];
                String[] parts = clean.split("-");
                if (parts.length == 3) {
                    return new String[] {
                            String.format("%02d", Integer.parseInt(parts[2])),
                            String.format("%02d", Integer.parseInt(parts[1])),
                            parts[0]
                    };
                }
            }
        } catch (Exception ignored) {
        }
        LocalDate now = LocalDate.now();
        return new String[] {
                String.format("%02d", now.getDayOfMonth()),
                String.format("%02d", now.getMonthValue()),
                String.valueOf(now.getYear())
        };
    }

    private void copySheet(Sheet srcSheet, Sheet destSheet) {
        Workbook srcWorkbook = srcSheet.getWorkbook();
        Workbook destWorkbook = destSheet.getWorkbook();

        for (int r = 0; r <= srcSheet.getLastRowNum(); r++) {
            Row srcRow = srcSheet.getRow(r);
            if (srcRow != null) {
                Row destRow = destSheet.createRow(r);
                destRow.setHeight(srcRow.getHeight());

                for (int c = 0; c < srcRow.getLastCellNum(); c++) {
                    Cell srcCell = srcRow.getCell(c);
                    if (srcCell != null) {
                        Cell destCell = destRow.createCell(c);
                        copyCell(srcCell, destCell, destWorkbook);
                    }
                }
            }
        }

        for (int i = 0; i < srcSheet.getNumMergedRegions(); i++) {
            destSheet.addMergedRegion(srcSheet.getMergedRegion(i));
        }

        for (int c = 0; c < 20; c++) {
            destSheet.setColumnWidth(c, srcSheet.getColumnWidth(c));
        }
    }

    private void copyCell(Cell srcCell, Cell destCell, Workbook destWorkbook) {
        CellStyle newStyle = destWorkbook.createCellStyle();
        newStyle.cloneStyleFrom(srcCell.getCellStyle());
        destCell.setCellStyle(newStyle);

        switch (srcCell.getCellType()) {
            case STRING:
                destCell.setCellValue(srcCell.getStringCellValue());
                break;
            case NUMERIC:
                destCell.setCellValue(srcCell.getNumericCellValue());
                break;
            case BOOLEAN:
                destCell.setCellValue(srcCell.getBooleanCellValue());
                break;
            case FORMULA:
                destCell.setCellFormula(srcCell.getCellFormula());
                break;
            default:
                break;
        }
    }
}
