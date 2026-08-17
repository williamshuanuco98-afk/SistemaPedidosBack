import com.lowagie.text.pdf.PdfReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;

public class FindVerticalLines {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        byte[] bytes = reader.getPageContent(1);
        String s = new String(bytes);

        System.out.println("=== VERTICAL LINES AROUND Y=730-760 ===");
        // Look for lines: x y m x y l
        Pattern p = Pattern.compile("([0-9.]+) ([0-9.]+) m\\s+([0-9.]+) ([0-9.]+) l");
        Matcher m = p.matcher(s);
        ArrayList<Float> vLines = new ArrayList<>();
        while (m.find()) {
            float x1 = Float.parseFloat(m.group(1));
            float y1 = Float.parseFloat(m.group(2));
            float x2 = Float.parseFloat(m.group(3));
            float y2 = Float.parseFloat(m.group(4));
            if (Math.abs(x1 - x2) < 0.5 && ((y1 >= 710 && y1 <= 770) || (y2 >= 710 && y2 <= 770))) {
                System.out.println(String.format("V-LINE at X=%.2f from Y=%.2f to Y=%.2f", x1, y1, y2));
                if (!vLines.contains(x1)) vLines.add(x1);
            }
        }
        Collections.sort(vLines);
        System.out.println("Unique X positions: " + vLines);
        reader.close();
    }
}
