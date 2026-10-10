package umg.sigelab.view;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.util.Collection;

/** Ayudas para llenar y mostrar los ComboBox de las pantallas. */
public final class ComboBoxUtils {

    private ComboBoxUtils() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Llena el combo con un primer elemento VACÍO (null) seguido de los valores.
     * El elemento vacío se muestra como {@code textoVacio} (por ejemplo "-- Seleccione --"),
     * lo que obliga al usuario a elegir y permite validar "selección válida" (null = no eligió).
     */
    public static <T> void llenarConPlaceholder(JComboBox<T> combo, Collection<? extends T> valores, String textoVacio) {
        combo.removeAllItems();
        combo.addItem(null);
        for (T valor : valores) {
            combo.addItem(valor);
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean seleccionado, boolean conFoco) {
                return super.getListCellRendererComponent(lista, valor == null ? textoVacio : valor,
                        indice, seleccionado, conFoco);
            }
        });
        combo.setSelectedItem(null);
    }
}
