package com.inplabel.pedidos.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Applies to JSON/base64 too, including chunked requests without Content-Length. */
@Component
@Order(3)
public class RequestSizeFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/") ||
                !java.util.Set.of("POST", "PUT", "PATCH").contains(request.getMethod())) {
            chain.doFilter(req, res); return;
        }
        int limit = request.getRequestURI().endsWith("/auth/login") ? 8192 : 15 * 1024 * 1024;
        byte[] bytes = request.getContentLengthLong() > limit ? null : request.getInputStream().readNBytes(limit + 1);
        if (bytes == null || bytes.length > limit) {
            HttpServletResponse response = (HttpServletResponse) res;
            response.setStatus(413);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"La solicitud supera el tamaño permitido.\"}");
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                return new ServletInputStream() {
                    @Override public int read() { return input.read(); }
                    @Override public int read(byte[] b, int off, int len) { return input.read(b, off, len); }
                    @Override public boolean isFinished() { return input.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                };
            }
            @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
        }, res);
    }
}
