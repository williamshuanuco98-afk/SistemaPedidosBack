package com.inplabel.pedidos.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class GuiaDaoImpl implements GuiaDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> guias = jdbcTemplate.queryForList(
            "SELECT g.*, c.razon_social AS nombre_cliente, c.nro_documento, c.direccion AS direccion_destino, p.nro_pedido " +
            "FROM guias g LEFT JOIN cliente c ON g.id_cliente = c.id_cliente LEFT JOIN pedido p ON g.id_pedido = p.id_pedido ORDER BY g.id_guia DESC"
        );

        for (Map<String, Object> guia : guias) {
            Integer idGuia = (Integer) guia.get("id_guia");
            List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                "SELECT d.*, pr.nombre_producto, CONCAT('PROD-', d.id_producto) AS codigo_producto FROM detalle_guias d " +
                "LEFT JOIN producto pr ON d.id_producto = pr.id_producto WHERE d.id_guia = ?",
                idGuia
            );
            guia.put("detalles", detalles);
        }

        return guias;
    }

    @Override
    public Map<String, Object> findById(int id) {
        List<Map<String, Object>> guias = jdbcTemplate.queryForList(
            "SELECT g.*, c.razon_social AS nombre_cliente, c.nro_documento, c.direccion AS direccion_destino, p.nro_pedido " +
            "FROM guias g LEFT JOIN cliente c ON g.id_cliente = c.id_cliente LEFT JOIN pedido p ON g.id_pedido = p.id_pedido WHERE g.id_guia = ?",
            id
        );
        if (guias.isEmpty()) return null;
        Map<String, Object> guia = guias.get(0);
        List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
            "SELECT d.*, pr.nombre_producto, CONCAT('PROD-', d.id_producto) AS codigo_producto FROM detalle_guias d " +
            "LEFT JOIN producto pr ON d.id_producto = pr.id_producto WHERE d.id_guia = ?",
            id
        );
        guia.put("detalles", detalles);
        return guia;
    }

    @Override
    public Map<String, String> getNextNumber(String serie) {
        String prefix = serie != null && serie.toUpperCase().startsWith("GR002") ? "GR002" : "GR001";
        String sql = "SELECT nro_guia FROM guias WHERE nro_guia LIKE ? ORDER BY id_guia DESC";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("nro_guia"), prefix + "-%");
        
        int maxNum = 0;
        for (String lastStr : list) {
            if (lastStr != null) {
                try {
                    String[] parts = lastStr.split("-");
                    if (parts.length > 1) {
                        int num = Integer.parseInt(parts[1]);
                        if (num > maxNum) maxNum = num;
                    }
                } catch (Exception ignored) {}
            }
        }

        String nextNro = String.format("%s-%04d", prefix, maxNum + 1);
        Map<String, String> res = new HashMap<>();
        res.put("next_nro_guia", nextNro);
        res.put("serie", prefix);
        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> body) {
        int idCliente = 0;
        Object idClienteRaw = body.get("id_cliente");
        if (idClienteRaw instanceof Number) {
            idCliente = ((Number) idClienteRaw).intValue();
        } else if (idClienteRaw instanceof String) {
            try {
                String str = ((String) idClienteRaw).replaceAll("[^0-9]", "");
                if (!str.isEmpty()) idCliente = Integer.parseInt(str);
            } catch (Exception ignored) {}
        }

        Integer idPedido = null;
        Object idPedidoRaw = body.get("id_pedido");
        if (idPedidoRaw instanceof Number) {
            idPedido = ((Number) idPedidoRaw).intValue();
        } else if (idPedidoRaw instanceof String) {
            try {
                String str = ((String) idPedidoRaw).replaceAll("[^0-9]", "");
                if (!str.isEmpty()) idPedido = Integer.parseInt(str);
            } catch (Exception ignored) {}
        }

        String fecha = (String) body.getOrDefault("fecha_guia", LocalDate.now().toString());
        String nroGuia = (String) body.get("nro_guia");
        String establecimiento = (String) body.getOrDefault("establecimiento", "CARABAYLLO");
        String seriePrefix = "CARABAYLLO".equalsIgnoreCase(establecimiento) ? "GR001" : "GR002";

        if (nroGuia == null || nroGuia.isEmpty()) {
            Map<String, String> nextRes = getNextNumber(seriePrefix);
            nroGuia = nextRes.get("next_nro_guia");
        }

        String estado = (String) body.getOrDefault("estado", "EMITIDA");
        String docRef = (String) body.getOrDefault("doc_referencia", "");
        String puntoPartida = (String) body.getOrDefault("punto_partida", "");
        String puntoLlegada = (String) body.getOrDefault("punto_llegada", "");
        String observaciones = (String) body.getOrDefault("observaciones", "");
        String storagePath = (String) body.getOrDefault("storage_path", "C:\\Users\\User\\OneDrive\\Escritorio\\GuiasI");

        List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");

        final int finalIdCliente = idCliente;
        final Integer finalIdPedido = idPedido;
        final String finalNroGuia = nroGuia;
        final String finalDocRef = docRef;
        final String finalPuntoPartida = puntoPartida;
        final String finalPuntoLlegada = puntoLlegada;
        final String finalObs = observaciones;
        final String finalEstab = establecimiento;
        final String finalStorage = storagePath;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO guias (id_cliente, id_pedido, fecha_guia, nro_guia, estado, activo, doc_referencia, punto_partida, punto_llegada, observaciones, establecimiento, storage_path) " +
                "VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, finalIdCliente);
            if (finalIdPedido != null) ps.setInt(2, finalIdPedido); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, fecha);
            ps.setString(4, finalNroGuia);
            ps.setString(5, estado);
            ps.setString(6, finalDocRef);
            ps.setString(7, finalPuntoPartida);
            ps.setString(8, finalPuntoLlegada);
            ps.setString(9, finalObs);
            ps.setString(10, finalEstab);
            ps.setString(11, finalStorage);
            return ps;
        }, keyHolder);

        Number newIdNum = keyHolder.getKey();
        int newId = newIdNum != null ? newIdNum.intValue() : 0;

        if (detalles != null) {
            for (Map<String, Object> item : detalles) {
                Number pIdNum = (Number) item.get("id_producto");
                Number cantNum = (Number) item.get("cantidad");
                if (pIdNum != null) {
                    jdbcTemplate.update(
                        "INSERT INTO detalle_guias (id_guia, id_producto, cantidad) VALUES (?, ?, ?)",
                        newId, pIdNum.intValue(), cantNum != null ? cantNum.intValue() : 1
                    );
                }
            }
        }

        if (idPedido != null) {
            Boolean cerrarSaldo = body.get("cerrar_saldo") != null ? Boolean.parseBoolean(String.valueOf(body.get("cerrar_saldo"))) : false;
            String reqEstado = (String) body.get("estado_pedido");

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
                    "SELECT COALESCE(SUM(dg.cantidad), 0) FROM detalle_guias dg " +
                    "INNER JOIN guias g ON dg.id_guia = g.id_guia " +
                    "WHERE g.id_pedido = ? AND (g.estado IS NULL OR g.estado != 'ANULADA')",
                    Integer.class, idPedido
                );
            } catch (Exception ignored) {}

            String nuevoEstadoPedido = "EN PROCESO";
            if (reqEstado != null && !reqEstado.trim().isEmpty()) {
                nuevoEstadoPedido = reqEstado.trim().toUpperCase();
            } else if (totalRequested != null && totalDelivered != null && totalDelivered >= totalRequested && totalRequested > 0) {
                nuevoEstadoPedido = "COMPLETADO";
            } else if (cerrarSaldo) {
                nuevoEstadoPedido = "FINALIZADO";
            }

            jdbcTemplate.update(
                "UPDATE pedido SET nro_guia = ?, fecha_entrega = ?, estado = ? WHERE id_pedido = ?",
                finalNroGuia, fecha, nuevoEstadoPedido, idPedido
            );
        }

        body.put("id_guia", newId);
        body.put("nro_guia", finalNroGuia);
        body.put("fecha_guia", fecha);
        body.put("estado", estado);
        return body;
    }

    @Override
    public Map<String, Object> anular(int id, String motivo) {
        jdbcTemplate.update(
            "UPDATE guias SET estado = 'ANULADA', motivo_anulacion = ? WHERE id_guia = ?",
            motivo, id
        );

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id_guia", id);
        res.put("estado", "ANULADA");
        res.put("motivo_anulacion", motivo);
        return res;
    }

    @Override
    public Map<String, Object> update(int id, Map<String, Object> body) {
        String nroGuia = (String) body.get("nro_guia");
        String estado = (String) body.getOrDefault("estado", "EMITIDA");
        String fecha = (String) body.getOrDefault("fecha_guia", LocalDate.now().toString());

        jdbcTemplate.update(
            "UPDATE guias SET nro_guia = ?, estado = ?, fecha_guia = ? WHERE id_guia = ?",
            nroGuia, estado, fecha, id
        );

        body.put("success", true);
        body.put("id_guia", id);
        return body;
    }
}
