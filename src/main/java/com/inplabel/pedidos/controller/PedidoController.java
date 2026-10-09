package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import com.inplabel.pedidos.model.Usuario;
import com.inplabel.pedidos.security.ApiSecurityFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Set;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public List<Map<String, Object>> getPedidos(HttpServletRequest request) {
        Usuario user = (Usuario) request.getAttribute(ApiSecurityFilter.CURRENT_USER);
        List<Map<String,Object>> orders = pedidoService.getPedidos();
        if (!ApiSecurityFilter.has(user, "pedidos.finances")) {
            orders = orders.stream().map(order -> {
                Map<String,Object> copy = new java.util.HashMap<>(order);
                copy.remove("adelantos"); return copy;
            }).toList();
        }
        return orders;
    }

    @PostMapping
    public Map<String, Object> addPedido(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Usuario user = (Usuario) request.getAttribute(ApiSecurityFilter.CURRENT_USER);
        if (!"PENDIENTE".equals(body.getOrDefault("estado", "PENDIENTE"))) require(user, "pedidos.finish");
        if (body.get("adelantos") instanceof java.util.Collection<?> advances && !advances.isEmpty()) require(user, "pedidos.finances");
        return pedidoService.addPedido(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePedido(@PathVariable("id") int id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Usuario user = (Usuario) request.getAttribute(ApiSecurityFilter.CURRENT_USER);
        if (body.containsKey("adelantos")) require(user, "pedidos.finances");
        if (body.containsKey("estado")) {
            String status = String.valueOf(body.get("estado")).trim().toUpperCase(java.util.Locale.ROOT);
            if (Set.of("CANCELADO", "ANULADO").contains(status)) require(user, "pedidos.cancel");
            else if (Set.of("COMPLETADO", "ENTREGADO", "FINALIZADO").contains(status)) require(user, "pedidos.finish");
            else require(user, "pedidos.edit");
        }
        String target = String.valueOf(body.getOrDefault("estado", "")).toUpperCase(java.util.Locale.ROOT);
        String action = Set.of("FINALIZADO", "COMPLETADO", "ENTREGADO").contains(target) ? "pedidos.finish"
                : Set.of("CANCELADO", "ANULADO").contains(target) ? "pedidos.cancel" : "pedidos.edit";
        if (body.containsKey("motivo") || body.containsKey("motivo_cancelacion")) require(user, action);
        if (body.containsKey("fecha_pedido")) require(user, action);
        if (body.containsKey("nro_guia") || body.containsKey("fecha_entrega")) require(user, action);
        if (body.isEmpty()) require(user, "pedidos.edit");
        return pedidoService.updatePedido(id, body);
    }
    private static void require(Usuario user, String... permissions) {
        if (!ApiSecurityFilter.has(user, permissions)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permiso insuficiente");
    }
}
