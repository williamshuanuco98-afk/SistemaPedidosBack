package com.inplabel.pedidos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getPedidos() {
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
                Integer idProd = (Integer) item.get("id_producto");
                int totalDelivered = 0;
                for (Map<String, Object> g : guias) {
                    List<Map<String, Object>> gDetalles = (List<Map<String, Object>>) g.get("detalles");
                    if (gDetalles != null) {
                        for (Map<String, Object> gd : gDetalles) {
                            Integer gProdId = (Integer) gd.get("id_producto");
                            if (gProdId != null && gProdId.equals(idProd)) {
                                Number cant = (Number) gd.get("cantidad");
                                if (cant != null)
                                    totalDelivered += cant.intValue();
                            }
                        }
                    }
                }
                item.put("cantidad_entregada", totalDelivered);
            }

            order.put("detalles", detalles);
            order.put("guias", guias);
        }

        return pedidos;
    }

    @PostMapping
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> addPedido(@RequestBody Map<String, Object> body) {
        Number idClienteNum = (Number) body.get("id_cliente");
        int idCliente = idClienteNum != null ? idClienteNum.intValue() : 0;
        String fecha = (String) body.getOrDefault("fecha_pedido", LocalDate.now().toString());
        String estado = (String) body.getOrDefault("estado", "PENDIENTE");
        String nroOrden = (String) body.getOrDefault("nro_orden_compra", "");
        String observaciones = (String) body.getOrDefault("observaciones", "");
        String establecimiento = (String) body.getOrDefault("establecimiento", "COMAS");

        Object adjuntosObj = body.get("adjuntos");
        String adjuntosJson = "";
        if (adjuntosObj != null) {
            try {
                adjuntosJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(adjuntosObj);
            } catch (Exception ignored) {
            }
        }

        final String finalNroOrden = nroOrden;
        final String finalAdjuntos = adjuntosJson;
        final String finalObs = observaciones;
        final String finalEstab = establecimiento;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO pedido (id_cliente, fecha_pedido, estado, nro_orden, adjuntos, observaciones, establecimiento) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idCliente);
            ps.setString(2, fecha);
            ps.setString(3, estado);
            ps.setString(4, finalNroOrden);
            ps.setString(5, finalAdjuntos);
            ps.setString(6, finalObs);
            ps.setString(7, finalEstab);
            return ps;
        }, keyHolder);

        Number newIdNum = keyHolder.getKey();
        int newId = newIdNum != null ? newIdNum.intValue() : 0;
        String nroPedido = String.format("PED-%04d", newId);

        // Process disk file storage for attached PDFs
        String storagePath = (String) body.getOrDefault("storage_path", "C:\\Users\\User\\OneDrive\\Escritorio\\OrdenesI");
        boolean useSubfolders = Boolean.TRUE.equals(body.get("use_subfolders"));

        if (adjuntosObj instanceof List) {
            List<Map<String, Object>> filesList = (List<Map<String, Object>>) adjuntosObj;
            for (Map<String, Object> f : filesList) {
                String fileName = (String) f.get("name");
                String base64Data = (String) f.get("data");

                if (fileName != null && base64Data != null && base64Data.contains(",")) {
                    try {
                        String base64Content = base64Data.substring(base64Data.indexOf(",") + 1);
                        byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Content);

                        java.io.File targetDir = new java.io.File(storagePath);
                        if (useSubfolders) {
                            targetDir = new java.io.File(targetDir, nroPedido);
                        }
                        if (!targetDir.exists()) {
                            targetDir.mkdirs();
                        }

                        java.io.File outFile = new java.io.File(targetDir, fileName);
                        java.nio.file.Files.write(outFile.toPath(), decodedBytes);
                        f.put("saved_path", outFile.getAbsolutePath());
                    } catch (Exception ex) {
                        System.err.println("Error al guardar archivo en disco: " + ex.getMessage());
                    }
                }
            }
            try {
                String updatedAdjuntosJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(filesList);
                jdbcTemplate.update("UPDATE pedido SET nro_pedido = ?, adjuntos = ? WHERE id_pedido = ?", nroPedido, updatedAdjuntosJson, newId);
            } catch (Exception e) {
                jdbcTemplate.update("UPDATE pedido SET nro_pedido = ? WHERE id_pedido = ?", nroPedido, newId);
            }
        } else {
            jdbcTemplate.update("UPDATE pedido SET nro_pedido = ? WHERE id_pedido = ?", nroPedido, newId);
        }

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
        return body;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePedido(@PathVariable int id, @RequestBody Map<String, Object> body) {
        String fecha = (String) body.getOrDefault("fecha_pedido", LocalDate.now().toString());
        String estado = (String) body.getOrDefault("estado", "PENDIENTE");

        jdbcTemplate.update(
                "UPDATE pedido SET fecha_pedido = ?, estado = ? WHERE id_pedido = ?",
                fecha, estado, id);

        body.put("success", true);
        body.put("id_pedido", id);
        return body;
    }
}
