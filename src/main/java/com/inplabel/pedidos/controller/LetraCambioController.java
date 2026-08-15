package com.inplabel.pedidos.controller;

import com.inplabel.pedidos.util.LetraPdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/letras")
@CrossOrigin(origins = "*")
public class LetraCambioController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LetraPdfGenerator letraPdfGenerator;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLetras(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String estado) {

        StringBuilder sql = new StringBuilder("SELECT * FROM letras_cambio WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            String q = "%" + search.trim() + "%";
            sql.append("AND (nro_letra LIKE ? OR ref_girador LIKE ? OR nombre_cliente LIKE ? OR nro_documento LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append("AND fecha_giro >= ? ");
            params.add(dateFrom.trim());
        }

        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append("AND fecha_giro <= ? ");
            params.add(dateTo.trim());
        }

        if (estado != null && !estado.trim().isEmpty() && !estado.equalsIgnoreCase("ALL")) {
            sql.append("AND estado = ? ");
            params.add(estado.trim());
        }

        sql.append("ORDER BY id_letra DESC");

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/next-correlativo")
    public ResponseEntity<Map<String, Object>> getNextCorrelativo() {
        int currentYear = LocalDate.now().getYear();
        Integer maxCorrelativo = jdbcTemplate.queryForObject(
                "SELECT MAX(numero_correlativo) FROM letras_cambio WHERE anio = ?",
                Integer.class,
                currentYear
        );

        int next = (maxCorrelativo != null && maxCorrelativo > 0) ? maxCorrelativo + 1 : 1;

        Map<String, Object> resp = new HashMap<>();
        resp.put("nextCorrelativo", next);
        resp.put("anio", currentYear);
        resp.put("suggestedNroLetra", String.format("%03d-%d", next, currentYear));

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/batch")
    @Transactional
    public ResponseEntity<?> createLetrasBatch(
            @RequestBody Map<String, Object> payload,
            @RequestParam(required = false) String storageDir,
            @RequestParam(required = false, defaultValue = "true") boolean useSubfolders) {

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> letras = (List<Map<String, Object>>) payload.get("letras");
            if (letras == null || letras.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No se proporcionaron letras para registrar"));
            }

            String idLote = "LOTE-" + System.currentTimeMillis();
            List<Map<String, Object>> createdLetras = new ArrayList<>();

            for (Map<String, Object> letraData : letras) {
                int idCliente = letraData.get("id_cliente") != null ? ((Number) letraData.get("id_cliente")).intValue() : 0;
                String nombreCliente = (String) letraData.getOrDefault("nombre_cliente", "CLIENTE S.A.C.");
                String nroDocumento = (String) letraData.getOrDefault("nro_documento", "-");
                String direccionCliente = (String) letraData.getOrDefault("direccion_cliente", "LIMA");
                
                int correlativo = ((Number) letraData.getOrDefault("numero_correlativo", 1)).intValue();
                int anio = ((Number) letraData.getOrDefault("anio", LocalDate.now().getYear())).intValue();
                String nroLetra = (String) letraData.getOrDefault("nro_letra", String.format("%03d-%d", correlativo, anio));
                
                String refGirador = (String) letraData.getOrDefault("ref_girador", "-");
                String lugarGiro = (String) letraData.getOrDefault("lugar_giro", "LIMA");
                String fechaGiro = (String) letraData.getOrDefault("fecha_giro", LocalDate.now().toString());
                int diasCredito = ((Number) letraData.getOrDefault("dias_credito", 30)).intValue();
                String fechaVencimiento = (String) letraData.getOrDefault("fecha_vencimiento", LocalDate.now().plusDays(diasCredito).toString());
                
                String moneda = (String) letraData.getOrDefault("moneda", "SOLES");
                Number montoObj = (Number) letraData.getOrDefault("monto", 0.0);
                double monto = montoObj.doubleValue();
                String montoLetras = (String) letraData.getOrDefault("monto_letras", "");

                String insertSql = "INSERT INTO letras_cambio (id_lote, id_cliente, nombre_cliente, nro_documento, direccion_cliente, " +
                        "nro_letra, numero_correlativo, anio, ref_girador, lugar_giro, fecha_giro, dias_credito, " +
                        "fecha_vencimiento, moneda, monto, monto_letras, estado, fecha_creacion) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?)";

                KeyHolder keyHolder = new GeneratedKeyHolder();
                LocalDateTime now = LocalDateTime.now();

                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, idLote);
                    ps.setInt(2, idCliente);
                    ps.setString(3, nombreCliente);
                    ps.setString(4, nroDocumento);
                    ps.setString(5, direccionCliente);
                    ps.setString(6, nroLetra);
                    ps.setInt(7, correlativo);
                    ps.setInt(8, anio);
                    ps.setString(9, refGirador);
                    ps.setString(10, lugarGiro);
                    ps.setString(11, fechaGiro);
                    ps.setInt(12, diasCredito);
                    ps.setString(13, fechaVencimiento);
                    ps.setString(14, moneda);
                    ps.setDouble(15, monto);
                    ps.setString(16, montoLetras);
                    ps.setString(17, now.toString());
                    return ps;
                }, keyHolder);

                Number generatedId = keyHolder.getKey();
                int idLetra = generatedId != null ? generatedId.intValue() : 0;

                Map<String, Object> record = new HashMap<>(letraData);
                record.put("id_letra", idLetra);
                record.put("id_lote", idLote);

                // Save PDF to disk if configured
                if (storageDir != null && !storageDir.trim().isEmpty()) {
                    letraPdfGenerator.savePdfToDisk(record, storageDir, useSubfolders);
                }

                createdLetras.add(record);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id_lote", idLote);
            response.put("total_generadas", createdLetras.size());
            response.put("letras", createdLetras);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al registrar lote de letras: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> getLetraPdf(
            @PathVariable int id,
            @RequestParam(required = false) String storageDir,
            @RequestParam(required = false, defaultValue = "true") boolean useSubfolders) {

        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM letras_cambio WHERE id_letra = ?", id);
            if (list.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> letra = list.get(0);
            byte[] pdfBytes = letraPdfGenerator.generatePdfBytes(letra);

            // Auto-save to disk if storageDir passed
            if (storageDir != null && !storageDir.trim().isEmpty()) {
                letraPdfGenerator.savePdfToDisk(letra, storageDir, useSubfolders);
            }

            String filename = ((String) letra.getOrDefault("nro_letra", "LE" + id)).replaceAll("[^a-zA-Z0-9-_]", "_") + ".pdf";

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al generar PDF de Letra: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<?> anularLetra(@PathVariable int id) {
        int updated = jdbcTemplate.update("UPDATE letras_cambio SET estado = 'ANULADA' WHERE id_letra = ?", id);
        if (updated > 0) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Letra de cambio anulada correctamente"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/lote/{idLote}/anular")
    public ResponseEntity<?> anularLoteLetras(@PathVariable String idLote) {
        int updated = jdbcTemplate.update("UPDATE letras_cambio SET estado = 'ANULADA' WHERE id_lote = ?", idLote);
        return ResponseEntity.ok(Map.of("success", true, "anuladas", updated, "message", "Lote de letras anulado correctamente"));
    }
}
