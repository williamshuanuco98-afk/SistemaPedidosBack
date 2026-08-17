import com.lowagie.text.pdf.PdfReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindGridBoxes {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        byte[] bytes = reader.getPageContent(1);
        String s = new String(bytes);

        System.out.println("=== RECTANGLES AROUND Y=710-770 ===");
        Pattern p = Pattern.compile("([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+) re");
        Matcher m = p.matcher(s);
        while (m.find()) {
            float x = Float.parseFloat(m.group(1));
            float y = Float.parseFloat(m.group(2));
            float w = Float.parseFloat(m.group(3));
            float h = Float.parseFloat(m.group(4));
            if (y >= 700 && y <= 775) {
                System.out.println(String.format("RE: X=%.2f, Y=%.2f, W=%.2f, H=%.2f (Right=%.2f, Top=%.2f)", x, y, w, h, x + w, y + h));
            }
        }
        reader.close();
    }
}
