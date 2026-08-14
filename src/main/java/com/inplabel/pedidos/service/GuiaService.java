package com.inplabel.pedidos.service;

import java.util.List;
import java.util.Map;

public interface GuiaService {
    List<Map<String, Object>> getGuias();
    Map<String, String> getNextNumber(String serie);
    Map<String, Object> addGuia(Map<String, Object> body);
    Map<String, Object> anularGuia(int id, Map<String, Object> body);
    Map<String, Object> updateGuia(int id, Map<String, Object> body);
}
