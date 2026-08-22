package com.inplabel.pedidos.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class SunatClientUtil {

    @Value("${decolecta.api.token:}")
    private String decolectaToken;

    @Value("${sunat.api.token:}")
    private String sunatToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
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
        HttpClient client = createHttpClient();

        // 1. Intentar apis.net.pe v1 (Público y rápido)
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.apis.net.pe/v1/ruc?numero=" + cleanRuc))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET();

            if (!token.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> httpRes = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpRes.statusCode() == 200 && httpRes.body() != null && !httpRes.body().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = objectMapper.readValue(httpRes.body(), Map.class);
                Map<String, Object> parsed = parseApisNetPeRuc(cleanRuc, bodyMap);
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (Throwable t) {
            System.err.println("Advertencia apis.net.pe RUC: " + t.getMessage());
        }

        // 2. Intentar Decolecta API (/v1/sunat/ruc)
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.decolecta.com/v1/sunat/ruc?numero=" + cleanRuc))
                    .header("User-Agent", "InplabelSistemaPedidos/1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET();

            if (!token.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> httpRes = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpRes.statusCode() == 200 && httpRes.body() != null && !httpRes.body().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = objectMapper.readValue(httpRes.body(), Map.class);
                Map<String, Object> parsed = parseDecolectaRuc(cleanRuc, bodyMap);
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (Throwable t) {
            System.err.println("Advertencia Decolecta RUC: " + t.getMessage());
        }

        // 3. Fallback: apiperu.dev
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://apiperu.dev/api/ruc/" + cleanRuc))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET();

            if (!token.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> httpRes = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpRes.statusCode() == 200 && httpRes.body() != null && !httpRes.body().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = objectMapper.readValue(httpRes.body(), Map.class);
                Map data = bodyMap.containsKey("data") && bodyMap.get("data") instanceof Map ? (Map) bodyMap.get("data") : bodyMap;
                if (data != null && (data.containsKey("nombre_o_razon_social") || data.containsKey("razon_social") || data.containsKey("nombre"))) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("nro_documento", cleanRuc);
                    result.put("nombre_cliente", String.valueOf(data.getOrDefault("nombre_o_razon_social", data.getOrDefault("razon_social", data.get("nombre")))));
                    result.put("direccion", String.valueOf(data.getOrDefault("direccion_completa", data.getOrDefault("direccion", ""))));
                    result.put("estado", String.valueOf(data.getOrDefault("estado", "ACTIVO")));
                    result.put("condicion", String.valueOf(data.getOrDefault("condicion", "HABIDO")));
                    return result;
                }
            }
        } catch (Throwable t) {
            System.err.println("Advertencia ApiPeru RUC: " + t.getMessage());
        }

        response.put("success", false);
        response.put("message", "No se encontró información para el RUC especificado.");
        return response;
    }

    private Map<String, Object> parseApisNetPeRuc(String cleanRuc, Map<String, Object> body) {
        if (body == null) return null;

        Object nameObj = body.get("nombre");
        if (nameObj == null) nameObj = body.get("razonSocial");
        if (nameObj == null) nameObj = body.get("razon_social");

        if (nameObj == null || nameObj.toString().trim().isEmpty()) {
            return null;
        }

        String nombre = nameObj.toString().trim();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("nro_documento", cleanRuc);
        result.put("nombre_cliente", nombre);

        Object dirObj = body.get("direccion");
        if (dirObj == null) dirObj = body.get("domicilio_fiscal");
        String dir = dirObj != null ? dirObj.toString().trim() : "";

        String dist = body.get("distrito") != null ? body.get("distrito").toString().trim() : "";
        String prov = body.get("provincia") != null ? body.get("provincia").toString().trim() : "";
        String dpto = body.get("departamento") != null ? body.get("departamento").toString().trim() : "";

        if (!dir.isEmpty() && !dist.isEmpty() && !dir.toUpperCase().contains(dist.toUpperCase())) {
            StringBuilder ubigeoStr = new StringBuilder();
            if (!dpto.isEmpty()) ubigeoStr.append(dpto).append(" - ");
            if (!prov.isEmpty()) ubigeoStr.append(prov).append(" - ");
            ubigeoStr.append(dist);
            dir = dir + " - " + ubigeoStr.toString();
        }

        result.put("direccion", dir);
        result.put("distrito", dist);
        result.put("provincia", prov);
        result.put("departamento", dpto);
        result.put("estado", String.valueOf(body.getOrDefault("estado", "ACTIVO")));
        result.put("condicion", String.valueOf(body.getOrDefault("condicion", "HABIDO")));

        return result;
    }

    private Map<String, Object> parseDecolectaRuc(String cleanRuc, Map<String, Object> body) {
        if (body == null) return null;

        Map data = (body.containsKey("data") && body.get("data") instanceof Map) ? (Map) body.get("data") : body;

        Object nameObj = data.get("razon_social");
        if (nameObj == null) nameObj = data.get("razonSocial");
        if (nameObj == null) nameObj = data.get("nombre_o_razon_social");
        if (nameObj == null) nameObj = data.get("nombre");

        if (nameObj == null || nameObj.toString().trim().isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("nro_documento", cleanRuc);
        result.put("nombre_cliente", nameObj.toString().trim());

        Object dirObj = data.get("direccion");
        if (dirObj == null) dirObj = data.get("domicilio_fiscal");
        if (dirObj == null) dirObj = data.get("direccion_completa");
        String dir = dirObj != null ? dirObj.toString().trim() : "";

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
        result.put("estado", String.valueOf(data.getOrDefault("estado", "ACTIVO")));
        result.put("condicion", String.valueOf(data.getOrDefault("condicion", "HABIDO")));

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
        HttpClient client = createHttpClient();

        // 1. Intentar apis.net.pe v1 DNI (Público y rápido)
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.apis.net.pe/v1/dni?numero=" + cleanDni))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET();

            if (!token.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> httpRes = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpRes.statusCode() == 200 && httpRes.body() != null && !httpRes.body().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(httpRes.body(), Map.class);
                Object nameObj = body.get("nombre");
                if (nameObj == null) nameObj = body.get("nombres");

                if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                    response.put("success", true);
                    response.put("nro_documento", cleanDni);
                    response.put("nombre_cliente", nameObj.toString().trim());
                    response.put("nombres", String.valueOf(body.getOrDefault("nombres", "")));
                    response.put("apellidoPaterno", String.valueOf(body.getOrDefault("apellidoPaterno", "")));
                    response.put("apellidoMaterno", String.valueOf(body.getOrDefault("apellidoMaterno", "")));
                    return response;
                }
            }
        } catch (Throwable t) {
            System.err.println("Advertencia apis.net.pe DNI: " + t.getMessage());
        }

        // 2. Intentar Decolecta RENIEC DNI
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.decolecta.com/v1/reniec/dni?numero=" + cleanDni))
                    .header("User-Agent", "InplabelSistemaPedidos/1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET();

            if (!token.isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> httpRes = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpRes.statusCode() == 200 && httpRes.body() != null && !httpRes.body().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(httpRes.body(), Map.class);
                Map data = (body.containsKey("data") && body.get("data") instanceof Map) ? (Map) body.get("data") : body;

                String nombres = data.get("nombres") != null ? data.get("nombres").toString() : "";
                String apPaterno = data.get("apellido_paterno") != null ? data.get("apellido_paterno").toString()
                        : data.get("apellidoPaterno") != null ? data.get("apellidoPaterno").toString() : "";
                String apMaterno = data.get("apellido_materno") != null ? data.get("apellido_materno").toString()
                        : data.get("apellidoMaterno") != null ? data.get("apellidoMaterno").toString() : "";

                String nombreCompleto = data.get("nombre_completo") != null ? data.get("nombre_completo").toString()
                        : data.get("nombre") != null ? data.get("nombre").toString()
                                : (nombres + " " + apPaterno + " " + apMaterno).trim();

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
        } catch (Throwable t) {
            System.err.println("Advertencia Decolecta DNI: " + t.getMessage());
        }

        response.put("success", false);
        response.put("message", "No se encontró información para el DNI especificado.");
        return response;
    }
}
