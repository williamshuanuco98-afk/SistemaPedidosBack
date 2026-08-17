package com.inplabel.pedidos.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class SunatClientUtil {

    @Value("${decolecta.api.token:}")
    private String decolectaToken;

    @Value("${sunat.api.token:}")
    private String sunatToken;

    private RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    private String getEffectiveToken() {
        if (decolectaToken != null && !decolectaToken.trim().isEmpty()) {
            return decolectaToken.trim();
        }
        if (sunatToken != null && !sunatToken.trim().isEmpty()) {
            return sunatToken.trim();
        }
        return "";
    }

    public Map<String, Object> consultarRuc(String ruc) {
        Map<String, Object> response = new HashMap<>();
        if (ruc == null || ruc.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "El número de RUC no puede estar vacío");
            return response;
        }

        String cleanRuc = ruc.replaceAll("[^0-9]", "").trim();
        if (cleanRuc.length() != 11) {
            response.put("success", false);
            response.put("message", "El RUC debe tener 11 dígitos");
            return response;
        }

        String token = getEffectiveToken();
        RestTemplate restTemplate = createRestTemplate(3500);

        // 1. Intentar Decolecta API - Endpoint estándar (/v1/sunat/ruc)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "InplabelSistemaPedidos/1.0");
            if (!token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://api.decolecta.com/v1/sunat/ruc?numero=" + cleanRuc;
            ResponseEntity<Map> apiRes = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (apiRes.getStatusCode().is2xxSuccessful() && apiRes.getBody() != null) {
                Map body = apiRes.getBody();
                Map<String, Object> parsed = parseDecolectaRucResponse(cleanRuc, body);
                if (parsed != null) return parsed;
            }
        } catch (Exception e) {
            // Intento con siguiente endpoint
        }

        // 2. Intentar Decolecta API - Endpoint extendido (/v1/sunat/ruc/full)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "InplabelSistemaPedidos/1.0");
            if (!token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://api.decolecta.com/v1/sunat/ruc/full?numero=" + cleanRuc;
            ResponseEntity<Map> apiRes = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (apiRes.getStatusCode().is2xxSuccessful() && apiRes.getBody() != null) {
                Map body = apiRes.getBody();
                Map<String, Object> parsed = parseDecolectaRucResponse(cleanRuc, body);
                if (parsed != null) return parsed;
            }
        } catch (Exception e) {
            // Intento con fallback APIs
        }

        // 3. Fallback: apis.net.pe
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            if (!token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://api.apis.net.pe/v1/ruc?numero=" + cleanRuc;
            ResponseEntity<Map> apiRes = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (apiRes.getStatusCode().is2xxSuccessful() && apiRes.getBody() != null) {
                Map body = apiRes.getBody();
                if (body.containsKey("nombre") || body.containsKey("razonSocial") || body.containsKey("razon_social")) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("nro_documento", cleanRuc);
                    
                    String nombre = (String) (body.get("nombre") != null ? body.get("nombre") :
                                    body.get("razonSocial") != null ? body.get("razonSocial") : body.get("razon_social"));
                    result.put("nombre_cliente", nombre);

                    String dir = (String) (body.get("direccion") != null ? body.get("direccion") : body.get("domicilio_fiscal"));
                    String dist = (String) body.get("distrito");
                    String prov = (String) body.get("provincia");
                    String dpto = (String) body.get("departamento");

                    if (dir != null && !dir.isEmpty() && dist != null && !dist.isEmpty() && !dir.toUpperCase().contains(dist.toUpperCase())) {
                        dir = dir.trim() + " - " + (dpto != null ? dpto : "") + " - " + (prov != null ? prov : "") + " - " + dist;
                    }
                    result.put("direccion", dir != null ? dir.trim() : "");
                    result.put("estado", body.getOrDefault("estado", "ACTIVO"));
                    result.put("condicion", body.getOrDefault("condicion", "HABIDO"));
                    return result;
                }
            }
        } catch (Exception e) {
            // Siguiente fallback
        }

        // 4. Fallback: apiperu.dev
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            if (!token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://apiperu.dev/api/ruc/" + cleanRuc;
            ResponseEntity<Map> apiRes = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (apiRes.getStatusCode().is2xxSuccessful() && apiRes.getBody() != null) {
                Map body = apiRes.getBody();
                Map data = body.containsKey("data") && body.get("data") instanceof Map ? (Map) body.get("data") : body;
                if (data != null && (data.containsKey("nombre_o_razon_social") || data.containsKey("razon_social") || data.containsKey("nombre"))) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("nro_documento", cleanRuc);
                    result.put("nombre_cliente", data.getOrDefault("nombre_o_razon_social", data.getOrDefault("razon_social", data.get("nombre"))));
                    result.put("direccion", data.getOrDefault("direccion_completa", data.getOrDefault("direccion", "")));
                    result.put("estado", data.getOrDefault("estado", "ACTIVO"));
                    result.put("condicion", data.getOrDefault("condicion", "HABIDO"));
                    return result;
                }
            }
        } catch (Exception e) {
            // Final
        }

        response.put("success", false);
        response.put("message", "No se encontró información para el RUC especificado.");
        return response;
    }

    private Map<String, Object> parseDecolectaRucResponse(String cleanRuc, Map body) {
        if (body == null) return null;
        
        // Decolecta can return data directly or inside "data"
        Map data = (body.containsKey("data") && body.get("data") instanceof Map) ? (Map) body.get("data") : body;
        
        String nombre = null;
        if (data.containsKey("razon_social") && data.get("razon_social") != null) {
            nombre = data.get("razon_social").toString();
        } else if (data.containsKey("razonSocial") && data.get("razonSocial") != null) {
            nombre = data.get("razonSocial").toString();
        } else if (data.containsKey("nombre_o_razon_social") && data.get("nombre_o_razon_social") != null) {
            nombre = data.get("nombre_o_razon_social").toString();
        } else if (data.containsKey("nombre") && data.get("nombre") != null) {
            nombre = data.get("nombre").toString();
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("nro_documento", cleanRuc);
        result.put("nombre_cliente", nombre.trim());

        String dir = "";
        if (data.containsKey("direccion") && data.get("direccion") != null) {
            dir = data.get("direccion").toString().trim();
        } else if (data.containsKey("domicilio_fiscal") && data.get("domicilio_fiscal") != null) {
            dir = data.get("domicilio_fiscal").toString().trim();
        } else if (data.containsKey("direccion_completa") && data.get("direccion_completa") != null) {
            dir = data.get("direccion_completa").toString().trim();
        }

        String distrito = data.get("distrito") != null ? data.get("distrito").toString().trim() : "";
        String provincia = data.get("provincia") != null ? data.get("provincia").toString().trim() : "";
        String departamento = data.get("departamento") != null ? data.get("departamento").toString().trim() : "";

        if (!dir.isEmpty() && !distrito.isEmpty() && !dir.toUpperCase().contains(distrito.toUpperCase())) {
            StringBuilder ubigeoStr = new StringBuilder();
            if (!departamento.isEmpty()) ubigeoStr.append(departamento).append(" - ");
            if (!provincia.isEmpty()) ubigeoStr.append(provincia).append(" - ");
            ubigeoStr.append(distrito);
            dir = dir + " - " + ubigeoStr.toString();
        }

        result.put("direccion", dir);
        result.put("distrito", distrito);
        result.put("provincia", provincia);
        result.put("departamento", departamento);
        result.put("ubigeo", data.getOrDefault("ubigeo", ""));
        result.put("estado", data.getOrDefault("estado", "ACTIVO"));
        result.put("condicion", data.getOrDefault("condicion", "HABIDO"));

        return result;
    }

    public Map<String, Object> consultarDni(String dni) {
        Map<String, Object> response = new HashMap<>();
        if (dni == null || dni.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "El número de DNI no puede estar vacío");
            return response;
        }

        String cleanDni = dni.replaceAll("[^0-9]", "").trim();
        if (cleanDni.length() != 8) {
            response.put("success", false);
            response.put("message", "El DNI debe tener 8 dígitos");
            return response;
        }

        String token = getEffectiveToken();
        RestTemplate restTemplate = createRestTemplate(3500);

        // 1. Intentar Decolecta RENIEC DNI
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "InplabelSistemaPedidos/1.0");
            if (!token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://api.decolecta.com/v1/reniec/dni?numero=" + cleanDni;
            ResponseEntity<Map> apiRes = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (apiRes.getStatusCode().is2xxSuccessful() && apiRes.getBody() != null) {
                Map body = apiRes.getBody();
                Map data = (body.containsKey("data") && body.get("data") instanceof Map) ? (Map) body.get("data") : body;
                
                String nombres = data.get("nombres") != null ? data.get("nombres").toString() : "";
                String apPaterno = data.get("apellido_paterno") != null ? data.get("apellido_paterno").toString() : 
                                   data.get("apellidoPaterno") != null ? data.get("apellidoPaterno").toString() : "";
                String apMaterno = data.get("apellido_materno") != null ? data.get("apellido_materno").toString() : 
                                   data.get("apellidoMaterno") != null ? data.get("apellidoMaterno").toString() : "";
                
                String nombreCompleto = data.get("nombre_completo") != null ? data.get("nombre_completo").toString() :
                                        data.get("nombre") != null ? data.get("nombre").toString() :
                                        (nombres + " " + apPaterno + " " + apMaterno).trim();

                if (!nombreCompleto.isEmpty()) {
                    response.put("success", true);
                    response.put("nro_documento", cleanDni);
                    response.put("nombre_cliente", nombreCompleto);
                    response.put("nombres", nombres);
                    response.put("apellidoPaterno", apPaterno);
                    response.put("apellidoMaterno", apMaterno);
                    return response;
                }
            }
        } catch (Exception e) {
            // Intento con fallback
        }

        // 2. Fallback apis.net.pe
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            if (!token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://api.apis.net.pe/v1/dni?numero=" + cleanDni;
            ResponseEntity<Map> apiRes = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (apiRes.getStatusCode().is2xxSuccessful() && apiRes.getBody() != null) {
                Map body = apiRes.getBody();
                if (body.containsKey("nombre") || body.containsKey("nombres")) {
                    response.put("success", true);
                    response.put("nro_documento", cleanDni);
                    response.put("nombre_cliente", body.getOrDefault("nombre", body.get("nombres")));
                    response.put("nombres", body.get("nombres"));
                    response.put("apellidoPaterno", body.get("apellidoPaterno"));
                    response.put("apellidoMaterno", body.get("apellidoMaterno"));
                    return response;
                }
            }
        } catch (Exception e) {
            // Final
        }

        response.put("success", false);
        response.put("message", "No se encontró información para el DNI especificado.");
        return response;
    }
}

