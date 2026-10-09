package com.inplabel.pedidos.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Datos inválidos. Revise los campos y los adjuntos."));
    }
    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    public ResponseEntity<?> unavailable(RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Error procesando la solicitud", e);
        return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "No se pudo completar la operación. Consulte con el administrador."));
    }
}
