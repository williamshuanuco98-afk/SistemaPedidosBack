package com.inplabel.pedidos.dao;

import java.util.List;
import java.util.Map;

public interface EnvioPedidoDao {
    List<Map<String, Object>> findByPedido(int idPedido);
    Map<String, Object> save(Map<String, Object> body);
}
