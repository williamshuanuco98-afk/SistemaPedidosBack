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
            "SELECT id_producto, nombre_producto, tipo_producto, COALESCE(NULLIF(TRIM(unidad_medida), ''), 'UNID') AS unidad_medida FROM producto ORDER BY id_producto ASC"
        );
        for (Map<String, Object> map : list) {
            String tipo = (String) map.get("tipo_producto");
            map.put("categoria", tipo != null && !tipo.isEmpty() ? tipo : "General");
            if (map.get("unidad_medida") == null) {
                map.put("unidad_medida", "UNID");
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> findById(Integer id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id_producto, nombre_producto, tipo_producto, COALESCE(NULLIF(TRIM(unidad_medida), ''), 'UNID') AS unidad_medida FROM producto WHERE id_producto = ?",
            id
        );
        if (list.isEmpty()) {
            return Map.of("error", "Producto no encontrado");
        }
        Map<String, Object> res = list.get(0);
        String tipo = (String) res.get("tipo_producto");
        res.put("categoria", tipo != null && !tipo.isEmpty() ? tipo : "General");
        if (res.get("unidad_medida") == null) {
            res.put("unidad_medida", "UNID");
        }
        return res;
    }

    @Override
    public Map<String, Object> save(String nombreProducto, String tipoProducto) {
        return save(nombreProducto, tipoProducto, "UNID");
    }

    @Override
    public Map<String, Object> save(String nombreProducto, String tipoProducto, String unidadMedida) {
        String finalUM = (unidadMedida != null && !unidadMedida.trim().isEmpty()) ? unidadMedida.trim().toUpperCase() : "UNID";
        
        Integer nextId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id_producto), 0) + 1 FROM producto", Integer.class);
        if (nextId == null || nextId < 1) nextId = 1;
        int generatedId = nextId;

        jdbcTemplate.update(
            "INSERT INTO producto (id_producto, nombre_producto, tipo_producto, unidad_medida) VALUES (?, ?, ?, ?)",
            generatedId, nombreProducto, tipoProducto, finalUM
        );

        Map<String, Object> res = new HashMap<>();
        res.put("id_producto", generatedId);
        res.put("nombre_producto", nombreProducto);
        res.put("tipo_producto", tipoProducto);
        res.put("categoria", tipoProducto);
        res.put("unidad_medida", finalUM);
        return res;
    }

    @Override
    public Map<String, Object> update(Integer id, String nombreProducto, String tipoProducto) {
        return update(id, nombreProducto, tipoProducto, "UNID");
    }

    @Override
    public Map<String, Object> update(Integer id, String nombreProducto, String tipoProducto, String unidadMedida) {
        String finalUM = (unidadMedida != null && !unidadMedida.trim().isEmpty()) ? unidadMedida.trim().toUpperCase() : "UNID";
        jdbcTemplate.update(
            "UPDATE producto SET nombre_producto = ?, tipo_producto = ?, unidad_medida = ? WHERE id_producto = ?",
            nombreProducto, tipoProducto, finalUM, id
        );

        Map<String, Object> res = new HashMap<>();
        res.put("id_producto", id);
        res.put("nombre_producto", nombreProducto);
        res.put("tipo_producto", tipoProducto);
        res.put("categoria", tipoProducto);
        res.put("unidad_medida", finalUM);
        return res;
    }

    @Override
    public boolean delete(Integer id) {
        int rows = jdbcTemplate.update("DELETE FROM producto WHERE id_producto = ?", id);
        return rows > 0;
    }
}
