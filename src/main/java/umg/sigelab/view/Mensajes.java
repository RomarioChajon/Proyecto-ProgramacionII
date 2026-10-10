package umg.sigelab.view;

import umg.sigelab.util.BusinessException;
import umg.sigelab.util.DataAccessException;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cuadros de diálogo comunes a TODAS las pantallas, para que los mensajes se vean siempre igual
 * y el manejo de errores esté en un solo lugar.
 */
public final class Mensajes {

    private static final Logger LOG = Logger.getLogger(Mensajes.class.getName());

    private Mensajes() {
        // Clase de utilidades: no se instancia.
    }

    public static void informar(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void advertir(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    /** Pregunta Sí/No. Devuelve true si el usuario elige Sí. */
    public static boolean confirmar(Component padre, String titulo, String pregunta) {
        return JOptionPane.showConfirmDialog(padre, pregunta, titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Muestra una excepción al usuario SIN exponer detalles técnicos:
     * <ul>
     *   <li>{@link BusinessException}: regla de negocio -> advertencia con el mensaje.</li>
     *   <li>{@link DataAccessException}: problema con la base de datos -> error con el mensaje amigable.</li>
     *   <li>Cualquier otra: se registra en el log y se muestra un mensaje genérico.</li>
     * </ul>
     */
    public static void mostrarExcepcion(Component padre, String titulo, Throwable e) {
        if (e instanceof BusinessException) {
            advertir(padre, titulo, e.getMessage());
        } else if (e instanceof DataAccessException) {
            error(padre, titulo, e.getMessage());
        } else {
            LOG.log(Level.SEVERE, "Error inesperado en la pantalla " + titulo, e);
            error(padre, titulo, "Ocurrió un error inesperado. Intente de nuevo o contacte al administrador.");
        }
    }
}
