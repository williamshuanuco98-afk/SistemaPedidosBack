package com.inplabel.pedidos.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class EnvioPedidoDaoImpl implements EnvioPedidoDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> findByPedido(int idPedido) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT e.* FROM envios_pedido e WHERE e.id_pedido = ? ORDER BY e.id_envio ASC",
            idPedido
        );
        for (Map<String, Object> envio : list) {
            Integer idEnvio = (Integer) envio.get("id_envio");
            List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                "SELECT de.*, pr.nombre_producto FROM detalle_envios_pedido de " +
                "LEFT JOIN producto pr ON de.id_producto = pr.id_producto WHERE de.id_envio = ?",
                idEnvio
            );
            envio.put("detalles", detalles);
        }
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> body) {
        Object idPedidoRaw = body.get("id_pedido");
        int idPedido = 0;
        if (idPedidoRaw instanceof Number) {
            idPedido = ((Number) idPedidoRaw).intValue();
        } else if (idPedidoRaw != null) {
            try {
                String str = idPedidoRaw.toString().replaceAll("[^0-9]", "");
                if (!str.isEmpty()) idPedido = Integer.parseInt(str);
            } catch (Exception ignored) {}
        }

        Object idClienteRaw = body.get("id_cliente");
        Integer idCliente = null;
        if (idClienteRaw instanceof Number) {
            idCliente = ((Number) idClienteRaw).intValue();
        } else if (idClienteRaw != null) {
            try {
                String str = idClienteRaw.toString().replaceAll("[^0-9]", "");
                if (!str.isEmpty()) idCliente = Integer.parseInt(str);
            } catch (Exception ignored) {}
        }

        String fechaStr = (String) body.getOrDefault("fecha_envio", LocalDate.now().toString());
        String nroComprobante = (String) body.getOrDefault("nro_comprobante", "");
        if (nroComprobante == null || nroComprobante.trim().isEmpty()) {
            nroComprobante = (String) body.getOrDefault("nro_guia", "");
        }
        Boolean cerrarSaldo = Boolean.TRUE.equals(body.get("cerrar_saldo"));
        String observaciones = (String) body.getOrDefault("observaciones", "");

        final int finalIdPedido = idPedido;
        final String finalFecha = fechaStr;
        final String finalNroComp = nroComprobante != null ? nroComprobante.trim() : "";
        final String finalObs = observaciones != null ? observaciones.trim() : "";
        final Integer finalIdCliente = idCliente;
        final Boolean finalCerrarSaldo = cerrarSaldo;

        // Duplicate submission prevention: check if identical shipment was created in last 5 seconds
        try {
            Integer duplicateId = jdbcTemplate.queryForObject(
                "SELECT id_envio FROM envios_pedido WHERE id_pedido = ? AND nro_comprobante = ? AND fecha_envio = ? AND TIMESTAMPDIFF(SECOND, fecha_registro, NOW()) < 5 ORDER BY id_envio DESC LIMIT 1",
                Integer.class, finalIdPedido, finalNroComp, finalFecha
            );
            if (duplicateId != null && duplicateId > 0) {
                body.put("id_envio", duplicateId);
                body.put("duplicate_prevented", true);
                return body;
            }
        } catch (Exception ignored) {}

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO envios_pedido (id_pedido, id_cliente, nro_comprobante, fecha_envio, cerrar_saldo, observaciones) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, finalIdPedido);
            if (finalIdCliente != null) ps.setInt(2, finalIdCliente); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, finalNroComp);
            ps.setString(4, finalFecha);
            ps.setBoolean(5, finalCerrarSaldo);
            ps.setString(6, finalObs);
            return ps;
        }, keyHolder);

        Number newIdNum = keyHolder.getKey();
        int newId = newIdNum != null ? newIdNum.intValue() : 0;

        List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");
        if (detalles != null) {
            for (Map<String, Object> item : detalles) {
                Number pIdNum = (Number) item.get("id_producto");
                Number cantNum = (Number) item.get("cantidad");
                String prodName = (String) item.get("nombre_producto");

                if (pIdNum != null) {
                    int pId = pIdNum.intValue();
                    try {
                        Integer exists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM producto WHERE id_producto = ?",
                            Integer.class, pId
                        );
                        if (exists == null || exists == 0) {
                            if (prodName == null || prodName.trim().isEmpty()) prodName = "PRODUCTO #" + pId;
                            jdbcTemplate.update(
                                "INSERT INTO producto (id_producto, nombre_producto, tipo_producto) VALUES (?, ?, ?)",
                                pId, prodName, "MERCADERIA"
                            );
                        }
                    } catch (Exception ignored) {}

                    jdbcTemplate.update(
                        "INSERT INTO detalle_envios_pedido (id_envio, id_producto, cantidad) VALUES (?, ?, ?)",
                        newId, pId, cantNum != null ? cantNum.intValue() : 1
                    );
                }
            }
        }

        if (idPedido > 0) {
            Integer totalRequested = 0;
            try {
                totalRequested = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(cantidad), 0) FROM detalle_pedido WHERE id_pedido = ?",
                    Integer.class, idPedido
                );
            } catch (Exception ignored) {}

            Integer totalDelivered = 0;
            try {
                totalDelivered = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(de.cantidad), 0) FROM detalle_envios_pedido de " +
                    "INNER JOIN envios_pedido e ON de.id_envio = e.id_envio " +
                    "WHERE e.id_pedido = ?",
                    Integer.class, idPedido
                );
            } catch (Exception ignored) {}

            String nuevoEstadoPedido = "EN PROCESO";
            if (totalRequested != null && totalDelivered != null && totalDelivered >= totalRequested && totalRequested > 0) {
                nuevoEstadoPedido = "COMPLETADO";
            } else if (finalCerrarSaldo) {
                nuevoEstadoPedido = "FINALIZADO";
            }

            jdbcTemplate.update(
                "UPDATE pedido SET fecha_entrega = ?, estado = ? WHERE id_pedido = ?",
                finalFecha, nuevoEstadoPedido, idPedido
            );
        }

        body.put("id_envio", newId);
        body.put("fecha_envio", finalFecha);
        body.put("nro_comprobante", finalNroComp);
        return body;
    }
}
