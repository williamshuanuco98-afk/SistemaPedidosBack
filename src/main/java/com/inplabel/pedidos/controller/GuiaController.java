package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.service.GuiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    @Autowired
    private GuiaService guiaService;

    @GetMapping
    public List<Map<String, Object>> getGuias() {
        return guiaService.getGuias();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGuiaById(@PathVariable("id") String idStr) {
        try {
            int id = Integer.parseInt(idStr);
            Map<String, Object> guia = guiaService.getGuiaById(id);
            if (guia != null) return ResponseEntity.ok(guia);
            return ResponseEntity.notFound().build();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de guía inválido: " + idStr));
        }
    }

    @GetMapping("/next-number")
    public Map<String, String> getNextNumber(@RequestParam(name = "serie", defaultValue = "GR001") String serie) {
        return guiaService.getNextNumber(serie);
    }

    @PostMapping
    public Map<String, Object> addGuia(@RequestBody Map<String, Object> body) {
        return guiaService.addGuia(body);
    }

    @PutMapping("/{id}/anular")
    public Map<String, Object> anularGuia(@PathVariable("id") int id, @RequestBody Map<String, Object> body) {
        return guiaService.anularGuia(id, body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateGuia(@PathVariable("id") int id, @RequestBody Map<String, Object> body) {
        return guiaService.updateGuia(id, body);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> getGuiaPdf(
            @PathVariable("id") String idStr,
            @RequestParam(name = "storageDir", required = false) String storageDir,
            @RequestParam(name = "useSubfolders", required = false) Boolean useSubfolders) {
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ID de guía no válido: " + idStr);
        }
        byte[] pdfBytes = guiaService.generatePdf(id, null, useSubfolders);
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> guia = guiaService.getGuiaById(id);
        String nroGuia = guia != null ? (String) guia.getOrDefault("nro_guia", "GR001-0001") : "GR001-0001";
        String filename = "GUIA_" + nroGuia + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/pdf/save")
    public Map<String, Object> saveGuiaPdf(
            @PathVariable("id") int id,
            @RequestBody(required = false) Map<String, Object> body) {
        String storageDir = body != null ? (String) body.get("storage_path") : null;
        Boolean useSub = body != null && body.containsKey("use_subfolders")
                ? Boolean.valueOf(String.valueOf(body.get("use_subfolders")))
                : null;
        return guiaService.savePdf(id, storageDir, useSub);
    }
}
