package com.inplabel.pedidos.model;

public class Cliente {
    private Integer idCliente;
    private String tipoDocumento;
    private String nroDocumento;
    private String nombreCliente;
    private String direccion;

    public Cliente() {}

    public Cliente(Integer idCliente, String tipoDocumento, String nroDocumento, String nombreCliente, String direccion) {
        this.idCliente = idCliente;
        this.tipoDocumento = tipoDocumento;
        this.nroDocumento = nroDocumento;
        this.nombreCliente = nombreCliente;
        this.direccion = direccion;
    }

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNroDocumento() { return nroDocumento; }
    public void setNroDocumento(String nroDocumento) { this.nroDocumento = nroDocumento; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
