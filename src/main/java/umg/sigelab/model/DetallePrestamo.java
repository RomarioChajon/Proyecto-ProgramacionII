
package umg.sigelab.model;

/**
 * Representa un equipo incluido en un préstamo.
 * Corresponde a la tabla dbo.DetallePrestamo.
 */
public class DetallePrestamo {

    private int idDetalle;
    private int idPrestamo;
    private int idEquipo;
    private int idLaboratorio;

    public DetallePrestamo() {
    }

    public DetallePrestamo(
            int idPrestamo,
            int idEquipo,
            int idLaboratorio) {

        this.idPrestamo = idPrestamo;
        this.idEquipo = idEquipo;
        this.idLaboratorio = idLaboratorio;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(int idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public int getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
    }

    public int getIdLaboratorio() {
        return idLaboratorio;
    }

    public void setIdLaboratorio(int idLaboratorio) {
        this.idLaboratorio = idLaboratorio;
    }
}
