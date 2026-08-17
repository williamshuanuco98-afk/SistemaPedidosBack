package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.dao.UsuarioDao;
import com.inplabel.pedidos.model.Usuario;
import com.inplabel.pedidos.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioDao usuarioDao;

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Debe ingresar usuario y contraseña.");
            return ResponseEntity.badRequest().body(error);
        }

        String username = request.getUsername().trim().toLowerCase();
        String password = request.getPassword();

        Optional<Usuario> userOpt = usuarioDao.findByUsername(username);

        if (userOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Usuario o contraseña incorrectos.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Usuario user = userOpt.get();

        boolean valid = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPassword());
        if (!valid) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Usuario o contraseña incorrectos.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("idUsuario", user.getIdUsuario());
        userData.put("username", user.getUsername());
        userData.put("nombreCompleto", user.getNombreCompleto());
        userData.put("rol", user.getRol());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Inicio de sesión exitoso.");
        response.put("user", userData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam String username) {
        Optional<Usuario> userOpt = usuarioDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario user = userOpt.get();
        Map<String, Object> userData = new HashMap<>();
        userData.put("idUsuario", user.getIdUsuario());
        userData.put("username", user.getUsername());
        userData.put("nombreCompleto", user.getNombreCompleto());
        userData.put("rol", user.getRol());
        return ResponseEntity.ok(userData);
    }
}
