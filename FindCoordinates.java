import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

public class FindCoordinates {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        byte[] streamBytes = reader.getPageContent(1);
        String content = new String(streamBytes);
        
        // Print lines containing text matrix (Tm) or text positioning
        String[] lines = content.split("\\r?\\n");
        System.out.println("Total content lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Tj") || lines[i].contains("TJ") || lines[i].contains("Tm")) {
                System.out.println("Line " + i + ": " + lines[i]);
            }
        }
        reader.close();
    }
}
