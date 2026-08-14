package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.PedidoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PedidoServiceImpl implements PedidoService {

    @Autowired
    private PedidoDao pedidoDao;

    @Override
    public List<Map<String, Object>> getPedidos() {
        return pedidoDao.findAll();
    }

    @Override
    @Transactional
    public Map<String, Object> addPedido(Map<String, Object> body) {
        return pedidoDao.save(body);
    }

    @Override
    @Transactional
    public Map<String, Object> updatePedido(int id, Map<String, Object> body) {
        return pedidoDao.update(id, body);
    }
}
