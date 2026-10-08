package com.inplabel.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class PedidosApplication {

    public static void main(String[] args) {
        // En Windows sin cabezal gráfico para evitar problemas con java.awt.headless
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(PedidosApplication.class, args);
        System.out.println(" Backend Spring Boot listo en http://localhost:8080");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserOnStartup() {
        System.out.println("🚀 Abriendo navegador en http://localhost:8080...");
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String url = "http://localhost:8080";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            System.err.println("No se pudo abrir el navegador automáticamente: " + e.getMessage());
        }
    }
}
