package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.ProductoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoDao productoDao;

    @Override
    public List<Map<String, Object>> getProductos() {
        return productoDao.findAll();
    }

    @Override
    public Map<String, Object> getProductoById(Integer id) {
        return productoDao.findById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> addProducto(Map<String, Object> body) {
        String nombre = (String) body.getOrDefault("nombre_producto", "");
        String tipo = (String) body.getOrDefault("tipo_producto", (String) body.getOrDefault("categoria", "General"));
        return productoDao.save(nombre, tipo);
    }

    @Override
    @Transactional
    public Map<String, Object> updateProducto(Integer id, Map<String, Object> body) {
        String nombre = (String) body.getOrDefault("nombre_producto", "");
        String tipo = (String) body.getOrDefault("tipo_producto", (String) body.getOrDefault("categoria", "General"));
        return productoDao.update(id, nombre, tipo);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteProducto(Integer id) {
        boolean success = productoDao.delete(id);
        return Map.of("success", success, "id_producto", id);
    }
}
