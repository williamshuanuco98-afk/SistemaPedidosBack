package com.inplabel.pedidos.service;

import java.util.List;
import java.util.Map;

public interface PedidoService {
    List<Map<String, Object>> getPedidos();
    Map<String, Object> addPedido(Map<String, Object> body);
    Map<String, Object> updatePedido(int id, Map<String, Object> body);
}
