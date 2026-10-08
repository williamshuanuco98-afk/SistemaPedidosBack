package com.inplabel.pedidos.service;

import java.util.List;
import java.util.Map;

public interface EnvioPedidoService {
    List<Map<String, Object>> getEnviosByPedido(int idPedido);
    Map<String, Object> addEnvioPedido(Map<String, Object> body);
}
