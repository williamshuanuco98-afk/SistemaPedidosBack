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
@RequestMapping("/api/guias")
@CrossOrigin(origins = "*")
public class GuiaController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getGuias() {
        List<Map<String, Object>> guias = jdbcTemplate.queryForList(
            "SELECT g.*, c.razon_social AS nombre_cliente, c.direccion AS direccion_destino, p.nro_pedido " +
            "FROM guias g LEFT JOIN cliente c ON g.id_cliente = c.id_cliente LEFT JOIN pedido p ON g.id_pedido = p.id_pedido ORDER BY g.id_guia DESC"
        );

        for (Map<String, Object> guia : guias) {
            Integer idGuia = (Integer) guia.get("id_guia");
            List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                "SELECT d.*, pr.nombre_producto FROM detalle_guias d " +
                "LEFT JOIN producto pr ON d.id_producto = pr.id_producto WHERE d.id_guia = ?",
                idGuia
            );
            guia.put("detalles", detalles);
        }

        return guias;
    }

    @PostMapping
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> addGuia(@RequestBody Map<String, Object> body) {
        Number idClienteNum = (Number) body.get("id_cliente");
        int idCliente = idClienteNum != null ? idClienteNum.intValue() : 0;
        Number idPedidoNum = (Number) body.get("id_pedido");
        Integer idPedido = idPedidoNum != null ? idPedidoNum.intValue() : null;

        String fecha = (String) body.getOrDefault("fecha_guia", LocalDate.now().toString());
        String nroGuia = (String) body.get("nro_guia");
        if (nroGuia == null || nroGuia.isEmpty()) {
            nroGuia = "G002-" + (System.currentTimeMillis() % 10000);
        }
        String estado = (String) body.getOrDefault("estado", "ACTIVA");
        List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");

        final String finalNroGuia = nroGuia;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO guias (id_cliente, id_pedido, fecha_guia, nro_guia, estado, activo) VALUES (?, ?, ?, ?, ?, TRUE)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, idCliente);
            if (idPedido != null) ps.setInt(2, idPedido); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, fecha);
            ps.setString(4, finalNroGuia);
            ps.setString(5, estado);
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

        // If linked to an order, update order nro_guia and fecha_entrega
        if (idPedido != null) {
            jdbcTemplate.update(
                "UPDATE pedido SET nro_guia = ?, fecha_entrega = ? WHERE id_pedido = ?",
                finalNroGuia, fecha, idPedido
            );
        }

        body.put("id_guia", newId);
        body.put("nro_guia", finalNroGuia);
        body.put("fecha_guia", fecha);
        return body;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateGuia(@PathVariable int id, @RequestBody Map<String, Object> body) {
        String nroGuia = (String) body.get("nro_guia");
        String estado = (String) body.getOrDefault("estado", "ACTIVA");
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
