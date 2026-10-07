package SIGELAB.model;

import SIGELAB.enums.EstadoProducto;

public class Producto {

    private int idProducto;
    private String codigoProducto;
    private int idTipoEquipo;
    private String nombre;
    private String marca;
    private String modelo;
    private String descripcion;
    private EstadoProducto estado;

    public Producto() {
    }

    public Producto(String codigoProducto,
                    String nombre,
                    int idTipoEquipo,
                    String marca,
                    String modelo,
                    String descripcion,
                    EstadoProducto estado) {

        this.codigoProducto = codigoProducto;
        this.nombre = nombre;
        this.idTipoEquipo = idTipoEquipo;
        this.marca = marca;
        this.modelo = modelo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Producto(int idProducto,
                    String codigoProducto,
                    String nombre,
                    int idTipoEquipo,
                    String marca,
                    String modelo,
                    String descripcion,
                    EstadoProducto estado) {

        this.idProducto = idProducto;
        this.codigoProducto = codigoProducto;
        this.nombre = nombre;
        this.idTipoEquipo = idTipoEquipo;
        this.marca = marca;
        this.modelo = modelo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public int getIdTipoEquipo() {
        return idTipoEquipo;
    }

    public void setIdTipoEquipo(int idTipoEquipo) {
        this.idTipoEquipo = idTipoEquipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoProducto getEstado() {
        return estado;
    }

    public void setEstado(EstadoProducto estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return codigoProducto + " - " + nombre;
    }
}