import com.lowagie.text.pdf.*;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import java.awt.Color;
import java.io.FileOutputStream;

public class TestStampPdf {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        FileOutputStream fos = new FileOutputStream("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/test_stamped.pdf");
        PdfStamper stamper = new PdfStamper(reader, fos);
        PdfContentByte cb = stamper.getOverContent(1);

        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        BaseFont bfNorm = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        // Let's test coordinates
        cb.beginText();
        cb.setColorFill(Color.RED);
        cb.setFontAndSize(bf, 10f);

        // Let's print a grid of markers every 50pt to find exact coordinates
        for (int x = 50; x <= 550; x += 50) {
            for (int y = 500; y <= 800; y += 50) {
                cb.showTextAligned(PdfContentByte.ALIGN_CENTER, x + "," + y, x, y, 0);
            }
        }

        cb.endText();
        stamper.close();
        reader.close();
        System.out.println("Stamped test generated successfully!");
    }
}
