package com.inplabel.pedidos.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inplabel.pedidos.util.FileStorageUtil;
import jakarta.annotation.PostConstruct;
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
public class PedidoDaoImpl implements PedidoDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initTable() {
        try {
            jdbcTemplate.update("ALTER TABLE pedido ADD COLUMN adelantos TEXT");
        } catch (Exception ignored) {}
    }

    @Override
    public void cleanOrphanDetails() {
        try {
            jdbcTemplate.update("DELETE FROM detalle_pedido WHERE id_pedido NOT IN (SELECT id_pedido FROM pedido)");
        } catch (Exception ignored) {}
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findAll() {
        cleanOrphanDetails();

        List<Map<String, Object>> pedidos = jdbcTemplate.queryForList(
                "SELECT p.*, c.razon_social AS nombre_cliente, c.nro_documento " +
                        "FROM pedido p LEFT JOIN cliente c ON p.id_cliente = c.id_cliente ORDER BY p.id_pedido DESC");

        for (Map<String, Object> order : pedidos) {
            Integer idPedido = (Integer) order.get("id_pedido");
            String nroPedido = (String) order.get("nro_pedido");
            if (nroPedido == null || nroPedido.isEmpty()) {
                nroPedido = String.format("PED-%04d", idPedido);
                order.put("nro_pedido", nroPedido);
            }

            // Fetch order items
            List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                    "SELECT d.*, pr.nombre_producto FROM detalle_pedido d " +
                            "LEFT JOIN producto pr ON d.id_producto = pr.id_producto WHERE d.id_pedido = ?",
                    idPedido);

            // Fetch linked guias/shipments for partial delivery tracking
            List<Map<String, Object>> guias = jdbcTemplate.queryForList(
                    "SELECT g.* FROM guias g WHERE g.id_pedido = ? ORDER BY g.id_guia ASC",
                    idPedido);

            for (Map<String, Object> guia : guias) {
                Integer idGuia = (Integer) guia.get("id_guia");
                List<Map<String, Object>> detGuia = jdbcTemplate.queryForList(
                        "SELECT dg.*, pr.nombre_producto FROM detalle_guias dg " +
                                "LEFT JOIN producto pr ON dg.id_producto = pr.id_producto WHERE dg.id_guia = ?",
                        idGuia);
                guia.put("detalles", detGuia);
            }

            // Calculate accumulated delivered quantity for each item
            for (Map<String, Object> item : detalles) {
                Number idProdNum = (Number) item.get("id_producto");
                int totalDelivered = 0;
                if (idProdNum != null) {
                    int idProd = idProdNum.intValue();
                    for (Map<String, Object> g : guias) {
                        List<Map<String, Object>> gDetalles = (List<Map<String, Object>>) g.get("detalles");
                        if (gDetalles != null) {
                            for (Map<String, Object> gd : gDetalles) {
                                Number gProdIdNum = (Number) gd.get("id_producto");
                                if (gProdIdNum != null && gProdIdNum.intValue() == idProd) {
                                    Number cant = (Number) gd.get("cantidad");
                                    if (cant != null)
                                        totalDelivered += cant.intValue();
                                }
                            }
                        }
                    }
                }
                item.put("cantidad_entregada", totalDelivered);
            }

            order.put("detalles", detalles);
            order.put("guias", guias);

            // Parse adelantos JSON if present
            Object adelantosRaw = order.get("adelantos");
            if (adelantosRaw instanceof String) {
                String strVal = ((String) adelantosRaw).trim();
                if (strVal.startsWith("[")) {
                    try {
                        List<Map<String, Object>> parsedList = objectMapper.readValue(strVal, List.class);
                        order.put("adelantos", parsedList);
                    } catch (Exception ignored) {}
                }
            }

            // Calculate dynamic order status if not explicitly CANCELADO, ANULADO or FINALIZADO
            String dbEstado = (String) order.get("estado");
            if (dbEstado == null) dbEstado = "PENDIENTE";
            dbEstado = dbEstado.trim().toUpperCase();

            int sumRequested = 0;
            int sumDelivered = 0;

            for (Map<String, Object> item : detalles) {
                Number reqNum = (Number) item.get("cantidad");
                Number delNum = (Number) item.get("cantidad_entregada");
                if (reqNum != null) sumRequested += reqNum.intValue();
                if (delNum != null) sumDelivered += delNum.intValue();
            }

            if (!"CANCELADO".equals(dbEstado) && !"ANULADO".equals(dbEstado) && !"FINALIZADO".equals(dbEstado)) {
                if (sumDelivered >= sumRequested && sumRequested > 0) {
                    order.put("estado", "COMPLETADO");
                } else if ((guias != null && !guias.isEmpty()) || sumDelivered > 0 || "EN PROCESO".equals(dbEstado) || "EN_PROCESO".equals(dbEstado)) {
                    order.put("estado", "EN PROCESO");
                }
            }
        }

        return pedidos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> body) {
        Number idClienteNum = (Number) body.get("id_cliente");
        int idCliente = idClienteNum != null ? idClienteNum.intValue() : 0;
        String todayStr = LocalDate.now().toString();
        String fecha = (String) body.getOrDefault("fecha_pedido", todayStr);
        if (fecha != null && fecha.compareTo(todayStr) > 0) {
            fecha = todayStr;
        }
        String fechaEntrega = (String) body.getOrDefault("fecha_entrega", fecha);
        String estado = (String) body.getOrDefault("estado", "PENDIENTE");
        String nroOrden = (String) body.getOrDefault("nro_orden_compra", "");
        if (nroOrden == null || nroOrden.isEmpty()) {
            nroOrden = (String) body.getOrDefault("nro_orden", "");
        }
        String observaciones = (String) body.getOrDefault("observaciones", "");
        String establecimiento = (String) body.getOrDefault("establecimiento", "COMAS");

        Object adelantosObj = body.get("adelantos");
        String adelantosJson = "";
        if (adelantosObj != null) {
            try {
                adelantosJson = objectMapper.writeValueAsString(adelantosObj);
            } catch (Exception ignored) {}
        }

        Object adjuntosObj = body.get("adjuntos");
        String storagePath = (String) body.getOrDefault("storage_path", "C:\\Users\\User\\OneDrive\\Escritorio\\OrdenesI");
        boolean useSubfolders = Boolean.TRUE.equals(body.get("use_subfolders"));

        final String finalFecha = fecha;
        final String finalFechaEntrega = fechaEntrega;
        final String finalNroOrden = nroOrden;
        final String finalObs = observaciones;
        final String finalEstab = establecimiento;
        final String finalAdelantosJson = adelantosJson;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO pedido (id_cliente, fecha_pedido, fecha_entrega, estado, nro_orden, adjuntos, observaciones, establecimiento, adelantos) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idCliente);
            ps.setString(2, finalFecha);
            ps.setString(3, finalFechaEntrega);
            ps.setString(4, estado);
            ps.setString(5, finalNroOrden);
            ps.setString(6, "");
            ps.setString(7, finalObs);
            ps.setString(8, finalEstab);
            ps.setString(9, finalAdelantosJson);
            return ps;
        }, keyHolder);

        Number newIdNum = keyHolder.getKey();
        int newId = newIdNum != null ? newIdNum.intValue() : 0;
        String nroPedido = String.format("PED-%04d", newId);

        // Ensure no stale details exist for newId
        jdbcTemplate.update("DELETE FROM detalle_pedido WHERE id_pedido = ?", newId);

        // Save attached files and update JSON string
        String updatedAdjuntosJson = fileStorageUtil.saveAttachedFiles(adjuntosObj, storagePath, useSubfolders, nroPedido);
        jdbcTemplate.update("UPDATE pedido SET nro_pedido = ?, adjuntos = ? WHERE id_pedido = ?", nroPedido, updatedAdjuntosJson, newId);

        List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");
        if (detalles != null) {
            for (Map<String, Object> item : detalles) {
                Number pIdNum = (Number) item.get("id_producto");
                Number cantNum = (Number) item.get("cantidad");
                if (pIdNum != null) {
                    jdbcTemplate.update(
                            "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad) VALUES (?, ?, ?)",
                            newId, pIdNum.intValue(), cantNum != null ? cantNum.intValue() : 1);
                }
            }
        }

        body.put("id_pedido", newId);
        body.put("nro_pedido", nroPedido);
        body.put("fecha_pedido", fecha);
        body.put("fecha_entrega", finalFechaEntrega);
        return body;
    }

    @Override
    public Map<String, Object> update(int id, Map<String, Object> body) {
        String fecha = (String) body.get("fecha_pedido");
        String fechaEntrega = (String) body.get("fecha_entrega");
        String estado = (String) body.get("estado");

        if (body.containsKey("adelantos")) {
            Object adelantosObj = body.get("adelantos");
            try {
                String adelantosJson = objectMapper.writeValueAsString(adelantosObj);
                jdbcTemplate.update("UPDATE pedido SET adelantos = ? WHERE id_pedido = ?", adelantosJson, id);
            } catch (Exception ignored) {}
        }

        if (fecha != null && estado != null) {
            if (fechaEntrega != null && !fechaEntrega.isEmpty()) {
                jdbcTemplate.update(
                        "UPDATE pedido SET fecha_pedido = ?, fecha_entrega = ?, estado = ? WHERE id_pedido = ?",
                        fecha, fechaEntrega, estado, id);
            } else {
                jdbcTemplate.update(
                        "UPDATE pedido SET fecha_pedido = ?, estado = ? WHERE id_pedido = ?",
                        fecha, estado, id);
            }
        } else if (estado != null) {
            jdbcTemplate.update("UPDATE pedido SET estado = ? WHERE id_pedido = ?", estado, id);
        }

        body.put("id_pedido", id);
        return body;
    }
}
