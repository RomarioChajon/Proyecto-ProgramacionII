package umg.sigelab.enums;

/**
 * Resultado al cerrar un mantenimiento.
 * REPARADO devuelve el equipo a DISPONIBLE; NO_REPARABLE lo da de BAJA.
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum ResultadoMantenimiento {
    REPARADO("Reparado"),
    NO_REPARABLE("No reparable");

    private final String etiqueta;

    ResultadoMantenimiento(String etiqueta) {
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
