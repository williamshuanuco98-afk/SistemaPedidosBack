package com.inplabel.pedidos.model;

public class Producto {
    private Integer idProducto;
    private String nombreProducto;
    private String tipoProducto;
    private String categoria;

    public Producto() {}

    public Producto(Integer idProducto, String nombreProducto, String tipoProducto) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.tipoProducto = tipoProducto;
        this.categoria = (tipoProducto != null && !tipoProducto.isEmpty()) ? tipoProducto : "General";
    }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getTipoProducto() { return tipoProducto; }
    public void setTipoProducto(String tipoProducto) {
        this.tipoProducto = tipoProducto;
        this.categoria = (tipoProducto != null && !tipoProducto.isEmpty()) ? tipoProducto : "General";
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
