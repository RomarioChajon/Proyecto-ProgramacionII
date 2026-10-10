package umg.sigelab.enums;

/**
 * Tipo de mantenimiento (tabla Mantenimiento).
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum TipoMantenimiento {
    PREVENTIVO("Preventivo"),
    CORRECTIVO("Correctivo");

    private final String etiqueta;

    TipoMantenimiento(String etiqueta) {
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
