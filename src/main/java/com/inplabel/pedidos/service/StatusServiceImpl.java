package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.StatusDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StatusServiceImpl implements StatusService {

    @Autowired
    private StatusDao statusDao;

    @Override
    public Map<String, Object> getStatus() {
        return statusDao.getStatus();
    }
}
