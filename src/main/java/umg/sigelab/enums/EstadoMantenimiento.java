package umg.sigelab.enums;

/**
 * Estado de un mantenimiento (tabla Mantenimiento).
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum EstadoMantenimiento {
    EN_PROCESO("En proceso"),
    FINALIZADO("Finalizado");

    private final String etiqueta;

    EstadoMantenimiento(String etiqueta) {
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
