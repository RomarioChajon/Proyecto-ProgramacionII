package umg.sigelab.enums;

/**
 * Estado de un bloqueo de usuario.
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum EstadoBloqueo {
    ACTIVO("Activo"),
    LIBERADO("Liberado");

    private final String etiqueta;

    EstadoBloqueo(String etiqueta) {
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
