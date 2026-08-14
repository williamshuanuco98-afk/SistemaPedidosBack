package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public List<Map<String, Object>> getClientes() {
        return clienteService.getClientes();
    }

    @GetMapping("/sunat/{ruc}")
    public Map<String, Object> consultarSunatRuc(@PathVariable String ruc) {
        return clienteService.consultarSunatRuc(ruc);
    }

    @GetMapping("/dni/{dni}")
    public Map<String, Object> consultarDni(@PathVariable String dni) {
        return clienteService.consultarDni(dni);
    }

    @PostMapping
    public Map<String, Object> addCliente(@RequestBody Map<String, Object> body) {
        return clienteService.addCliente(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCliente(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return clienteService.updateCliente(id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCliente(@PathVariable int id) {
        return clienteService.deleteCliente(id);
    }
}
