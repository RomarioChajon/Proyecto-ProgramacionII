package umg.sigelab.enums;

/**
 * Motivo por el cual un usuario queda bloqueado para pedir préstamos (tabla BloqueoUsuario).
 *
 * <p>IMPORTANTE: el valor que se guarda en la base de datos es {@link #name()}
 * (por ejemplo "DISPONIBLE"). {@link #toString()} devuelve la etiqueta legible
 * y se usa solo para mostrar en pantalla (ComboBox, tablas).</p>
 */
public enum MotivoBloqueo {
    EQUIPO_DANADO("Devolvió un equipo dañado"),
    VENCIDO("Préstamo vencido"),
    PARCIAL("Devolución parcial");

    private final String etiqueta;

    MotivoBloqueo(String etiqueta) {
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
