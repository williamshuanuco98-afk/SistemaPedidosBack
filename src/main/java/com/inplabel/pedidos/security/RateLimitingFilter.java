package com.inplabel.pedidos.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    // Límite: Máximo 2000 peticiones por minuto por dirección IP
    private static final int MAX_REQUESTS_PER_MINUTE = 2000;
    private static final long WINDOW_DURATION_MS = 60_000L;

    private static class RequestTracker {
        long windowStartTime;
        AtomicInteger count;

        RequestTracker(long now) {
            this.windowStartTime = now;
            this.count = new AtomicInteger(1);
        }
    }

    private final ConcurrentHashMap<String, RequestTracker> ipTracker = new ConcurrentHashMap<>();
    private long lastCleanupTime = System.currentTimeMillis();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Agregar cabeceras de seguridad y control de caché (Prevenir almacenamiento en caché obsoleto)
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setHeader("Expires", "0");

        // Excluir pre-flight OPTIONS y todos los recursos estáticos (CSS, JS, imágenes, HTML) del rate limiter
        String path = httpRequest.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod()) || !path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(httpRequest);
        long now = System.currentTimeMillis();

        // Limpieza periódica de IPs inactivas cada 5 minutos para evitar fugas de memoria
        if (now - lastCleanupTime > 300_000L) {
            cleanupExpiredIps(now);
        }

        RequestTracker tracker = ipTracker.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.windowStartTime) > WINDOW_DURATION_MS) {
                return new RequestTracker(now);
            }
            current.count.incrementAndGet();
            return current;
        });

        int currentCount = tracker.count.get();
        long resetSeconds = Math.max(1, (WINDOW_DURATION_MS - (now - tracker.windowStartTime)) / 1000);

        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

        // Si excede el límite permitido en la ventana de tiempo
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            httpResponse.setStatus(429); // 429 Too Many Requests
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.setHeader("Retry-After", String.valueOf(resetSeconds));
            httpResponse.getWriter().write(String.format(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Límite de peticiones excedido (máx %d por minuto). Por favor intente en %d segundos.\"}",
                    MAX_REQUESTS_PER_MINUTE, resetSeconds
            ));
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupExpiredIps(long now) {
        lastCleanupTime = now;
        ipTracker.entrySet().removeIf(entry -> (now - entry.getValue().windowStartTime) > WINDOW_DURATION_MS * 2);
    }
}
