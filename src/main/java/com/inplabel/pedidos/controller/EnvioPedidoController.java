package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.EnvioPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/envios-pedido")
public class EnvioPedidoController {

    @Autowired
    private EnvioPedidoService envioPedidoService;

    @GetMapping("/pedido/{idPedido}")
    public List<Map<String, Object>> getEnviosByPedido(@PathVariable int idPedido) {
        return envioPedidoService.getEnviosByPedido(idPedido);
    }

    @PostMapping
    public Map<String, Object> addEnvioPedido(@RequestBody Map<String, Object> body) {
        return envioPedidoService.addEnvioPedido(body);
    }
}
