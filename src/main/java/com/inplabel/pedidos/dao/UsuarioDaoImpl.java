package com.inplabel.pedidos.dao;

import com.inplabel.pedidos.model.Usuario;
import com.inplabel.pedidos.security.PasswordUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioDaoImpl implements UsuarioDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Usuario> rowMapper = (rs, rowNum) -> {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setSalt(rs.getString("salt"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            u.setCreatedAt(ts.toLocalDateTime());
        }
        return u;
    };

    @PostConstruct
    public void init() {
        try {
            // Asegurar que la tabla exista
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS usuarios (" +
                    "id_usuario INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "salt VARCHAR(100) NOT NULL, " +
                    "nombre_completo VARCHAR(100) NOT NULL, " +
                    "rol VARCHAR(20) NOT NULL, " +
                    "activo BOOLEAN DEFAULT TRUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
            );
            initDefaultUsers();
        } catch (Exception e) {
            System.err.println("Advertencia al inicializar tabla de usuarios: " + e.getMessage());
        }
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        List<Usuario> list = jdbcTemplate.query(
                "SELECT * FROM usuarios WHERE username = ? AND activo = TRUE LIMIT 1",
                rowMapper,
                username.trim().toLowerCase()
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Usuario> findAll() {
        return jdbcTemplate.query("SELECT * FROM usuarios ORDER BY id_usuario ASC", rowMapper);
    }

    @Override
    public Usuario create(String username, String rawPassword, String nombreCompleto, String rol) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);

        jdbcTemplate.update(
                "INSERT INTO usuarios (username, password, salt, nombre_completo, rol, activo) VALUES (?, ?, ?, ?, ?, TRUE)",
                username.trim().toLowerCase(),
                hash,
                salt,
                nombreCompleto.trim(),
                rol.trim().toUpperCase()
        );

        return findByUsername(username).orElse(null);
    }

    @Override
    public boolean updatePassword(int idUsuario, String newRawPassword) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newRawPassword, salt);
        int rows = jdbcTemplate.update(
                "UPDATE usuarios SET password = ?, salt = ? WHERE id_usuario = ?",
                hash, salt, idUsuario
        );
        return rows > 0;
    }

    @Override
    public void initDefaultUsers() {
        // 1. Usuario Administrador
        if (findByUsername("admin").isEmpty()) {
            create("admin", "admin123", "Administrador Inplabel", "ADMIN");
            System.out.println(">>> Usuario 'admin' inicializado con contraseña hasheada.");
        }

        // 2. Usuario Operaciones
        if (findByUsername("operaciones").isEmpty()) {
            create("operaciones", "operaciones123", "Área de Operaciones", "OPERACIONES");
            System.out.println(">>> Usuario 'operaciones' inicializado con contraseña hasheada.");
        }
    }
}
