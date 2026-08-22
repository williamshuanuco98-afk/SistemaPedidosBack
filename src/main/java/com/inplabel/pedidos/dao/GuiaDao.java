package com.inplabel.pedidos.dao;

import java.util.List;
import java.util.Map;

public interface GuiaDao {
    List<Map<String, Object>> findAll();

    Map<String, Object> findById(int id);

    Map<String, String> getNextNumber(String serie);

    Map<String, Object> save(Map<String, Object> body);

    Map<String, Object> anular(int id, String motivo);

    Map<String, Object> update(int id, Map<String, Object> body);
}
