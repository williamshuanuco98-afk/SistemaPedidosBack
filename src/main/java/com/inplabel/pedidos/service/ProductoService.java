package com.inplabel.pedidos.service;

import java.util.List;
import java.util.Map;

public interface ProductoService {
    List<Map<String, Object>> getProductos();
    Map<String, Object> getProductoById(Integer id);
    Map<String, Object> addProducto(Map<String, Object> body);
    Map<String, Object> updateProducto(Integer id, Map<String, Object> body);
    Map<String, Object> deleteProducto(Integer id);
}
