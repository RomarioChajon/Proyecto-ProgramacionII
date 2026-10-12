
package SIGELAB.model;

import SIGELAB.enums.EstadoEquipo;
import java.time.LocalDateTime;

public class Equipo {

    private int idEquipo;
    private String codigoInventario;
    private int idProducto;
    private String numeroSerie;
    private int idLaboratorio;
    private EstadoEquipo estado;
    private String observaciones;
    private LocalDateTime fechaRegistro;

    public Equipo() {
    }

    public Equipo(String codigoInventario,
                  int idProducto,
                  String numeroSerie,
                  int idLaboratorio,
                  EstadoEquipo estado,
                  String observaciones) {

        this.codigoInventario = codigoInventario;
        this.idProducto = idProducto;
        this.numeroSerie = numeroSerie;
        this.idLaboratorio = idLaboratorio;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Equipo(int idEquipo,
                  String codigoInventario,
                  int idProducto,
                  String numeroSerie,
                  int idLaboratorio,
                  EstadoEquipo estado,
                  String observaciones,
                  LocalDateTime fechaRegistro) {

        this(codigoInventario, idProducto, numeroSerie,
                idLaboratorio, estado, observaciones);

        this.idEquipo = idEquipo;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
    }

    public String getCodigoInventario() {
        return codigoInventario;
    }

    public void setCodigoInventario(String codigoInventario) {
        this.codigoInventario = codigoInventario;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public int getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(int idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return codigoInventario + " - " + numeroSerie;
    }
}
