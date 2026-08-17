import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.FileOutputStream;

public class TestExactCoordinates {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        FileOutputStream fos = new FileOutputStream("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/test_letra_overlay.pdf");
        PdfStamper stamper = new PdfStamper(reader, fos);
        PdfContentByte cb = stamper.getOverContent(1);

        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        BaseFont bfNorm = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        cb.beginText();
        cb.setColorFill(new Color(20, 20, 20));

        // 1. Nro Letra (Col 1)
        cb.setFontAndSize(bfBold, 11f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "226-2026", 132f, 735f, 0);

        // 2. Ref Girador (Col 2)
        cb.setFontAndSize(bfBold, 11f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "FF02 - 200026", 212f, 735f, 0);

        // 3. Lugar Giro (Col 3)
        cb.setFontAndSize(bfBold, 11f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "LIMA", 280f, 735f, 0);

        // 4. Fecha Giro (DIA, MES, AÑO)
        cb.setFontAndSize(bfBold, 9.5f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "15", 336f, 735f, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "08", 358f, 735f, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "2026", 382f, 735f, 0);

        // 5. Fecha Vencimiento (DIA, MES, AÑO)
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "14", 416f, 735f, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "09", 438f, 735f, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "2026", 462f, 735f, 0);

        // 6. Moneda e Importe
        cb.setFontAndSize(bfBold, 12.5f);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "S/ 3,083.00", 524f, 735f, 0);

        // 7. Monto en letras (Row 6)
        cb.setFontAndSize(bfBold, 9.2f);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "TRES MIL OCHENTA Y TRES CON 00 / 100 SOLES", 102f, 703f, 0);

        // 8. Cliente / Girado a
        cb.setFontAndSize(bfBold, 8.2f);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "CHEMIFABRIK PERU SOCIEDAD ANONIMA CERRADA - CHEMIFABRIK", 145f, 650f, 0);

        // 9. RUC
        cb.setFontAndSize(bfNorm, 8.0f);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "20252814036", 145f, 634f, 0);

        // 10. Dirección
        cb.setFontAndSize(bfNorm, 7.5f);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "CAL. GILBERTO ESPINOZA N.120 M NRO 120 URB. LOS FICUS - LIMA - LIMA - SANTA ANITA", 145f, 620f, 0);

        cb.endText();
        stamper.close();
        reader.close();
        System.out.println("Test stamped successfully!");
    }
}
