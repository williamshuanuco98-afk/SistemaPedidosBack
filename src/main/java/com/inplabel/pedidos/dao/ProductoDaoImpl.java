package com.inplabel.pedidos.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProductoDaoImpl implements ProductoDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id_producto, nombre_producto, tipo_producto FROM producto ORDER BY id_producto ASC"
        );
        for (Map<String, Object> map : list) {
            String tipo = (String) map.get("tipo_producto");
            map.put("categoria", tipo != null && !tipo.isEmpty() ? tipo : "General");
        }
        return list;
    }

    @Override
    public Map<String, Object> findById(Integer id) {
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

    @Override
    public Map<String, Object> save(String nombreProducto, String tipoProducto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO producto (nombre_producto, tipo_producto) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, nombreProducto);
            ps.setString(2, tipoProducto);
            return ps;
        }, keyHolder);

        Number newId = keyHolder.getKey();
        int generatedId = newId != null ? newId.intValue() : 0;

        Map<String, Object> res = new HashMap<>();
        res.put("id_producto", generatedId);
        res.put("nombre_producto", nombreProducto);
        res.put("tipo_producto", tipoProducto);
        res.put("categoria", tipoProducto);
        return res;
    }

    @Override
    public Map<String, Object> update(Integer id, String nombreProducto, String tipoProducto) {
        jdbcTemplate.update(
            "UPDATE producto SET nombre_producto = ?, tipo_producto = ? WHERE id_producto = ?",
            nombreProducto, tipoProducto, id
        );

        Map<String, Object> res = new HashMap<>();
        res.put("id_producto", id);
        res.put("nombre_producto", nombreProducto);
        res.put("tipo_producto", tipoProducto);
        res.put("categoria", tipoProducto);
        return res;
    }

    @Override
    public boolean delete(Integer id) {
        int rows = jdbcTemplate.update("DELETE FROM producto WHERE id_producto = ?", id);
        return rows > 0;
    }
}
