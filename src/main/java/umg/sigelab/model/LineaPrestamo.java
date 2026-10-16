package umg.sigelab.model;

import umg.sigelab.enums.CondicionDevolucion;
import umg.sigelab.enums.EstadoEquipo;

import java.time.LocalDateTime;

/** Un equipo dentro de un préstamo (DetallePrestamo) con su devolución, si ya ocurrió. */
public record LineaPrestamo(int idDetalle, int idPrestamo, int idEquipo, String codigoInventario,
                            String producto, String marca, String modelo, String categoria,
                            EstadoEquipo estadoEquipo, CondicionDevolucion condicion,
                            LocalDateTime fechaDevolucion, String observacion) {

    public boolean devuelto() {
        return condicion != null;
    }
}
