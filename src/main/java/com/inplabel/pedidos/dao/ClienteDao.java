package com.inplabel.pedidos.dao;

import java.util.List;
import java.util.Map;

public interface ClienteDao {
    List<Map<String, Object>> findAll();
    Map<String, Object> save(String tipoDocumento, String nroDocumento, String razonSocial, String direccion);
    Map<String, Object> update(int id, String tipoDocumento, String nroDocumento, String razonSocial, String direccion);
    boolean delete(int id);
}
