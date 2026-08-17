import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.Rectangle;

public class InspectPdf {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        System.out.println("Pages: " + reader.getNumberOfPages());
        Rectangle pageRect = reader.getPageSize(1);
        System.out.println("Page 1 Size: " + pageRect.getWidth() + " x " + pageRect.getHeight());
        System.out.println("Page 1 Rotation: " + reader.getPageRotation(1));
        reader.close();
    }
}
