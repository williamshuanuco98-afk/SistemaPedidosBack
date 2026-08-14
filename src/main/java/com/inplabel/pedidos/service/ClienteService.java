package com.inplabel.pedidos.service;

import java.util.List;
import java.util.Map;

public interface ClienteService {
    List<Map<String, Object>> getClientes();
    Map<String, Object> consultarSunatRuc(String ruc);
    Map<String, Object> consultarDni(String dni);
    Map<String, Object> addCliente(Map<String, Object> body);
    Map<String, Object> updateCliente(int id, Map<String, Object> body);
    Map<String, Object> deleteCliente(int id);
}
