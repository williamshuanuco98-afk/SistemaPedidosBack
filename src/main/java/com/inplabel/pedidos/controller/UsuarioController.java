package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.dao.UsuarioDao;
import com.inplabel.pedidos.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioDao usuarioDao;

    @GetMapping
    public ResponseEntity<List<Usuario>> getAllUsuarios() {
        List<Usuario> list = usuarioDao.findAll();
        list.forEach(u -> {
            u.setPassword(null);
            u.setSalt(null);
        });
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUsuarioById(@PathVariable int id) {
        Optional<Usuario> opt = usuarioDao.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario u = opt.get();
        u.setPassword(null);
        u.setSalt(null);
        return ResponseEntity.ok(u);
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> createUsuario(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String nombreCompleto = (String) body.get("nombreCompleto");
            String rol = (String) body.get("rol");
            List<String> permisos = (List<String>) body.get("permisos");

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nombre de usuario y contrase?a son requeridos."));
            }

            if (usuarioDao.findByUsername(username.trim()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "El nombre de usuario '" + username + "' ya existe."));
            }

            if (rol == null || rol.trim().isEmpty()) rol = "OPERADOR";

            Usuario created = usuarioDao.createWithPermissions(username, password, nombreCompleto, rol, permisos);
            if (created != null) {
                created.setPassword(null);
                created.setSalt(null);
                return ResponseEntity.ok(created);
            } else {
                return ResponseEntity.internalServerError().body(Map.of("message", "Error al crear usuario."));
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> updateUsuario(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String nombreCompleto = (String) body.get("nombreCompleto");
            String rol = (String) body.get("rol");
            Boolean activo = (Boolean) body.get("activo");
            List<String> permisos = (List<String>) body.get("permisos");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "El nombre de usuario es requerido."));
            }

            if (rol == null || rol.trim().isEmpty()) rol = "OPERADOR";
            if (activo == null) activo = true;

            boolean updated = usuarioDao.updateUser(id, username, nombreCompleto, rol, activo, permisos, password);
            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Usuario actualizado correctamente."));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable int id) {
        boolean ok = usuarioDao.toggleActive(id);
        if (ok) {
            return ResponseEntity.ok(Map.of("message", "Estado del usuario cambiado correctamente."));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}