
package SIGELAB.model;

import SIGELAB.enums.EstadoTipoEquipo;

public class TipoEquipo {

    private int idTipoEquipo;
    private String codigo;
    private String nombre;
    private String descripcion;
    private EstadoTipoEquipo estado;

    public TipoEquipo() {
    }

    // Constructor original: se conserva por compatibilidad.
    public TipoEquipo(
            String nombre,
            String descripcion,
            EstadoTipoEquipo estado) {

        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    // Constructor original: se conserva por compatibilidad.
    public TipoEquipo(
            int idTipoEquipo,
            String nombre,
            String descripcion,
            EstadoTipoEquipo estado) {

        this.idTipoEquipo = idTipoEquipo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    // Nuevo constructor con código.
    public TipoEquipo(
            int idTipoEquipo,
            String codigo,
            String nombre,
            String descripcion,
            EstadoTipoEquipo estado) {

        this.idTipoEquipo = idTipoEquipo;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdTipoEquipo() {
        return idTipoEquipo;
    }

    public void setIdTipoEquipo(int idTipoEquipo) {
        this.idTipoEquipo = idTipoEquipo;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoTipoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoTipoEquipo estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
