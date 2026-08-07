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
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getProductos() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id_producto, nombre_producto FROM producto ORDER BY id_producto DESC"
        );
        for (Map<String, Object> map : list) {
            map.put("categoria", "Etiquetas & Insumos");
        }
        return list;
    }

    @PostMapping
    public Map<String, Object> addProducto(@RequestBody Map<String, Object> body) {
        String nombre = (String) body.getOrDefault("nombre_producto", "");
        String categoria = (String) body.getOrDefault("categoria", "Etiquetas & Insumos");

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO producto (nombre_producto) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, nombre);
            return ps;
        }, keyHolder);

        Number newId = keyHolder.getKey();
        body.put("id_producto", newId != null ? newId.intValue() : 0);
        body.put("categoria", categoria);
        return body;
    }
}
