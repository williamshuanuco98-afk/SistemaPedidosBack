package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.ClienteDao;
import com.inplabel.pedidos.util.SunatClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteDao clienteDao;

    @Autowired
    private SunatClientUtil sunatClientUtil;

    @Override
    public List<Map<String, Object>> getClientes() {
        return clienteDao.findAll();
    }

    @Override
    public Map<String, Object> consultarSunatRuc(String ruc) {
        return sunatClientUtil.consultarRuc(ruc);
    }

    @Override
    public Map<String, Object> consultarDni(String dni) {
        return sunatClientUtil.consultarDni(dni);
    }

    @Override
    @Transactional
    public Map<String, Object> addCliente(Map<String, Object> body) {
        String tipoDoc = (String) body.getOrDefault("tipo_documento", "RUC");
        String nroDoc = (String) body.getOrDefault("nro_documento", "");
        String nombre = (String) body.getOrDefault("nombre_cliente", "");
        String direccion = (String) body.getOrDefault("direccion", "");

        return clienteDao.save(tipoDoc, nroDoc, nombre, direccion);
    }

    @Override
    @Transactional
    public Map<String, Object> updateCliente(int id, Map<String, Object> body) {
        String tipoDoc = (String) body.getOrDefault("tipo_documento", "RUC");
        String nroDoc = (String) body.getOrDefault("nro_documento", "");
        String nombre = (String) body.getOrDefault("nombre_cliente", "");
        String direccion = (String) body.getOrDefault("direccion", "");

        return clienteDao.update(id, tipoDoc, nroDoc, nombre, direccion);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteCliente(int id) {
        boolean success = clienteDao.delete(id);
        return Map.of("success", success, "id_cliente", id);
    }
}
