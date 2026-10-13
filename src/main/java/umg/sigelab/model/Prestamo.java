
package umg.sigelab.model;

import umg.sigelab.enums.EstadoPrestamo;

import java.time.LocalDateTime;

public class Prestamo {

    private int idPrestamo;
    private int idSolicitante;
    private int idLaboratorio;
    private LocalDateTime fechaPrestamo;
    private int idUsuarioRegistra;

    private Integer idUsuarioRevisa;
    private LocalDateTime fechaRevision;

    private Integer diasPrestamo;
    private LocalDateTime fechaDespacho;
    private LocalDateTime fechaEsperadaDevolucion;
    private Integer idUsuarioDespacha;

    private LocalDateTime fechaCierre;

    private EstadoPrestamo estado = EstadoPrestamo.PENDIENTE;
    private String motivoRechazo;
    private String observaciones;

    public Prestamo() {
    }

    public int getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(int idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public int getIdSolicitante() {
        return idSolicitante;
    }

    public void setIdSolicitante(int idSolicitante) {
        this.idSolicitante = idSolicitante;
    }

    public int getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(int idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }

    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public int getIdUsuarioRegistra() {
        return idUsuarioRegistra;
    }

    public void setIdUsuarioRegistra(int idUsuarioRegistra) {
        this.idUsuarioRegistra = idUsuarioRegistra;
    }

    public Integer getIdUsuarioRevisa() {
        return idUsuarioRevisa;
    }

    public void setIdUsuarioRevisa(Integer idUsuarioRevisa) {
        this.idUsuarioRevisa = idUsuarioRevisa;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public Integer getDiasPrestamo() {
        return diasPrestamo;
    }

    public void setDiasPrestamo(Integer diasPrestamo) {
        this.diasPrestamo = diasPrestamo;
    }

    public LocalDateTime getFechaDespacho() {
        return fechaDespacho;
    }

    public void setFechaDespacho(LocalDateTime fechaDespacho) {
        this.fechaDespacho = fechaDespacho;
    }

    public LocalDateTime getFechaEsperadaDevolucion() {
        return fechaEsperadaDevolucion;
    }

    public void setFechaEsperadaDevolucion(
            LocalDateTime fechaEsperadaDevolucion) {
        this.fechaEsperadaDevolucion = fechaEsperadaDevolucion;
    }

    public Integer getIdUsuarioDespacha() {
        return idUsuarioDespacha;
    }

    public void setIdUsuarioDespacha(Integer idUsuarioDespacha) {
        this.idUsuarioDespacha = idUsuarioDespacha;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamo estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
