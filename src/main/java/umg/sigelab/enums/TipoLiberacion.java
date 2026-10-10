package umg.sigelab.enums;

/**
 * Forma en que se liberó un bloqueo.
 * DEVOLUCION_COMPLETA es automática; MANUAL la hace un ADMIN o ENCARGADO.
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum TipoLiberacion {
    DEVOLUCION_COMPLETA("Devolución completa"),
    MANUAL("Manual");

    private final String etiqueta;

    TipoLiberacion(String etiqueta) {
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
