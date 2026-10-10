package umg.sigelab.enums;

/**
 * Estado de un préstamo (tabla Prestamo).
 *
 * <p>Flujo normal: PENDIENTE -> EN_TRAMITE -> DESPACHADO -> FINALIZADO.
 * También puede terminar en RECHAZADO (el encargado lo niega) o CANCELADO (lo cancela quien lo solicitó).
 * VENCIDO NO es un estado guardado: se calcula comparando la fecha esperada de devolución con la fecha actual.</p>
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum EstadoPrestamo {
    PENDIENTE("Pendiente de revisión"),
    EN_TRAMITE("En trámite"),
    DESPACHADO("Despachado"),
    FINALIZADO("Finalizado"),
    RECHAZADO("Rechazado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPrestamo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
