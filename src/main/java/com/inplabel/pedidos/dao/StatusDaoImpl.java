package com.inplabel.pedidos.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class StatusDaoImpl implements StatusDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> dbInfo = jdbcTemplate.queryForMap("SELECT DATABASE() as db, VERSION() as version");
            Integer clientesCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cliente", Integer.class);
            Integer productosCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM producto", Integer.class);
            Integer pedidosCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class);
            Integer guiasCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM guias", Integer.class);

            Map<String, Integer> counts = new HashMap<>();
            counts.put("clientes", clientesCount != null ? clientesCount : 0);
            counts.put("productos", productosCount != null ? productosCount : 0);
            counts.put("pedidos", pedidosCount != null ? pedidosCount : 0);
            counts.put("guias", guiasCount != null ? guiasCount : 0);

            response.put("connected", true);
            response.put("db", dbInfo.get("db"));
            response.put("version", dbInfo.get("version"));
            response.put("counts", counts);
        } catch (Exception e) {
            response.put("connected", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
}
