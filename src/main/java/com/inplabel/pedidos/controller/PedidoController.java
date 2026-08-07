package com.inplabel.pedidos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
            "FROM pedido p LEFT JOIN cliente c ON p.id_cliente = c.id_cliente ORDER BY p.id_pedido DESC"
        );

        for (Map<String, Object> order : pedidos) {
            Integer idPedido = (Integer) order.get("id_pedido");
            String nroPedido = (String) order.get("nro_pedido");
            if (nroPedido == null || nroPedido.isEmpty()) {
                nroPedido = String.format("PED-%04d", idPedido);
                order.put("nro_pedido", nroPedido);
            }

            List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                "SELECT d.*, pr.nombre_producto FROM detalle_pedido d " +
                "LEFT JOIN producto pr ON d.id_producto = pr.id_producto WHERE d.id_pedido = ?",
                idPedido
            );
            order.put("detalles", detalles);
        }

        return pedidos;
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public Map<String, Object> addPedido(@RequestBody Map<String, Object> body) {
        Number idClienteNum = (Number) body.get("id_cliente");
        int idCliente = idClienteNum != null ? idClienteNum.intValue() : 0;
        String fecha = (String) body.getOrDefault("fecha_pedido", "2026-08-06");
        String estado = (String) body.getOrDefault("estado", "PENDIENTE");
        List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO pedido (id_cliente, fecha_pedido, estado) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, idCliente);
            ps.setString(2, fecha);
            ps.setString(3, estado);
            return ps;
        }, keyHolder);

        Number newIdNum = keyHolder.getKey();
        int newId = newIdNum != null ? newIdNum.intValue() : 0;
        String nroPedido = String.format("PED-%04d", newId);

        jdbcTemplate.update("UPDATE pedido SET nro_pedido = ? WHERE id_pedido = ?", nroPedido, newId);

        if (detalles != null) {
            for (Map<String, Object> item : detalles) {
                Number pIdNum = (Number) item.get("id_producto");
                Number cantNum = (Number) item.get("cantidad");
                if (pIdNum != null) {
                    jdbcTemplate.update(
                        "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad) VALUES (?, ?, ?)",
                        newId, pIdNum.intValue(), cantNum != null ? cantNum.intValue() : 1
                    );
                }
            }
        }

        body.put("id_pedido", newId);
        body.put("nro_pedido", nroPedido);
        return body;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePedido(@PathVariable int id, @RequestBody Map<String, Object> body) {
        String fecha = (String) body.getOrDefault("fecha_pedido", "2026-08-06");
        String estado = (String) body.getOrDefault("estado", "PENDIENTE");

        jdbcTemplate.update(
            "UPDATE pedido SET fecha_pedido = ?, estado = ? WHERE id_pedido = ?",
            fecha, estado, id
        );

        body.put("success", true);
        body.put("id_pedido", id);
        return body;
    }
}
