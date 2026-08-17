import com.lowagie.text.pdf.PdfReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InspectLines {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        byte[] bytes = reader.getPageContent(1);
        String s = new String(bytes);

        System.out.println("=== RECTANGLES / LINES IN Plantilla excel.pdf ===");
        // Match: x y w h re
        Pattern pRe = Pattern.compile("([0-9.]+) ([0-9.]+) ([0-9.]+) ([0-9.]+) re");
        Matcher mRe = pRe.matcher(s);
        while (mRe.find()) {
            System.out.println("RE: x=" + mRe.group(1) + ", y=" + mRe.group(2) + ", w=" + mRe.group(3) + ", h=" + mRe.group(4));
        }

        // Match: x y m  x y l
        Pattern pLine = Pattern.compile("([0-9.]+) ([0-9.]+) m\\s+([0-9.]+) ([0-9.]+) l");
        Matcher mLine = pLine.matcher(s);
        while (mLine.find()) {
            System.out.println("LINE: from (" + mLine.group(1) + ", " + mLine.group(2) + ") to (" + mLine.group(3) + ", " + mLine.group(4) + ")");
        }
        reader.close();
    }
}
