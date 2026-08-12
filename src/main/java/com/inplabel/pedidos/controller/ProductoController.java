package com.inplabel.pedidos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
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
            "SELECT id_producto, nombre_producto, tipo_producto FROM producto ORDER BY id_producto ASC"
        );
        for (Map<String, Object> map : list) {
            String tipo = (String) map.get("tipo_producto");
            map.put("categoria", tipo != null && !tipo.isEmpty() ? tipo : "General");
        }
        return list;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProductoById(@PathVariable Integer id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id_producto, nombre_producto, tipo_producto FROM producto WHERE id_producto = ?",
            id
        );
        if (list.isEmpty()) {
            return Map.of("error", "Producto no encontrado");
        }
        Map<String, Object> res = list.get(0);
        String tipo = (String) res.get("tipo_producto");
        res.put("categoria", tipo != null && !tipo.isEmpty() ? tipo : "General");
        return res;
    }

    @PostMapping
    @Transactional
    public Map<String, Object> addProducto(@RequestBody Map<String, Object> body) {
        String nombre = (String) body.getOrDefault("nombre_producto", "");
        String tipo = (String) body.getOrDefault("tipo_producto", (String) body.getOrDefault("categoria", "General"));

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO producto (nombre_producto, tipo_producto) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, nombre);
            ps.setString(2, tipo);
            return ps;
        }, keyHolder);

        Number newId = keyHolder.getKey();
        int generatedId = newId != null ? newId.intValue() : 0;

        body.put("id_producto", generatedId);
        body.put("tipo_producto", tipo);
        body.put("categoria", tipo);
        return body;
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> updateProducto(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String nombre = (String) body.getOrDefault("nombre_producto", "");
        String tipo = (String) body.getOrDefault("tipo_producto", (String) body.getOrDefault("categoria", "General"));

        jdbcTemplate.update(
            "UPDATE producto SET nombre_producto = ?, tipo_producto = ? WHERE id_producto = ?",
            nombre, tipo, id
        );

        body.put("id_producto", id);
        body.put("tipo_producto", tipo);
        body.put("categoria", tipo);
        return body;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> deleteProducto(@PathVariable Integer id) {
        jdbcTemplate.update("DELETE FROM producto WHERE id_producto = ?", id);
        return Map.of("success", true, "id_producto", id);
    }
}
