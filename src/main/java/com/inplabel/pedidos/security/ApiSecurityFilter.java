package com.inplabel.pedidos.security;

import com.inplabel.pedidos.dao.UsuarioDao;
import com.inplabel.pedidos.model.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Set;

/** Identity is always resolved on the server; client role headers are never trusted. */
@Component
@Order(2)
public class ApiSecurityFilter implements Filter {
    public static final String USER_ID = "authenticatedUserId";
    public static final String PASSWORD_VERSION = "authenticatedPasswordVersion";
    public static final String CURRENT_USER = "authenticatedUser";
    private final UsuarioDao users;
    public ApiSecurityFilter(UsuarioDao users) { this.users = users; }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/api/")) { chain.doFilter(req, res); return; }
        boolean write = !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        // CORS is disabled. Foreign pages cannot supply this non-simple header.
        if (write && !"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            reject(response, 403, "Solicitud no autorizada. Recargue la aplicación."); return;
        }
        if ("cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
            reject(response, 403, "Origen no autorizado."); return;
        }
        if (path.equals("/api/auth/login") && request.getMethod().equals("POST")) { chain.doFilter(req, res); return; }
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(USER_ID) instanceof Integer id)) {
            reject(response, 401, "Inicie sesión para continuar."); return;
        }
        Usuario user = users.findById(id).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActivo()) ||
                !java.util.Objects.equals(user.getPassword(), session.getAttribute(PASSWORD_VERSION))) {
            session.invalidate();
            reject(response, 401, "La sesión ha expirado o el acceso fue revocado."); return;
        }
        request.setAttribute(CURRENT_USER, user);
        if (!allowed(user, path, request.getMethod())) {
            reject(response, 403, "No tiene permiso para realizar esta operación."); return;
        }
        chain.doFilter(req, res);
    }

    public static boolean has(Usuario user, String... permissions) {
        if (user == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRol()) || "ADMINISTRADOR".equalsIgnoreCase(user.getRol())) return true;
        for (String permission : permissions)
            if (user.getPermisos() != null && user.getPermisos().contains(permission)) return true;
        return false;
    }

    static boolean allowed(Usuario user, String path, String method) {
        boolean read = method.equals("GET") || method.equals("HEAD");
        if (path.equals("/api/auth/me")) return read;
        if (path.equals("/api/auth/logout")) return method.equals("POST");
        if (path.equals("/api/status")) return read;
        String[] parts = path.split("/", 4);
        String module = parts.length > 2 ? parts[2] : "";
        return switch (module) {
            case "usuarios" -> has(user, "usuarios.manage");
            case "clientes" -> read ? has(user, "clientes.manage", "pedidos.view", "pedidos.create", "guias.create", "pedidos.finances")
                    : method.equals("DELETE") ? has(user) : has(user, "clientes.manage");
            case "productos" -> read ? has(user, "productos.manage", "pedidos.view", "pedidos.create", "guias.create", "produccion.view", "envios.create")
                    : method.equals("DELETE") ? has(user) : has(user, "productos.manage");
            case "pedidos" -> read ? has(user, "pedidos.view", "produccion.view", "envios.view", "envios.create")
                    : method.equals("POST") ? has(user, "pedidos.create")
                    : method.equals("PUT") && has(user, "pedidos.edit", "pedidos.cancel", "pedidos.finish", "pedidos.finances");
            case "envios-pedido" -> read ? has(user, "envios.view", "pedidos.view") : has(user, "envios.create");
            case "guias" -> read ? has(user, "guias.view", "guias.create") : has(user, "guias.create");
            case "letras" -> has(user, "pedidos.finances");
            default -> false;
        };
    }
    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
