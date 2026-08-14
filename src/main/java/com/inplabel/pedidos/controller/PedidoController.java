package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public List<Map<String, Object>> getPedidos() {
        return pedidoService.getPedidos();
    }

    @PostMapping
    public Map<String, Object> addPedido(@RequestBody Map<String, Object> body) {
        return pedidoService.addPedido(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePedido(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return pedidoService.updatePedido(id, body);
    }
}
