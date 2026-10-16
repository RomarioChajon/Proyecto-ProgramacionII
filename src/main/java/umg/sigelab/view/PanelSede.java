package umg.sigelab.view;

import com.toedter.calendar.JDateChooser;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Supplier;

/**
 * Base de las secciones del encargado (órdenes, inventario, mantenimiento, préstamo, devoluciones).
 * Cada sección trabaja SIEMPRE sobre una sede: la del encargado, o la que elija el administrador.
 */
abstract class PanelSede extends JPanel {

    protected final Usuario operador;
    private final Supplier<Laboratorio> proveedorSede;

    PanelSede(Usuario operador, Supplier<Laboratorio> proveedorSede) {
        super(new BorderLayout(8, 8));
        this.operador = operador;
        this.proveedorSede = proveedorSede;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    /** Sede con la que se trabaja ahora (puede ser null si el administrador aún no elige una). */
    protected Laboratorio sede() {
        return proveedorSede.get();
    }

    /** Sede o aviso: devuelve null (y avisa) si no hay sede seleccionada. */
    protected Laboratorio sedeObligatoria() {
        Laboratorio sede = sede();
        if (sede == null) {
            Mensajes.advertir(this, "Sede", "Seleccione primero un laboratorio / sede.");
        }
        return sede;
    }

    /** Vuelve a leer los datos de la sede actual. */
    abstract void refrescar();

    // --- calendario ---

    static JDateChooser calendario(LocalDate inicial, LocalDate minima) {
        JDateChooser c = new JDateChooser();
        c.setDateFormatString("dd/MM/yyyy");
        c.setDate(aDate(inicial));
        if (minima != null) {
            c.setMinSelectableDate(aDate(minima));
        }
        return c;
    }

    static Date aDate(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    static LocalDate aLocalDate(Date fecha) {
        return fecha == null ? null : fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
