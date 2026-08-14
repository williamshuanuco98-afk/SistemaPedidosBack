package com.inplabel.pedidos.model;

public class GuiaDetalle {
    private Integer idDetalleGuia;
    private Integer idGuia;
    private Integer idProducto;
    private String nombreProducto;
    private String codigoProducto;
    private Integer cantidad;

    public GuiaDetalle() {}

    public Integer getIdDetalleGuia() { return idDetalleGuia; }
    public void setIdDetalleGuia(Integer idDetalleGuia) { this.idDetalleGuia = idDetalleGuia; }

    public Integer getIdGuia() { return idGuia; }
    public void setIdGuia(Integer idGuia) { this.idGuia = idGuia; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
