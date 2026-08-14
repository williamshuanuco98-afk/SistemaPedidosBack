package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.GuiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guias")
@CrossOrigin(origins = "*")
public class GuiaController {

    @Autowired
    private GuiaService guiaService;

    @GetMapping
    public List<Map<String, Object>> getGuias() {
        return guiaService.getGuias();
    }

    @GetMapping("/next-number")
    public Map<String, String> getNextNumber(@RequestParam(defaultValue = "GR001") String serie) {
        return guiaService.getNextNumber(serie);
    }

    @PostMapping
    public Map<String, Object> addGuia(@RequestBody Map<String, Object> body) {
        return guiaService.addGuia(body);
    }

    @PutMapping("/{id}/anular")
    public Map<String, Object> anularGuia(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return guiaService.anularGuia(id, body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateGuia(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return guiaService.updateGuia(id, body);
    }
}
