package com.inplabel.pedidos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getClientes() {
        return jdbcTemplate.queryForList(
            "SELECT id_cliente, tipo_documento, nro_documento, razon_social AS nombre_cliente, direccion FROM cliente ORDER BY id_cliente DESC"
        );
    }

    @PostMapping
    public Map<String, Object> addCliente(@RequestBody Map<String, Object> body) {
        String tipoDoc = (String) body.getOrDefault("tipo_documento", "RUC");
        String nroDoc = (String) body.getOrDefault("nro_documento", "");
        String nombre = (String) body.getOrDefault("nombre_cliente", "");
        String direccion = (String) body.getOrDefault("direccion", "");

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO cliente (tipo_documento, nro_documento, razon_social, direccion) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, tipoDoc);
            ps.setString(2, nroDoc);
            ps.setString(3, nombre);
            ps.setString(4, direccion);
            return ps;
        }, keyHolder);

        Number newId = keyHolder.getKey();
        body.put("id_cliente", newId != null ? newId.intValue() : 0);
        return body;
    }
}
