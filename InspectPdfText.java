import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

public class InspectPdfText {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String text = extractor.getTextFromPage(1);
        System.out.println("=== EXTRACTED TEXT FROM Plantilla excel.pdf ===");
        System.out.println(text);
        reader.close();
    }
}
