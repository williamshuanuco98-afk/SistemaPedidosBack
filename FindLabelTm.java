import com.lowagie.text.pdf.PdfReader;

public class FindLabelTm {
    public static void main(String[] args) throws Exception {
        PdfReader reader = new PdfReader("c:/Users/User/OneDrive/Escritorio/Proyectos/SistemaWebPedidosBack/Plantilla excel.pdf");
        byte[] bytes = reader.getPageContent(1);
        String s = new String(bytes);

        String[] lines = s.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Girado") || lines[i].contains("RUC") || lines[i].contains("DIRECCION") || lines[i].contains("AVALISTA") || lines[i].contains("TELEFONO")) {
                System.out.println("FOUND AT LINE " + i + ": " + lines[i]);
                for (int j = Math.max(0, i - 5); j <= Math.min(lines.length - 1, i + 2); j++) {
                    System.out.println("  [" + j + "] " + lines[j]);
                }
            }
        }
        reader.close();
    }
}
