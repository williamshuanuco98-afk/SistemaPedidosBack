package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return statusService.getStatus();
    }
}
