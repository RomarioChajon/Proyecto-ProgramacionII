package SIGELAB.model;

import SIGELAB.enums.EstadoLaboratorio;

import java.time.LocalDateTime;

public class Laboratorio {

    private int idLaboratorio;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private int capacidad;
    private EstadoLaboratorio estado;
    private LocalDateTime fechaCreacion;

    public Laboratorio() {
    }

    public Laboratorio(
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
        this.estado = estado;
    }

    public Laboratorio(
            int idLaboratorio,
            String codigo,
            String nombre,
            String ubicacion,
            int capacidad,
            EstadoLaboratorio estado,
            LocalDateTime fechaCreacion
    ) {
        this.idLaboratorio = idLaboratorio;
        this.codigo = codigo;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public int getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(int idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public EstadoLaboratorio getEstado() {
        return estado;
    }

    public void setEstado(EstadoLaboratorio estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}