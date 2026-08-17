package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @GetMapping
    public List<Map<String, Object>> getProductos() {
        return productoService.getProductos();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProductoById(@PathVariable Integer id) {
        return productoService.getProductoById(id);
    }

    @PostMapping
    public Map<String, Object> addProducto(@RequestBody Map<String, Object> body) {
        return productoService.addProducto(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProducto(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return productoService.updateProducto(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProducto(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        if (role != null && "OPERACIONES".equalsIgnoreCase(role.trim())) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Acceso denegado: El rol OPERACIONES no tiene permisos para eliminar productos.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        return ResponseEntity.ok(productoService.deleteProducto(id));
    }
}
