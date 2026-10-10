package umg.sigelab.enums;

/**
 * Condición en la que regresa un equipo al devolverlo (tabla Devolucion).
 * BUENO deja el equipo DISPONIBLE (RN-12); DANADO lo deja DANADO (RN-13).
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum CondicionDevolucion {
    BUENO("Buen estado"),
    DANADO("Dañado");

    private final String etiqueta;

    CondicionDevolucion(String etiqueta) {
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
