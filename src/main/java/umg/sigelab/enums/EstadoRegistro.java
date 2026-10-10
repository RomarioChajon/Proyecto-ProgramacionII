package umg.sigelab.enums;

/**
 * Estado genérico de un registro con baja lógica (Usuario, Laboratorio, TipoEquipo, Producto).
 * Nunca se elimina físicamente: se cambia a INACTIVO para conservar el historial.
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum EstadoRegistro {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String etiqueta;

    EstadoRegistro(String etiqueta) {
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
