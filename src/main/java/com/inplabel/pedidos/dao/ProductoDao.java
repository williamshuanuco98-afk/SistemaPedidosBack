package com.inplabel.pedidos.dao;

import java.util.List;
import java.util.Map;

public interface ProductoDao {
    List<Map<String, Object>> findAll();
    Map<String, Object> findById(Integer id);
    Map<String, Object> save(String nombreProducto, String tipoProducto);
    Map<String, Object> update(Integer id, String nombreProducto, String tipoProducto);
    boolean delete(Integer id);
}
