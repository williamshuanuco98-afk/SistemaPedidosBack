package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.EnvioPedidoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EnvioPedidoServiceImpl implements EnvioPedidoService {

    @Autowired
    private EnvioPedidoDao envioPedidoDao;

    @Override
    public List<Map<String, Object>> getEnviosByPedido(int idPedido) {
        return envioPedidoDao.findByPedido(idPedido);
    }

    @Override
    public Map<String, Object> addEnvioPedido(Map<String, Object> body) {
        return envioPedidoDao.save(body);
    }
}
