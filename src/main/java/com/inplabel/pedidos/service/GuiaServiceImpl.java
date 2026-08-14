package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.GuiaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class GuiaServiceImpl implements GuiaService {

    @Autowired
    private GuiaDao guiaDao;

    @Override
    public List<Map<String, Object>> getGuias() {
        return guiaDao.findAll();
    }

    @Override
    public Map<String, String> getNextNumber(String serie) {
        return guiaDao.getNextNumber(serie);
    }

    @Override
    @Transactional
    public Map<String, Object> addGuia(Map<String, Object> body) {
        return guiaDao.save(body);
    }

    @Override
    @Transactional
    public Map<String, Object> anularGuia(int id, Map<String, Object> body) {
        String motivo = (String) body.getOrDefault("motivo_anulacion", "Anulado por el usuario");
        return guiaDao.anular(id, motivo);
    }

    @Override
    @Transactional
    public Map<String, Object> updateGuia(int id, Map<String, Object> body) {
        return guiaDao.update(id, body);
    }
}
