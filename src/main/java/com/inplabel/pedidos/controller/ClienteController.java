package com.inplabel.pedidos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public List<Map<String, Object>> getClientes() {
        return jdbcTemplate.queryForList(
                "SELECT id_cliente, tipo_documento, nro_documento, razon_social AS nombre_cliente, direccion FROM cliente ORDER BY id_cliente DESC");
    }

    @GetMapping("/sunat/{ruc}")
    public Map<String, Object> consultarSunatRuc(@PathVariable String ruc) {
        Map<String, Object> response = new HashMap<>();
        if (ruc == null || ruc.trim().isEmpty()) {
            response.put("success", false);
            return response;
        }

        String cleanRuc = ruc.trim();

        // Attempt Primary API
        try {
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            String url = "https://api.apis.net.pe/v1/ruc?numero=" + cleanRuc;
            org.springframework.http.ResponseEntity<Map> apiRes = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class);

            if (apiRes.getBody() != null && apiRes.getBody().containsKey("nombre")) {
                Map body = apiRes.getBody();
                response.put("success", true);
                response.put("nro_documento", cleanRuc);
                response.put("nombre_cliente", body.get("nombre"));
                response.put("direccion", body.get("direccion"));
                response.put("estado", body.getOrDefault("estado", "ACTIVO"));
                response.put("condicion", body.getOrDefault("condicion", "HABIDO"));
                return response;
            }
        } catch (Exception e) {
            response.put("primary_error", e.getMessage());
        }

        // Attempt Secondary Fallback API
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://apiperu.dev/api/ruc/" + cleanRuc;
            Map body = restTemplate.getForObject(url, Map.class);
            if (body != null && body.containsKey("data")) {
                Map data = (Map) body.get("data");
                if (data != null && data.containsKey("nombre_o_razon_social")) {
                    response.put("success", true);
                    response.put("nro_documento", cleanRuc);
                    response.put("nombre_cliente", data.get("nombre_o_razon_social"));
                    response.put("direccion",
                            data.getOrDefault("direccion_completa", data.getOrDefault("direccion", "")));
                    response.put("estado", data.getOrDefault("estado", "ACTIVO"));
                    response.put("condicion", data.getOrDefault("condicion", "HABIDO"));
                    return response;
                }
            }
        } catch (Exception e) {
            response.put("fallback_error", e.getMessage());
        }

        response.put("success", false);
        return response;
    }

    @GetMapping("/dni/{dni}")
    public Map<String, Object> consultarDni(@PathVariable String dni) {
        Map<String, Object> response = new HashMap<>();
        if (dni == null || dni.trim().isEmpty()) {
            response.put("success", false);
            return response;
        }

        String cleanDni = dni.trim();

        try {
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "application/json");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            String url = "https://api.apis.net.pe/v1/dni?numero=" + cleanDni;
            org.springframework.http.ResponseEntity<Map> apiRes = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class);

            if (apiRes.getBody() != null && apiRes.getBody().containsKey("nombre")) {
                Map body = apiRes.getBody();
                response.put("success", true);
                response.put("nro_documento", cleanDni);
                response.put("nombre_cliente", body.get("nombre"));
                response.put("nombres", body.get("nombres"));
                response.put("apellidoPaterno", body.get("apellidoPaterno"));
                response.put("apellidoMaterno", body.get("apellidoMaterno"));
                return response;
            }
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        response.put("success", false);
        return response;
    }

    @PostMapping
    public Map<String, Object> addCliente(@RequestBody Map<String, Object> body) {
        String tipoDoc = (String) body.getOrDefault("tipo_documento", "RUC");
        String nroDoc = (String) body.getOrDefault("nro_documento", "");
        String nombre = (String) body.getOrDefault("nombre_cliente", "");
        String direccion = (String) body.getOrDefault("direccion", "");

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO cliente (tipo_documento, nro_documento, razon_social, direccion) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tipoDoc);
            ps.setString(2, nroDoc);
            ps.setString(3, nombre);
            ps.setString(4, direccion);
            return ps;
        }, keyHolder);

        Number newId = keyHolder.getKey();
        body.put("id_cliente", newId != null ? newId.intValue() : 0);
        return body;
    }
}
