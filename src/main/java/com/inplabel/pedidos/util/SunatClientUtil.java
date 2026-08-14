package com.inplabel.pedidos.util;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class SunatClientUtil {

    public Map<String, Object> consultarRuc(String ruc) {
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
                
                String dir = (String) body.get("direccion");
                String distrito = (String) body.get("distrito");
                String provincia = (String) body.get("provincia");
                String departamento = (String) body.get("departamento");

                if (dir != null && !dir.isEmpty()) {
                    if (distrito != null && !distrito.isEmpty() && !dir.contains(distrito)) {
                        dir = dir.trim() + " - " + (departamento != null ? departamento : "") + " - " + (provincia != null ? provincia : "") + " - " + distrito;
                    }
                }
                response.put("direccion", dir);
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

    public Map<String, Object> consultarDni(String dni) {
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
}
