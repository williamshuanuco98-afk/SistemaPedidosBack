package com.inplabel.pedidos.util;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LetraPdfGenerator {

    private static final int W = 1700;
    private static final int H = 960;

    private static final Color BG = Color.WHITE;
    private static final Color BLACK = new Color(20, 20, 20);
    private static final Color GREEN = new Color(0, 140, 90);
    private static final Color GRAY_LINE = new Color(60, 60, 60);

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
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>Letra de Cambio - " + nroLetra + "</title>\n" +
                "  <style>\n" +
                "    @page { size: portrait; margin: 10mm 10mm; }\n" +
                "    * { box-sizing: border-box; }\n" +
                "    body { font-family: Arial, Helvetica, sans-serif; margin: 0; padding: 0; color: #141414; font-size: 11px; background: #fff; }\n" +
                "    .letra-box { width: 100%; max-width: 740px; margin: 0 auto; }\n" +
                "    .top-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 8px; }\n" +
                "    .logo-block { display: flex; align-items: center; }\n" +
                "    .company-info { text-align: right; font-size: 9.5px; line-height: 1.35; color: #141414; }\n" +
                "    .company-info-line { border-top: 1.5px solid #3c3c3c; width: 260px; margin-bottom: 4px; margin-left: auto; }\n" +
                "    .main-container { display: flex; border: 1.5px solid #141414; }\n" +
                "    .left-clauses {\n" +
                "      width: 28px;\n" +
                "      min-width: 28px;\n" +
                "      padding: 6px 2px;\n" +
                "      border-right: 1.5px solid #141414;\n" +
                "      font-size: 6.8px;\n" +
                "      line-height: 1.2;\n" +
                "      writing-mode: vertical-rl;\n" +
                "      transform: rotate(180deg);\n" +
                "      text-align: left;\n" +
                "    }\n" +
                "    .right-content { flex: 1; display: flex; flex-direction: column; }\n" +
                "    .upper-wrapper { display: flex; }\n" +
                "    .aceptante-tag {\n" +
                "      width: 22px;\n" +
                "      min-width: 22px;\n" +
                "      border-right: 1.5px solid #141414;\n" +
                "      border-bottom: 1.5px solid #141414;\n" +
                "      writing-mode: vertical-rl;\n" +
                "      transform: rotate(180deg);\n" +
                "      text-align: center;\n" +
                "      font-size: 8.5px;\n" +
                "      font-weight: bold;\n" +
                "      padding: 4px 0;\n" +
                "    }\n" +
                "    .upper-main { flex: 1; display: flex; flex-direction: column; }\n" +
                "    .grid-header { width: 100%; border-collapse: collapse; text-align: center; }\n" +
                "    .grid-header th, .grid-header td { border: 1.5px solid #141414; padding: 3px 2px; }\n" +
                "    .grid-header th { font-size: 9px; font-weight: bold; background: #e2ebd8; }\n" +
                "    .grid-header td { font-size: 11.5px; font-weight: bold; height: 26px; }\n" +
                "    .date-cell-table { width: 100%; border-collapse: collapse; }\n" +
                "    .date-cell-table td { border: none; padding: 1px; font-size: 8.5px; background: #e2ebd8; }\n" +
                "    .date-cell-table tr.date-val td { border-top: 1.5px solid #141414; font-weight: bold; font-size: 11.5px; background: #fff; height: 26px; }\n" +
                "    .banner-text { font-size: 10.5px; padding: 4px 6px; }\n" +
                "    .amount-box { border: 1.5px solid #141414; padding: 5px 8px; font-weight: bold; font-size: 11px; margin: 0 4px 4px 4px; }\n" +
                "    .sub-text { font-size: 10px; padding: 2px 6px 4px 6px; border-bottom: 1.5px solid #141414; }\n" +
                "    .lower-grid { display: flex; }\n" +
                "    .lower-left { width: 56%; display: flex; flex-direction: column; border-right: 1.5px solid #141414; }\n" +
                "    .girado-info { padding: 6px 8px; font-size: 10.5px; line-height: 1.5; }\n" +
                "    .aval-wrapper { display: flex; border-top: 1.5px solid #141414; flex: 1; }\n" +
                "    .por-aval-tag {\n" +
                "      width: 22px;\n" +
                "      min-width: 22px;\n" +
                "      border-right: 1.5px solid #141414;\n" +
                "      writing-mode: vertical-rl;\n" +
                "      transform: rotate(180deg);\n" +
                "      text-align: center;\n" +
                "      font-size: 8.5px;\n" +
                "      font-weight: bold;\n" +
                "      padding: 4px 0;\n" +
                "    }\n" +
                "    .aval-info { flex: 1; padding: 6px 8px; font-size: 10px; line-height: 1.5; }\n" +
                "    .lower-right { width: 44%; padding: 5px 6px; font-size: 10px; text-align: center; display: flex; flex-direction: column; }\n" +
                "    .bank-debit-line { font-size: 9.5px; text-align: left; margin-bottom: 3px; }\n" +
                "    .bank-table { width: 100%; border-collapse: collapse; margin-bottom: 4px; font-size: 8.5px; }\n" +
                "    .bank-table th, .bank-table td { border: 1.5px solid #141414; padding: 2px; }\n" +
                "    .bank-table th { background: #e2ebd8; font-weight: bold; }\n" +
                "    .bank-table td { height: 18px; }\n" +
                "    .company-name { font-weight: bold; font-style: italic; color: #008c5a; font-size: 12px; margin-top: 4px; }\n" +
                "    .company-ruc { font-weight: bold; font-size: 11px; margin-bottom: 6px; }\n" +
                "    .signature-line { margin-top: 24px; border-bottom: 1.5px dotted #141414; width: 75%; margin-left: auto; margin-right: auto; }\n" +
                "    .signature-label { font-size: 9.5px; font-weight: bold; margin-top: 2px; }\n" +
                "    .signature-sub { font-size: 8.5px; color: #333; text-align: center; margin-top: 1px; }\n" +
                "    .doi-label { font-size: 9px; font-weight: bold; text-align: left; margin-top: 3px; padding-left: 4px; }\n" +
                "    .footer-line { border-top: 1.5px solid #141414; margin-top: 4px; }\n" +
                "    .footer-note { font-size: 9.5px; margin-top: 3px; text-align: left; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"letra-box\">\n" +
                "    <div class=\"top-header\">\n" +
                "      <div class=\"logo-block\">\n" +
                "        <svg width=\"46\" height=\"46\" viewBox=\"0 0 60 60\" style=\"margin-right: 8px;\">\n" +
                "          <circle cx=\"30\" cy=\"30\" r=\"17\" fill=\"none\" stroke=\"#008c5a\" stroke-width=\"4\"/>\n" +
                "          <line x1=\"30\" y1=\"9\" x2=\"30\" y2=\"4\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"30\" y1=\"51\" x2=\"30\" y2=\"56\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"9\" y1=\"30\" x2=\"4\" y2=\"30\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"51\" y1=\"30\" x2=\"56\" y2=\"30\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"15\" y1=\"15\" x2=\"11\" y2=\"11\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"45\" y1=\"45\" x2=\"49\" y2=\"49\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"15\" y1=\"45\" x2=\"11\" y2=\"49\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <line x1=\"45\" y1=\"15\" x2=\"49\" y2=\"11\" stroke=\"#008c5a\" stroke-width=\"4\" stroke-linecap=\"round\"/>\n" +
                "          <rect x=\"21\" y=\"24\" width=\"5\" height=\"12\" rx=\"1.5\" fill=\"#008c5a\"/>\n" +
                "          <rect x=\"28\" y=\"21\" width=\"5\" height=\"15\" rx=\"1.5\" fill=\"#008c5a\"/>\n" +
                "          <rect x=\"34\" y=\"26\" width=\"4\" height=\"10\" rx=\"1.5\" fill=\"#008c5a\"/>\n" +
                "        </svg>\n" +
                "        <div>\n" +
                "          <div style=\"font-size: 24px; font-weight: bold; line-height: 1; color: #1e1e1e;\">Inplabel</div>\n" +
                "          <div style=\"font-size: 9.5px; font-weight: bold; color: #008c5a; margin-top: 2px;\">Industrias plasticos belsa S.A.C.</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div class=\"company-info\">\n" +
                "        <div class=\"company-info-line\"></div>\n" +
                "        <div><strong>Av. María Parado de Bellido Lte. 5</strong></div>\n" +
                "        <div>Lotización Chacra Cerro - Comas - Lima - Lima</div>\n" +
                "        <div>Telf.: (01)557-1526 Claro: 975 564 460 / 983 518 504</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"main-container\">\n" +
                "      <div class=\"left-clauses\">\n" +
                "        <strong>CLÁUSULAS ESPECIALES:</strong> (1) En caso de mora, esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor. (2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale, sin que sea necesario la intervención del obligado principal ni de los solidarios. (3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias. (4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima.\n" +
                "      </div>\n" +
                "      <div class=\"right-content\">\n" +
                "        <div class=\"upper-wrapper\">\n" +
                "          <div class=\"aceptante-tag\">Aceptante(s)</div>\n" +
                "          <div class=\"upper-main\">\n" +
                "            <table class=\"grid-header\">\n" +
                "              <tr>\n" +
                "                <th style=\"width: 17%;\">NUMERO DE LETRA</th>\n" +
                "                <th style=\"width: 17%;\">REF. DEL GIRADOR</th>\n" +
                "                <th style=\"width: 14%;\">LUGAR DE GIRO</th>\n" +
                "                <th style=\"width: 18%;\">FECHA DE GIRO</th>\n" +
                "                <th style=\"width: 18%;\">FECHA DE VENCIMIENTO</th>\n" +
                "                <th style=\"width: 16%;\">MONEDA E IMPORTE</th>\n" +
                "              </tr>\n" +
                "              <tr>\n" +
                "                <td>" + nroLetra + "</td>\n" +
                "                <td>" + refGirador + "</td>\n" +
                "                <td>" + lugarGiro + "</td>\n" +
                "                <td style=\"padding:0;\">\n" +
                "                  <table class=\"date-cell-table\">\n" +
                "                    <tr><td>DIA</td><td>MES</td><td>AÑO</td></tr>\n" +
                "                    <tr class=\"date-val\"><td>" + fgVals[0] + "</td><td>" + fgVals[1] + "</td><td>" + fgVals[2] + "</td></tr>\n" +
                "                  </table>\n" +
                "                </td>\n" +
                "                <td style=\"padding:0;\">\n" +
                "                  <table class=\"date-cell-table\">\n" +
                "                    <tr><td>DIA</td><td>MES</td><td>AÑO</td></tr>\n" +
                "                    <tr class=\"date-val\"><td>" + fvVals[0] + "</td><td>" + fvVals[1] + "</td><td>" + fvVals[2] + "</td></tr>\n" +
                "                  </table>\n" +
                "                </td>\n" +
                "                <td>" + montoFormatted + "</td>\n" +
                "              </tr>\n" +
                "            </table>\n" +
                "            <div class=\"banner-text\">\n" +
                "              Por esta <strong>LETRA DE CAMBIO</strong>, se servirá(n) pagar a la orden de <strong style=\"color: #008c5a; font-style: italic;\">INDUSTRIAS PLASTICOS BELSA S.A.C.</strong> la cantidad de:\n" +
                "            </div>\n" +
                "            <div class=\"amount-box\">" + montoLetras + "</div>\n" +
                "            <div class=\"sub-text\">\n" +
                "              Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:\n" +
                "            </div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <div class=\"lower-grid\">\n" +
                "          <div class=\"lower-left\">\n" +
                "            <div class=\"girado-info\">\n" +
                "              <div><strong>GIRADO A:</strong> <strong>" + cliente + "</strong></div>\n" +
                "              <div style=\"margin-top: 6px;\"><strong>RUC:</strong> " + ruc + "</div>\n" +
                "              <div style=\"margin-top: 4px;\"><strong>DIRECCION:</strong> " + direccion + "</div>\n" +
                "            </div>\n" +
                "            <div class=\"aval-wrapper\">\n" +
                "              <div class=\"por-aval-tag\">Por Aval</div>\n" +
                "              <div class=\"aval-info\">\n" +
                "                <div><strong>AVALISTA:</strong> .....................................................................................</div>\n" +
                "                <div style=\"margin-top: 6px;\">\n" +
                "                  <span><strong>D.I/R.U.C:</strong> ................................</span>\n" +
                "                  <span style=\"margin-left: 15px;\"><strong>TELEFONO:</strong> ..........................</span>\n" +
                "                </div>\n" +
                "                <div style=\"margin-top: 6px;\"><strong>DIRECCION:</strong> ....................................................................................</div>\n" +
                "              </div>\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class=\"lower-right\">\n" +
                "            <div class=\"bank-debit-line\">Importe a debitar en cuenta del Aceptante del Banco:....................................</div>\n" +
                "            <table class=\"bank-table\">\n" +
                "              <tr><th>BANCO</th><th>OFICINA</th><th>NUMERO DE CUENTA</th><th>D.C.</th></tr>\n" +
                "              <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n" +
                "            </table>\n" +
                "            <div class=\"company-name\">INDUSTRIAS PLASTICOS BELSA S.A.C.</div>\n" +
                "            <div class=\"company-ruc\">RUC: 20544368827</div>\n" +
                "            <div class=\"signature-line\"></div>\n" +
                "            <div class=\"signature-label\">FIRMA</div>\n" +
                "            <div class=\"signature-sub\">Nombre del representante(S)</div>\n" +
                "            <div class=\"doi-label\">D.O.I</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"footer-line\"></div>\n" +
                "    <div class=\"footer-note\">No escribir ni firmar debajo de esta linea</div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    public byte[] generatePdfBytes(Map<String, Object> letra) {
        try {
            BufferedImage img = generateLetraImage(letra);

            ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", imgBaos);
            byte[] imgBytes = imgBaos.toByteArray();

            // A4 Portrait format (595.28 x 841.89 pt) - 1 Sola Página garantizada
            Document doc = new Document(PageSize.A4, 20, 20, 20, 20);
            ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, pdfBaos);
            doc.open();

            Image pdfImg = Image.getInstance(imgBytes);
            pdfImg.scaleToFit(555f, 380f);
            pdfImg.setAlignment(Image.ALIGN_CENTER);
            doc.add(pdfImg);

            doc.close();
            return pdfBaos.toByteArray();
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

    public BufferedImage generateLetraImage(Map<String, Object> letra) {
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

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Antialiasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Fill background
        g.setColor(BG);
        g.fillRect(0, 0, W, H);

        int marginR = W - 65; // 1635

        // Fonts
        Font fLogo = new Font("Arial", Font.BOLD, 40);
        Font fLogoSub = new Font("Arial", Font.BOLD, 13);
        Font fAddr = new Font("Arial", Font.PLAIN, 15);
        Font fLabel = new Font("Arial", Font.BOLD, 15);
        Font fLabelSm = new Font("Arial", Font.BOLD, 13);
        Font fVal = new Font("Arial", Font.BOLD, 22);
        Font fValSm = new Font("Arial", Font.BOLD, 17);
        Font fText = new Font("Arial", Font.PLAIN, 16);
        Font fTextBold = new Font("Arial", Font.BOLD, 16);
        Font fTextBoldItalic = new Font("Arial", Font.BOLD | Font.ITALIC, 17);
        Font fAmount = new Font("Arial", Font.BOLD, 26);
        Font fVerticalLabel = new Font("Arial", Font.BOLD, 13);
        Font fSmall = new Font("Arial", Font.PLAIN, 13);
        Font fDotted = new Font("Arial", Font.PLAIN, 14);

        // ============================================================
        // 3. ENCABEZADO: LOGO Y DATOS DE LA EMPRESA
        // ============================================================
        int logoX = 265;
        int logoY = 65;

        // Gear icon
        g.setColor(GREEN);
        g.setStroke(new BasicStroke(5f));
        g.drawOval(logoX, logoY, 60, 60);

        int cx = logoX + 30;
        int cy = logoY + 30;
        int r = 34;
        for (int i = 0; i < 8; i++) {
            double ang = i * (360.0 / 8.0);
            double rad = Math.toRadians(ang);
            int x1 = (int) (cx + r * Math.cos(rad));
            int y1 = (int) (cy + r * Math.sin(rad));
            int x2 = (int) (cx + (r + 8) * Math.cos(rad));
            int y2 = (int) (cy + (r + 8) * Math.sin(rad));
            g.drawLine(x1, y1, x2, y2);
        }

        // Bottles inside
        g.fill(new RoundRectangle2D.Float(cx - 14, cy - 6, 10, 24, 3, 3));
        g.fill(new RoundRectangle2D.Float(cx, cy - 12, 10, 30, 3, 3));
        g.fill(new RoundRectangle2D.Float(cx + 12, cy - 2, 8, 20, 3, 3));

        g.setColor(new Color(30, 30, 30));
        g.setFont(fLogo);
        g.drawString("Inplabel", logoX + 75, logoY + 35);

        g.setColor(GREEN);
        g.setFont(fLogoSub);
        g.drawString("Industrias plasticos belsa S.A.C.", logoX + 78, logoY + 58);

        // Address Right
        g.setColor(GRAY_LINE);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(1200, 60, marginR, 60);

        g.setColor(BLACK);
        g.setFont(fAddr);
        String[] addrLines = {
                "Av. María Parado de Bellido Lte. 5",
                "Lotización Chacra Cerro - Comas - Lima - Lima",
                "Telf.: (01)557-1526 Claro: 975 564 460 / 983 518 504"
        };
        int addrY = 82;
        for (String line : addrLines) {
            int tw = g.getFontMetrics().stringWidth(line);
            g.drawString(line, marginR - tw, addrY);
            addrY += 22;
        }

        // ============================================================
        // 4. TABLA SUPERIOR
        // ============================================================
        int topTableY0 = 210;
        int tableX0 = 155;

        int c1 = tableX0;
        int c2 = tableX0 + 225;
        int c3 = tableX0 + 425;
        int fg0 = tableX0 + 600;
        int fgW = 105;
        int fv0 = fg0 + fgW * 3;
        int mi0 = fv0 + fgW * 3;

        int headerH = 40;
        int subH = 40;
        int valH = 65;

        int topTableY1 = topTableY0 + headerH + subH;
        int valuesY0 = topTableY1;
        int valuesY1 = valuesY0 + valH;

        g.setColor(BLACK);
        g.setStroke(new BasicStroke(2f));

        // Outer box
        g.drawRect(tableX0, topTableY0, marginR - tableX0, valuesY1 - topTableY0);

        // Tall headers
        drawCell(g, c1, topTableY0, c2 - c1, topTableY1 - topTableY0);
        drawCenteredText(g, "NUMERO DE LETRA", c1, c2, topTableY0 + (headerH + subH) / 2 + 5, fLabel);

        drawCell(g, c2, topTableY0, c3 - c2, topTableY1 - topTableY0);
        drawCenteredText(g, "REF. DEL GIRADOR", c2, c3, topTableY0 + (headerH + subH) / 2 + 5, fLabel);

        drawCell(g, c3, topTableY0, fg0 - c3, topTableY1 - topTableY0);
        drawCenteredText(g, "LUGAR DE GIRO", c3, fg0, topTableY0 + (headerH + subH) / 2 + 5, fLabel);

        drawCell(g, mi0, topTableY0, marginR - mi0, topTableY1 - topTableY0);
        drawCenteredText(g, "MONEDA E IMPORTE", mi0, marginR, topTableY0 + (headerH + subH) / 2 + 5, fLabelSm);

        // Fecha de Giro & Fecha de Vencimiento
        drawCell(g, fg0, topTableY0, fv0 - fg0, headerH);
        drawCenteredText(g, "FECHA DE GIRO", fg0, fv0, topTableY0 + 26, fLabel);

        drawCell(g, fv0, topTableY0, mi0 - fv0, headerH);
        drawCenteredText(g, "FECHA DE VENCIMIENTO", fv0, mi0, topTableY0 + 26, fLabel);

        // Sub labels DIA, MES, AÑO
        int[][] subGiro = {{fg0, fg0 + fgW}, {fg0 + fgW, fg0 + 2 * fgW}, {fg0 + 2 * fgW, fv0}};
        int[][] subVenc = {{fv0, fv0 + fgW}, {fv0 + fgW, fv0 + 2 * fgW}, {fv0 + 2 * fgW, mi0}};
        String[] subNames = {"DIA", "MES", "AÑO"};

        for (int i = 0; i < 3; i++) {
            drawCell(g, subGiro[i][0], topTableY0 + headerH, subGiro[i][1] - subGiro[i][0], subH);
            drawCenteredText(g, subNames[i], subGiro[i][0], subGiro[i][1], topTableY0 + headerH + 26, fLabelSm);

            drawCell(g, subVenc[i][0], topTableY0 + headerH, subVenc[i][1] - subVenc[i][0], subH);
            drawCenteredText(g, subNames[i], subVenc[i][0], subVenc[i][1], topTableY0 + headerH + 26, fLabelSm);
        }

        // Values Row
        drawCell(g, c1, valuesY0, c2 - c1, valH);
        drawCenteredText(g, nroLetra, c1, c2, valuesY0 + 40, fVal);

        drawCell(g, c2, valuesY0, c3 - c2, valH);
        drawCenteredText(g, refGirador, c2, c3, valuesY0 + 40, fVal);

        drawCell(g, c3, valuesY0, fg0 - c3, valH);
        drawCenteredText(g, lugarGiro, c3, fg0, valuesY0 + 40, fVal);

        for (int i = 0; i < 3; i++) {
            drawCell(g, subGiro[i][0], valuesY0, subGiro[i][1] - subGiro[i][0], valH);
            drawCenteredText(g, fgVals[i], subGiro[i][0], subGiro[i][1], valuesY0 + 40, fVal);

            drawCell(g, subVenc[i][0], valuesY0, subVenc[i][1] - subVenc[i][0], valH);
            drawCenteredText(g, fvVals[i], subVenc[i][0], subVenc[i][1], valuesY0 + 40, fVal);
        }

        drawCell(g, mi0, valuesY0, marginR - mi0, valH);
        drawCenteredText(g, montoFormatted, mi0, marginR, valuesY0 + 42, fAmount);

        // Aceptante(s) vertical label
        drawVerticalText(g, tableX0 - 40, topTableY0, valuesY1, "Aceptante(s)", fVerticalLabel, BLACK);
        g.drawLine(tableX0 - 5, topTableY0, tableX0 - 5, valuesY1);

        // ============================================================
        // 5. BANNER: Por esta LETRA DE CAMBIO...
        // ============================================================
        int yText = valuesY1 + 24;
        int xText = tableX0;

        g.setColor(BLACK);
        g.setFont(fText);
        g.drawString("Por esta ", xText, yText);
        xText += g.getFontMetrics().stringWidth("Por esta ");

        g.setFont(fTextBold);
        g.drawString("LETRA DE CAMBIO", xText, yText);
        xText += g.getFontMetrics().stringWidth("LETRA DE CAMBIO");

        g.setFont(fText);
        g.drawString(", se serivá(n) pagar a la orden de ", xText, yText);
        xText += g.getFontMetrics().stringWidth(", se serivá(n) pagar a la orden de ");

        g.setColor(GREEN);
        g.setFont(fTextBoldItalic);
        g.drawString("INDUSTRIAS PLASTICOS BELSA S.A.C.", xText, yText);
        xText += g.getFontMetrics().stringWidth("INDUSTRIAS PLASTICOS BELSA S.A.C.");

        g.setColor(BLACK);
        g.setFont(fText);
        g.drawString(" la cantidad de:", xText, yText);

        // ============================================================
        // 6. CAJA CANTIDAD EN LETRAS
        // ============================================================
        int y2 = yText + 12;
        int boxH = 55;
        drawCell(g, tableX0, y2, marginR - tableX0, boxH);

        g.setFont(fValSm);
        g.drawString(montoLetras, tableX0 + 15, y2 + 35);

        // ============================================================
        // 7. "Valor que sentará(n)..."
        // ============================================================
        int y3 = y2 + boxH + 24;
        g.setFont(fText);
        g.drawString("Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:", tableX0, y3);

        // ============================================================
        // 8. TABLA INFERIOR: GIRADO A / BANCO
        // ============================================================
        int botY0 = y3 + 12;
        int botY1 = botY0 + 340;
        int midX = tableX0 + 950;

        drawCell(g, tableX0, botY0, marginR - tableX0, botY1 - botY0);
        g.drawLine(midX, botY0, midX, botY1);

        // Por Aval vertical label
        drawVerticalText(g, tableX0 - 40, botY0, botY1, "Por Aval", fVerticalLabel, BLACK);
        g.drawLine(tableX0 - 5, botY0, tableX0 - 5, botY1);

        // Left Side: Client
        int lx = tableX0 + 20;
        int ly = botY0 + 32;
        g.setFont(fTextBold);
        g.drawString("GIRADO A:", lx, ly);
        g.drawString(cliente, lx + 150, ly);

        int ly2 = ly + 40;
        g.drawString("RUC:", lx, ly2);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString(ruc, lx + 150, ly2);

        int ly3 = ly2 + 32;
        g.setFont(fTextBold);
        g.drawString("DIRECCION:", lx, ly3);
        g.setFont(fText);
        g.drawString(direccion, lx + 150, ly3);

        // Divider
        int divY = ly3 + 25;
        g.drawLine(tableX0, divY, midX, divY);

        // Avalista
        int avy = divY + 32;
        g.setFont(fTextBold);
        g.drawString("AVALISTA:", lx, avy);
        g.setFont(fDotted);
        g.drawString(".....................................................................................................", lx + 150, avy);

        int avy2 = avy + 40;
        g.setFont(fTextBold);
        g.drawString("D.I/R.U.C:", lx, avy2);
        g.setFont(fDotted);
        g.drawString("....................................", lx + 120, avy2);

        g.setFont(fTextBold);
        g.drawString("TELEFONO:", lx + 330, avy2);
        g.setFont(fDotted);
        g.drawString("......................", lx + 460, avy2);

        int avy3 = avy2 + 35;
        g.setFont(fTextBold);
        g.drawString("DIRECCION:", lx, avy3);
        g.setFont(fDotted);
        g.drawString(".....................................................................................................", lx + 150, avy3);

        // Right Side: Bank
        int rx0 = midX;
        int rx1 = marginR;
        g.setFont(fSmall);
        g.drawString("Importe a debitar en cuenta del Aceptante del Banco:....................................", rx0 + 15, botY0 + 25);

        int subTableY0 = botY0 + 40;
        int subTableY1 = subTableY0 + 35;
        int[] bEdges = {rx0, rx0 + 90, rx0 + 190, rx0 + 400, rx1};
        String[] bLabels = {"BANCO", "OFICINA", "NUMERO DE CUENTA", "D.C."};

        for (int i = 0; i < 4; i++) {
            drawCell(g, bEdges[i], subTableY0, bEdges[i + 1] - bEdges[i], subTableY1 - subTableY0);
            drawCenteredText(g, bLabels[i], bEdges[i], bEdges[i + 1], subTableY0 + 23, fLabelSm);
        }

        int valRowY0 = subTableY1;
        int valRowY1 = valRowY0 + 40;
        for (int i = 0; i < 4; i++) {
            drawCell(g, bEdges[i], valRowY0, bEdges[i + 1] - bEdges[i], valRowY1 - valRowY0);
        }

        // Beneficiary Name + RUC
        int benY = valRowY1 + 35;
        drawCenteredText(g, "INDUSTRIAS PLASTICOS BELSA S.A.C.", rx0, rx1, benY, fTextBoldItalic, GREEN);
        drawCenteredText(g, "RUC: 20544368827", rx0, rx1, benY + 28, new Font("Arial", Font.BOLD, 17), BLACK);

        // Signature Line
        int firmaY = benY + 80;
        drawCenteredText(g, "............................................................", rx0 + 60, rx1 - 20, firmaY, fDotted, BLACK);
        drawCenteredText(g, "FIRMA", rx0, rx1, firmaY + 22, fTextBold, BLACK);
        drawCenteredText(g, "Nombre del representante(S)", rx0, rx1, firmaY + 45, fText, BLACK);
        g.setFont(fTextBold);
        g.drawString("D.O.I", rx0 + 15, firmaY + 70);

        // ============================================================
        // 9. CLAUSULAS ESPECIALES (texto vertical lateral izquierdo)
        // ============================================================
        int clauseX = 12;
        int clauseY0 = topTableY0;
        int clauseY1 = botY1;
        int availH = clauseY1 - clauseY0;

        String fullClauses = "CLÁUSULAS ESPECIALES: (1) En caso de mora, esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor.   (2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale, sin que sea necesario la intervención del obligado principal ni de los solidarios.   (3) Las partes acuerdan consignar la cláusula \"sin protesto\" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias.   (4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima.";

        Font fClause = new Font("Arial", Font.BOLD, 10);
        Font fClauseReg = new Font("Arial", Font.PLAIN, 10);

        List<String> lines = wrapText(g, fullClauses, fClauseReg, availH - 20);
        int lineH = 14;
        int blockW = availH;
        int blockH = lineH * lines.size() + 10;

        BufferedImage tmp = new BufferedImage(blockW, blockH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tmp.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int curY = lineH;
        for (int i = 0; i < lines.size(); i++) {
            tg.setFont(i == 0 ? fClause : fClauseReg);
            tg.setColor(BLACK);
            tg.drawString(lines.get(i), 0, curY);
            curY += lineH;
        }
        tg.dispose();

        BufferedImage rotated = rotate90CCW(tmp);
        g.drawImage(rotated, clauseX, clauseY0, null);

        // ============================================================
        // 10. LINEA INFERIOR
        // ============================================================
        int lineY = botY1 + 25;
        g.drawLine(tableX0, lineY, marginR, lineY);
        g.setFont(fText);
        g.drawString("No escribir ni firmar debajo de esta linea", tableX0, lineY + 20);

        g.dispose();
        return img;
    }

    private static void drawCell(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(BLACK);
        g.drawRect(x, y, w, h);
    }

    private static void drawCenteredText(Graphics2D g, String text, int x0, int x1, int y, Font font) {
        drawCenteredText(g, text, x0, x1, y, font, BLACK);
    }

    private static void drawCenteredText(Graphics2D g, String text, int x0, int x1, int y, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        int tw = g.getFontMetrics().stringWidth(text);
        int x = x0 + (x1 - x0 - tw) / 2;
        g.drawString(text, x, y);
    }

    private static void drawVerticalText(Graphics2D g, int x, int yTop, int yBottom, String text, Font font, Color color) {
        FontMetrics fm = g.getFontMetrics(font);
        int tw = fm.stringWidth(text);
        int th = fm.getHeight();

        BufferedImage tmp = new BufferedImage(tw + 10, th + 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tmp.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        tg.setFont(font);
        tg.setColor(color);
        tg.drawString(text, 5, th - 2);
        tg.dispose();

        BufferedImage rot = rotate90CCW(tmp);
        int rh = rot.getHeight();
        int y = yTop + ((yBottom - yTop) - rh) / 2;
        g.drawImage(rot, x, y, null);
    }

    private static BufferedImage rotate90CCW(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dest = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.translate(0, w);
        g2.rotate(-Math.PI / 2);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return dest;
    }

    private static List<String> wrapText(Graphics2D g, String text, Font font, int maxWidth) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        for (String word : words) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(test) <= maxWidth) {
                cur.append(cur.length() == 0 ? "" : " ").append(word);
            } else {
                if (cur.length() > 0) lines.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
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
