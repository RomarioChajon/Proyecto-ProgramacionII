package umg.sigelab.enums;

/**
 * Estado de una unidad física de equipo (tabla Equipo).
 *
 * <p>Se escribe DANADO (sin Ñ) para que coincida exactamente con el CHECK de la base de datos
 * y evitar problemas de codificación; la etiqueta sí muestra "Dañado".</p>
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum EstadoEquipo {
    DISPONIBLE("Disponible"),
    PRESTADO("Prestado"),
    DANADO("Dañado"),
    MANTENIMIENTO("En mantenimiento"),
    BAJA("De baja");

    private final String etiqueta;

    EstadoEquipo(String etiqueta) {
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
