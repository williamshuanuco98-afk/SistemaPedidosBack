package com.inplabel.pedidos.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inplabel.pedidos.model.Usuario;
import com.inplabel.pedidos.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioDaoImpl implements UsuarioDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RowMapper<Usuario> rowMapper = (rs, rowNum) -> {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setSalt(rs.getString("salt"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        
        String permsStr = rs.getString("permisos");
        if (permsStr != null && !permsStr.trim().isEmpty()) {
            try {
                List<String> list = objectMapper.readValue(permsStr, new TypeReference<List<String>>() {});
                u.setPermisos(list);
            } catch (Exception e) {
                u.setPermisos(new ArrayList<>());
            }
        } else {
            u.setPermisos(new ArrayList<>());
        }

        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            u.setCreatedAt(ts.toLocalDateTime());
        }
        return u;
    };

    @Override
    public Optional<Usuario> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        List<Usuario> list = jdbcTemplate.query(
                "SELECT * FROM usuarios WHERE username = ? AND activo = TRUE LIMIT 1",
                rowMapper,
                username.trim().toLowerCase());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Usuario> findById(int idUsuario) {
        List<Usuario> list = jdbcTemplate.query(
                "SELECT * FROM usuarios WHERE id_usuario = ? LIMIT 1",
                rowMapper,
                idUsuario);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Usuario> findAll() {
        return jdbcTemplate.query("SELECT * FROM usuarios ORDER BY id_usuario ASC", rowMapper);
    }

    @Override
    public Usuario create(String username, String rawPassword, String nombreCompleto, String rol) {
        return createWithPermissions(username, rawPassword, nombreCompleto, rol, null);
    }

    @Override
    public Usuario createWithPermissions(String username, String rawPassword, String nombreCompleto, String rol, List<String> permisos) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);
        String permsJson = "[]";
        if (permisos != null) {
            try {
                permsJson = objectMapper.writeValueAsString(permisos);
            } catch (Exception ignored) {}
        }

        jdbcTemplate.update(
                "INSERT INTO usuarios (username, password, salt, nombre_completo, rol, activo, permisos) VALUES (?, ?, ?, ?, ?, TRUE, ?)",
                username.trim().toLowerCase(),
                hash,
                salt,
                nombreCompleto.trim(),
                rol.trim().toUpperCase(),
                permsJson);

        return findByUsername(username).orElse(null);
    }

    @Override
    public boolean updateUser(int idUsuario, String username, String nombreCompleto, String rol, Boolean activo, List<String> permisos, String newPassword) {
        String permsJson = "[]";
        if (permisos != null) {
            try {
                permsJson = objectMapper.writeValueAsString(permisos);
            } catch (Exception ignored) {}
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(newPassword, salt);
            int rows = jdbcTemplate.update(
                    "UPDATE usuarios SET username = ?, password = ?, salt = ?, nombre_completo = ?, rol = ?, activo = ?, permisos = ? WHERE id_usuario = ?",
                    username.trim().toLowerCase(), hash, salt, nombreCompleto.trim(), rol.trim().toUpperCase(), activo, permsJson, idUsuario);
            return rows > 0;
        } else {
            int rows = jdbcTemplate.update(
                    "UPDATE usuarios SET username = ?, nombre_completo = ?, rol = ?, activo = ?, permisos = ? WHERE id_usuario = ?",
                    username.trim().toLowerCase(), nombreCompleto.trim(), rol.trim().toUpperCase(), activo, permsJson, idUsuario);
            return rows > 0;
        }
    }

    @Override
    public boolean toggleActive(int idUsuario) {
        int rows = jdbcTemplate.update("UPDATE usuarios SET activo = NOT activo WHERE id_usuario = ?", idUsuario);
        return rows > 0;
    }

    @Override
    public boolean updatePassword(int idUsuario, String newRawPassword) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newRawPassword, salt);
        int rows = jdbcTemplate.update(
                "UPDATE usuarios SET password = ?, salt = ? WHERE id_usuario = ?",
                hash, salt, idUsuario);
        return rows > 0;
    }

    @Override
    public void initDefaultUsers() {
        // 1. Usuario Administrador
        if (findByUsername("admin").isEmpty()) {
            List<String> fullPerms = List.of(
                "pedidos.view", "pedidos.create", "pedidos.edit", "pedidos.cancel", "pedidos.finish", "pedidos.finances",
                "envios.create", "envios.view", "guias.create", "guias.view", "produccion.view",
                "clientes.manage", "productos.manage", "usuarios.manage"
            );
            createWithPermissions("admin", "admin123", "Administrador Inplabel", "ADMIN", fullPerms);
            System.out.println(">>> Usuario 'admin' inicializado con matriz completa de permisos.");
        }

        // 2. Usuario Operaciones
        if (findByUsername("operaciones").isEmpty()) {
            List<String> opPerms = List.of(
                "pedidos.view", "envios.create", "envios.view", "guias.create", "guias.view", "produccion.view"
            );
            createWithPermissions("operaciones", "operaciones123", "Área de Operaciones", "OPERACIONES", opPerms);
            System.out.println(">>> Usuario 'operaciones' inicializado con matriz de permisos.");
        }
    }
}
