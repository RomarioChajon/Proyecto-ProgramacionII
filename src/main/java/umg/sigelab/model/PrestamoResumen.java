package umg.sigelab.model;

import umg.sigelab.enums.EstadoPrestamo;

import java.time.LocalDateTime;

/**
 * Una fila de préstamo para tablas (historial, órdenes pendientes, devoluciones).
 * {@code vencido} NO se guarda en la base: se calcula (RN-17: fecha esperada ya pasó y hay equipos pendientes).
 */
public record PrestamoResumen(int idPrestamo, int idSolicitante, String solicitante, String usuario,
                              int idLaboratorio, String laboratorio, LocalDateTime fechaPrestamo,
                              EstadoPrestamo estado, Integer diasSolicitados, Integer diasPrestamo,
                              LocalDateTime fechaDespacho, LocalDateTime fechaEsperada,
                              LocalDateTime fechaCierre, String motivoRechazo,
                              int total, int pendientes, boolean vencido) {

    /** Estado para mostrar: agrega VENCIDO cuando corresponde. */
    public String estadoVisible() {
        return vencido ? "Vencido" : estado.getEtiqueta();
    }
}
