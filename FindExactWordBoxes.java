import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

public class FindExactWordBoxes {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        
        // Let's inspect stream bytes by parsing text blocks
        byte[] bytes = reader.getPageContent(1);
        String s = new String(bytes);
        String[] tokens = s.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("Tm")) {
                // Preceding 6 tokens are: a b c d e f Tm
                if (i >= 6) {
                    float a = Float.parseFloat(tokens[i - 6]);
                    float b = Float.parseFloat(tokens[i - 5]);
                    float c = Float.parseFloat(tokens[i - 4]);
                    float d = Float.parseFloat(tokens[i - 3]);
                    float e = Float.parseFloat(tokens[i - 2]);
                    float f = Float.parseFloat(tokens[i - 1]);
                    // If around the lower section y=500 to 700
                    if (f >= 530 && f <= 700 && e < 150) {
                        // Check following token for text
                        String next = (i + 1 < tokens.length) ? tokens[i + 1] : "";
                        String next2 = (i + 2 < tokens.length) ? tokens[i + 2] : "";
                        System.out.println(String.format("TEXT at X=%.2f, Y=%.2f -> next: %s %s", e, f, next, next2));
                    }
                }
            }
        }
        reader.close();
    }
}
