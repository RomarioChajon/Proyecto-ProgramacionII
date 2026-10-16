package umg.sigelab.controller;

import umg.sigelab.enums.CondicionDevolucion;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.ModeloDisponible;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.DevolucionService;
import umg.sigelab.service.PrestamoService;

import java.time.LocalDate;
import java.util.List;

/**
 * Coordina las pantallas de préstamos y devoluciones con los Service.
 * No contiene reglas ni SQL: solo recibe lo que pide la vista y lo entrega a la capa de negocio.
 */
public class PrestamoController {

    private final PrestamoService prestamos = new PrestamoService();
    private final DevolucionService devoluciones = new DevolucionService();

    // --- Docente / estudiante ---

    public List<Laboratorio> laboratorios() {
        return prestamos.laboratoriosDisponibles();
    }

    public List<CategoriaStock> categorias(int idLaboratorio) {
        return prestamos.categorias(idLaboratorio);
    }

    public List<ModeloDisponible> modelos(int idLaboratorio, int idTipoEquipo) {
        return prestamos.modelos(idLaboratorio, idTipoEquipo);
    }

    public String motivoBloqueo(Usuario usuario) {
        return prestamos.motivoBloqueo(usuario);
    }

    public int solicitar(Usuario usuario, int idLaboratorio, int dias, List<Integer> idProductos) {
        return prestamos.solicitar(usuario, idLaboratorio, dias, idProductos);
    }

    public List<PrestamoResumen> historial(Usuario usuario) {
        return prestamos.historial(usuario);
    }

    public List<LineaPrestamo> lineas(Usuario usuario, int idPrestamo) {
        return prestamos.lineas(usuario, idPrestamo);
    }

    // --- Encargado / administrador ---

    public List<PrestamoResumen> pendientesDeDespacho(Usuario operador, int idLaboratorio) {
        return prestamos.pendientesDeDespacho(operador, idLaboratorio);
    }

    public void despachar(Usuario operador, int idPrestamo, LocalDate inicio, LocalDate fin) {
        prestamos.despachar(operador, idPrestamo, inicio, fin);
    }

    public void rechazar(Usuario operador, int idPrestamo, String motivo) {
        prestamos.rechazar(operador, idPrestamo, motivo);
    }

    public int crearYDespachar(Usuario operador, int idLaboratorio, String usuarioIdONombre, List<String> codigos,
                               LocalDate inicio, LocalDate fin) {
        return prestamos.crearYDespachar(operador, idLaboratorio, usuarioIdONombre, codigos, inicio, fin);
    }

    // --- Devoluciones ---

    public List<PrestamoResumen> prestamosActivos(Usuario operador, int idLaboratorio) {
        return devoluciones.prestamosActivos(operador, idLaboratorio);
    }

    public List<LineaPrestamo> lineasOperador(Usuario operador, int idPrestamo) {
        return devoluciones.lineas(operador, idPrestamo);
    }

    public int[] buscarPendientePorCodigo(Usuario operador, int idLaboratorio, String codigo) {
        return devoluciones.buscarPendientePorCodigo(operador, idLaboratorio, codigo);
    }

    public String devolver(Usuario operador, List<Integer> idDetalles, CondicionDevolucion condicion,
                           String observacion) {
        return devoluciones.devolver(operador, idDetalles, condicion, observacion);
    }
}
