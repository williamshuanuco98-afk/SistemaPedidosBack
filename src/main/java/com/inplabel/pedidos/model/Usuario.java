package com.inplabel.pedidos.model;

import java.time.LocalDateTime;

public class Usuario {
    private Integer idUsuario;
    private String username;
    private String password;
    private String salt;
    private String nombreCompleto;
    private String rol;
    private Boolean activo;
    private LocalDateTime createdAt;

    public Usuario() {}

    public Usuario(Integer idUsuario, String username, String password, String salt, String nombreCompleto, String rol, Boolean activo, LocalDateTime createdAt) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.activo = activo;
        this.createdAt = createdAt;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
