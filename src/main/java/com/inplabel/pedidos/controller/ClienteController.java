package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public Map<String, Object> consultarSunatRuc(@PathVariable("ruc") String ruc) {
        return clienteService.consultarSunatRuc(ruc);
    }

    @GetMapping("/dni/{dni}")
    public Map<String, Object> consultarDni(@PathVariable("dni") String dni) {
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
    public ResponseEntity<?> deleteCliente(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        if (role != null && "OPERACIONES".equalsIgnoreCase(role.trim())) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Acceso denegado: El rol OPERACIONES no tiene permisos para eliminar clientes.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        return ResponseEntity.ok(clienteService.deleteCliente(id));
    }
}
