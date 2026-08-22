package com.inplabel.pedidos.dao;

import java.util.List;
import java.util.Map;

public interface PedidoDao {
    List<Map<String, Object>> findAll();

    Map<String, Object> save(Map<String, Object> body);

    Map<String, Object> update(int id, Map<String, Object> body);
}
