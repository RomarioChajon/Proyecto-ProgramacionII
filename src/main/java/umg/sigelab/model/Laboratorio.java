package umg.sigelab.model;

import umg.sigelab.enums.EstadoRegistro;

import java.time.LocalDateTime;

/**
 * Laboratorio = SEDE. Cada equipo se registra en un laboratorio y cada préstamo
 * se atiende en uno solo (tabla Laboratorio).
 */
public class Laboratorio {

    private int idLaboratorio;
    private String codigo;
    private String nombre;
    private String ubicacion;
    private int capacidad;
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
    private LocalDateTime fechaCreacion;

    public Laboratorio() {
    }

    public int getIdLaboratorio() { return idLaboratorio; }
    public void setIdLaboratorio(int idLaboratorio) { this.idLaboratorio = idLaboratorio; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public EstadoRegistro getEstado() { return estado; }
    public void setEstado(EstadoRegistro estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    /** Texto que muestran los ComboBox: "LAB-01 - Laboratorio de Cómputo 1". */
    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
