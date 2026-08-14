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
public class ClienteDaoImpl implements ClienteDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(
                "SELECT id_cliente, tipo_documento, nro_documento, razon_social AS nombre_cliente, direccion FROM cliente ORDER BY id_cliente DESC");
    }

    @Override
    public Map<String, Object> save(String tipoDocumento, String nroDocumento, String razonSocial, String direccion) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO cliente (tipo_documento, nro_documento, razon_social, direccion) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tipoDocumento);
            ps.setString(2, nroDocumento);
            ps.setString(3, razonSocial);
            ps.setString(4, direccion);
            return ps;
        }, keyHolder);

        Number newId = keyHolder.getKey();
        Map<String, Object> res = new HashMap<>();
        res.put("id_cliente", newId != null ? newId.intValue() : 0);
        res.put("tipo_documento", tipoDocumento);
        res.put("nro_documento", nroDocumento);
        res.put("nombre_cliente", razonSocial);
        res.put("direccion", direccion);
        return res;
    }

    @Override
    public Map<String, Object> update(int id, String tipoDocumento, String nroDocumento, String razonSocial, String direccion) {
        jdbcTemplate.update(
                "UPDATE cliente SET tipo_documento = ?, nro_documento = ?, razon_social = ?, direccion = ? WHERE id_cliente = ?",
                tipoDocumento, nroDocumento, razonSocial, direccion, id);

        Map<String, Object> res = new HashMap<>();
        res.put("id_cliente", id);
        res.put("tipo_documento", tipoDocumento);
        res.put("nro_documento", nroDocumento);
        res.put("nombre_cliente", razonSocial);
        res.put("direccion", direccion);
        return res;
    }

    @Override
    public boolean delete(int id) {
        int rows = jdbcTemplate.update("DELETE FROM cliente WHERE id_cliente = ?", id);
        return rows > 0;
    }
}
