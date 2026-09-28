package com.inplabel.pedidos.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inplabel.pedidos.util.FileStorageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PedidoDaoImpl implements PedidoDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> pedidos = jdbcTemplate.queryForList(
                "SELECT p.*, c.razon_social AS nombre_cliente, c.nro_documento " +
                        "FROM pedido p LEFT JOIN cliente c ON p.id_cliente = c.id_cliente ORDER BY p.fecha_pedido DESC, p.id_pedido DESC");

        if (pedidos.isEmpty()) {
            return pedidos;
        }

        // Batch 1: Traer todos los detalles_pedido
        List<Map<String, Object>> todosDetallesPedido = jdbcTemplate.queryForList(
            "SELECT d.*, pr.nombre_producto, COALESCE(NULLIF(TRIM(pr.unidad_medida), ''), 'UNID') AS unidad_medida FROM detalle_pedido d " +
            "LEFT JOIN producto pr ON d.id_producto = pr.id_producto ORDER BY d.id_pedido DESC, d.id_detalle ASC"
        );
        Map<Integer, List<Map<String, Object>>> detallesPorPedido = new HashMap<>();
        for (Map<String, Object> det : todosDetallesPedido) {
            Integer idPed = (Integer) det.get("id_pedido");
            if (idPed != null) {
                detallesPorPedido.computeIfAbsent(idPed, k -> new ArrayList<>()).add(det);
            }
        }

        // Batch 2: Traer todos los envios_pedido
        List<Map<String, Object>> todosEnvios = jdbcTemplate.queryForList(
            "SELECT e.* FROM envios_pedido e ORDER BY e.id_pedido DESC, e.id_envio ASC"
        );

        // Batch 3: Traer todos los detalle_envios_pedido
        List<Map<String, Object>> todosDetallesEnvio = jdbcTemplate.queryForList(
            "SELECT de.*, pr.nombre_producto FROM detalle_envios_pedido de " +
            "LEFT JOIN producto pr ON de.id_producto = pr.id_producto ORDER BY de.id_envio ASC"
        );
        Map<Integer, List<Map<String, Object>>> detallesPorEnvio = new HashMap<>();
        for (Map<String, Object> de : todosDetallesEnvio) {
            Integer idEnv = (Integer) de.get("id_envio");
            if (idEnv != null) {
                detallesPorEnvio.computeIfAbsent(idEnv, k -> new ArrayList<>()).add(de);
            }
        }

        // Asociar detalles a cada envio y agrupar envios por pedido
        Map<Integer, List<Map<String, Object>>> enviosPorPedido = new HashMap<>();
        for (Map<String, Object> envio : todosEnvios) {
            Integer idEnv = (Integer) envio.get("id_envio");
            Integer idPed = (Integer) envio.get("id_pedido");
            envio.put("detalles", detallesPorEnvio.getOrDefault(idEnv, new ArrayList<>()));
            if (idPed != null) {
                enviosPorPedido.computeIfAbsent(idPed, k -> new ArrayList<>()).add(envio);
            }
        }

        for (Map<String, Object> order : pedidos) {
            Integer idPedido = (Integer) order.get("id_pedido");
            String nroPedido = (String) order.get("nro_pedido");
            if (nroPedido == null || nroPedido.isEmpty()) {
                nroPedido = String.format("PED-%04d", idPedido);
                order.put("nro_pedido", nroPedido);
            }

            List<Map<String, Object>> detalles = detallesPorPedido.getOrDefault(idPedido, new ArrayList<>());
            List<Map<String, Object>> envios = enviosPorPedido.getOrDefault(idPedido, new ArrayList<>());

            // Calculate accumulated delivered quantity for each item from envios_pedido ONLY
            String dbEstado = (String) order.get("estado");
            if (dbEstado == null) dbEstado = "PENDIENTE";
            dbEstado = dbEstado.trim().toUpperCase();

            boolean hasEnvios = (envios != null && !envios.isEmpty());
            boolean isCompletedStatus = "COMPLETADO".equals(dbEstado) || "ENTREGADO".equals(dbEstado);

            for (Map<String, Object> item : detalles) {
                Number idProdNum = (Number) item.get("id_producto");
                Number reqCantNum = (Number) item.get("cantidad");
                int reqCant = reqCantNum != null ? reqCantNum.intValue() : 0;
                int totalDelivered = 0;

                if (hasEnvios && idProdNum != null) {
                    int idProd = idProdNum.intValue();
                    for (Map<String, Object> e : envios) {
                        List<Map<String, Object>> eDetalles = (List<Map<String, Object>>) e.get("detalles");
                        if (eDetalles != null) {
                            for (Map<String, Object> ed : eDetalles) {
                                Number eProdIdNum = (Number) ed.get("id_producto");
                                if (eProdIdNum != null && eProdIdNum.intValue() == idProd) {
                                    Number cant = (Number) ed.get("cantidad");
                                    if (cant != null)
                                        totalDelivered += cant.intValue();
                                }
                            }
                        }
                    }
                } else if (!hasEnvios && isCompletedStatus) {
                    // For legacy or manually completed orders without shipment logs, items default to 100% delivered
                    totalDelivered = reqCant;
                }

                item.put("cantidad_entregada", totalDelivered);
            }

            order.put("detalles", detalles);
            order.put("envios", envios);

            // Parse adelantos JSON if present
            Object adelantosRaw = order.get("adelantos");
            if (adelantosRaw instanceof String) {
                String strVal = ((String) adelantosRaw).trim();
                if (strVal.startsWith("[")) {
                    try {
                        List<Map<String, Object>> parsedList = objectMapper.readValue(strVal, List.class);
                        order.put("adelantos", parsedList);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Calculate dynamic order status if not explicitly CANCELADO, ANULADO or FINALIZADO
            int sumRequested = 0;
            int sumDelivered = 0;

            for (Map<String, Object> item : detalles) {
                Number reqNum = (Number) item.get("cantidad");
                Number delNum = (Number) item.get("cantidad_entregada");
                if (reqNum != null)
                    sumRequested += reqNum.intValue();
                if (delNum != null)
                    sumDelivered += delNum.intValue();
            }

            if (!"CANCELADO".equals(dbEstado) && !"ANULADO".equals(dbEstado) && !"FINALIZADO".equals(dbEstado)) {
                if (sumDelivered >= sumRequested && sumRequested > 0) {
                    order.put("estado", "COMPLETADO");
                } else if ((envios != null && !envios.isEmpty()) || sumDelivered > 0 || "EN PROCESO".equals(dbEstado)
                        || "EN_PROCESO".equals(dbEstado)) {
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
            } catch (Exception ignored) {
            }
        }

        Object adjuntosObj = body.get("adjuntos");
        String storagePath = (String) body.getOrDefault("storage_path",
                "C:\\Users\\User\\OneDrive\\Escritorio\\OrdenesI");
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
        String updatedAdjuntosJson = fileStorageUtil.saveAttachedFiles(adjuntosObj, storagePath, useSubfolders,
                nroPedido);
        jdbcTemplate.update("UPDATE pedido SET nro_pedido = ?, adjuntos = ? WHERE id_pedido = ?", nroPedido,
                updatedAdjuntosJson, newId);

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

        if (body.containsKey("motivo_cancelacion")) {
            String motivo = (String) body.get("motivo_cancelacion");
            jdbcTemplate.update("UPDATE pedido SET motivo_cancelacion = ? WHERE id_pedido = ?", motivo, id);
        } else if (body.containsKey("motivo")) {
            String motivo = (String) body.get("motivo");
            jdbcTemplate.update("UPDATE pedido SET motivo_cancelacion = ? WHERE id_pedido = ?", motivo, id);
        }

        if (body.containsKey("adelantos")) {
            Object adelantosObj = body.get("adelantos");
            try {
                String adelantosJson = objectMapper.writeValueAsString(adelantosObj);
                jdbcTemplate.update("UPDATE pedido SET adelantos = ? WHERE id_pedido = ?", adelantosJson, id);
            } catch (Exception ignored) {
            }
        }

        if (body.containsKey("nro_guia")) {
            String nroGuia = (String) body.get("nro_guia");
            if (nroGuia != null && !nroGuia.trim().isEmpty()) {
                jdbcTemplate.update("UPDATE pedido SET nro_guia = ? WHERE id_pedido = ?", nroGuia.trim(), id);
            }
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

        if (estado != null && ("COMPLETADO".equalsIgnoreCase(estado.trim()) || "ENTREGADO".equalsIgnoreCase(estado.trim()))) {
            try {
                autoCompleteEnviosForPedido(id, (String) body.get("nro_guia"), fechaEntrega);
            } catch (Exception e) {
                System.err.println("Error auto-completing envios for pedido " + id + ": " + e.getMessage());
            }
        }

        body.put("id_pedido", id);
        return body;
    }

    private void autoCompleteEnviosForPedido(int idPedido, String nroGuiaInput, String fechaEntregaInput) {
        List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                "SELECT dp.*, p.nombre_producto FROM detalle_pedido dp LEFT JOIN producto p ON dp.id_producto = p.id_producto WHERE dp.id_pedido = ?",
                idPedido);
        if (detalles == null || detalles.isEmpty()) return;

        Map<String, Object> orderRow = null;
        try {
            orderRow = jdbcTemplate.queryForMap("SELECT id_cliente, nro_guia, fecha_entrega FROM pedido WHERE id_pedido = ?", idPedido);
        } catch (Exception ignored) {}

        Integer idCliente = orderRow != null ? (Integer) orderRow.get("id_cliente") : null;
        String existingGuia = orderRow != null ? (String) orderRow.get("nro_guia") : null;
        String nroGuiaFinal = (nroGuiaInput != null && !nroGuiaInput.trim().isEmpty()) ? nroGuiaInput.trim() : existingGuia;
        if (nroGuiaFinal == null || nroGuiaFinal.trim().isEmpty()) {
            nroGuiaFinal = "DESPACHO-AUTO";
        }

        String fechaEnvioFinal = (fechaEntregaInput != null && !fechaEntregaInput.trim().isEmpty())
                ? fechaEntregaInput
                : (orderRow != null && orderRow.get("fecha_entrega") != null ? orderRow.get("fecha_entrega").toString() : java.time.LocalDate.now().toString());

        List<Map<String, Object>> itemsToDeliver = new java.util.ArrayList<>();
        for (Map<String, Object> item : detalles) {
            Number idProdNum = (Number) item.get("id_producto");
            Number cantNum = (Number) item.get("cantidad");
            if (idProdNum == null) continue;
            int idProd = idProdNum.intValue();
            int cantSol = cantNum != null ? cantNum.intValue() : 0;

            Integer deliveredSoFar = 0;
            try {
                deliveredSoFar = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(de.cantidad), 0) FROM detalle_envios_pedido de INNER JOIN envios_pedido e ON de.id_envio = e.id_envio WHERE e.id_pedido = ? AND de.id_producto = ?",
                        Integer.class, idPedido, idProd);
            } catch (Exception ignored) {}

            int pending = Math.max(0, cantSol - (deliveredSoFar != null ? deliveredSoFar : 0));
            if (pending > 0) {
                Map<String, Object> toDel = new java.util.HashMap<>();
                toDel.put("id_producto", idProd);
                toDel.put("cantidad", pending);
                itemsToDeliver.add(toDel);
            }
        }

        if (!itemsToDeliver.isEmpty()) {
            final Integer finalIdCliente = idCliente;
            final String finalNroGuia = nroGuiaFinal;
            final String finalFechaEnvio = fechaEnvioFinal;

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO envios_pedido (id_pedido, id_cliente, nro_comprobante, fecha_envio, cerrar_saldo, observaciones) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, idPedido);
                if (finalIdCliente != null) ps.setInt(2, finalIdCliente); else ps.setNull(2, java.sql.Types.INTEGER);
                ps.setString(3, finalNroGuia);
                ps.setString(4, finalFechaEnvio);
                ps.setBoolean(5, true);
                ps.setString(6, "Despacho automático por Cierre de Orden a COMPLETADO");
                return ps;
            }, keyHolder);

            Number newEnvioIdNum = keyHolder.getKey();
            if (newEnvioIdNum != null) {
                int newEnvioId = newEnvioIdNum.intValue();
                for (Map<String, Object> item : itemsToDeliver) {
                    int pId = (Integer) item.get("id_producto");
                    int cant = (Integer) item.get("cantidad");
                    jdbcTemplate.update(
                            "INSERT INTO detalle_envios_pedido (id_envio, id_producto, cantidad) VALUES (?, ?, ?)",
                            newEnvioId, pId, cant);
                }
            }
        }
    }
}
