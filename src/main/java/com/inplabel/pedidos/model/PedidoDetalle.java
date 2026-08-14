package com.inplabel.pedidos.model;

public class PedidoDetalle {
    private Integer idDetalle;
    private Integer idPedido;
    private Integer idProducto;
    private String nombreProducto;
    private Integer cantidad;
    private Integer cantidadEntregada;

    public PedidoDetalle() {}

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }

    public Integer getIdPedido() { return idPedido; }
    public void setIdPedido(Integer idPedido) { this.idPedido = idPedido; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Integer getCantidadEntregada() { return cantidadEntregada; }
    public void setCantidadEntregada(Integer cantidadEntregada) { this.cantidadEntregada = cantidadEntregada; }
}
