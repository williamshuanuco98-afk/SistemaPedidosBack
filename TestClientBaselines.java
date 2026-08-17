import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.FileOutputStream;

public class TestClientBaselines {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        FileOutputStream fos = new FileOutputStream("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/test_baselines.pdf");
        PdfStamper stamper = new PdfStamper(reader, fos);
        PdfContentByte cb = stamper.getOverContent(1);

        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        cb.beginText();
        cb.setColorFill(Color.RED);
        cb.setFontAndSize(bf, 6f);

        for (float y = 605; y <= 685; y += 5) {
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "--- Y=" + y + " ---", 140f, y, 0);
        }

        cb.endText();
        stamper.close();
        reader.close();
        System.out.println("Baselines test generated!");
    }
}
