package umg.sigelab.service;

import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.model.FilaAuditoria;
import umg.sigelab.model.FilaBloqueo;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.TablaReporte;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.ConsultaRepository;
import umg.sigelab.repository.InventarioRepository;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dashboard, reportes, bloqueos y auditoría (PDF 6.10 y 6.11). */
public class ConsultaService {

    private final ConsultaRepository repository = new ConsultaRepository();
    private final PrestamoRepository prestamos = new PrestamoRepository();
    private final AuditoriaRepository auditoria = new AuditoriaRepository();
    private final InventarioRepository inventario = new InventarioRepository();

    /** Totales para el dashboard: equipos por estado, préstamos activos y vencidos. */
    public Map<String, Integer> dashboard(Usuario operador, Integer idLaboratorio) {
        exigir(operador, idLaboratorio);
        return Transaccion.consultar("consultar el dashboard", cn -> {
            Map<String, Integer> equipos = repository.equiposPorEstado(cn, idLaboratorio);
            Map<String, Integer> prestamos = repository.prestamosPorEstado(cn, idLaboratorio);
            Map<String, Integer> resultado = new LinkedHashMap<>();
            int total = equipos.values().stream().mapToInt(Integer::intValue).sum();
            resultado.put("Total de equipos", total);
            resultado.put("Disponibles", equipos.get("DISPONIBLE"));
            resultado.put("Prestados", equipos.get("PRESTADO"));
            resultado.put("Dañados", equipos.get("DANADO"));
            resultado.put("En mantenimiento", equipos.get("MANTENIMIENTO"));
            resultado.put("De baja", equipos.get("BAJA"));
            resultado.put("Solicitudes pendientes", prestamos.getOrDefault("PENDIENTE", 0));
            resultado.put("Préstamos activos", prestamos.getOrDefault("DESPACHADO", 0));
            resultado.put("Préstamos vencidos", repository.contarVencidos(cn, idLaboratorio));
            resultado.put("Préstamos finalizados", prestamos.getOrDefault("FINALIZADO", 0));
            return resultado;
        });
    }

    public List<PrestamoResumen> vencidos(Usuario operador, Integer idLaboratorio) {
        exigir(operador, idLaboratorio);
        return Transaccion.consultar("consultar los préstamos vencidos", cn -> prestamos.vencidos(cn, idLaboratorio));
    }

    public List<FilaBloqueo> bloqueos(Usuario operador, boolean soloActivos) {
        exigir(operador, null);
        return Transaccion.consultar("consultar los bloqueos", cn -> repository.bloqueos(cn, soloActivos));
    }

    /** Libera un bloqueo manualmente (solo administrador) y lo deja en auditoría. */
    public void liberarBloqueo(Usuario admin, FilaBloqueo bloqueo) {
        Permisos.exigirAdmin(admin);
        Transaccion.ejecutar("liberar el bloqueo", cn -> {
            if (!repository.liberarManual(cn, bloqueo.idBloqueo(), admin.getIdUsuario())) {
                throw new BusinessException("El bloqueo ya estaba liberado.");
            }
            auditoria.registrar(cn, admin.getIdUsuario(), null, AccionAuditoria.BLOQUEO_LIBERADO, "BloqueoUsuario",
                    bloqueo.idBloqueo(), "Bloqueo #" + bloqueo.idBloqueo() + " de " + bloqueo.usuario()
                            + " liberado manualmente por " + admin.getNombreUsuario() + ".");
            return null;
        });
    }

    public List<FilaAuditoria> auditoria(Usuario operador, Integer idLaboratorio) {
        exigir(operador, idLaboratorio);
        return Transaccion.consultar("consultar la auditoría", cn -> auditoria.listar(cn, idLaboratorio, 500));
    }


    // --- Consultas del PDF 6.11: equipos por estado, préstamos entre fechas e historiales ---

    /** Equipos por estado (y categoría) de una sede; ADMIN puede pedir todas las sedes con idLaboratorio null. */
    public TablaReporte equiposPorEstado(Usuario operador, Integer idLaboratorio, EstadoEquipo estado,
                                         Integer idTipoEquipo) {
        exigir(operador, idLaboratorio);
        return Transaccion.consultar("consultar los equipos por estado", cn -> repository.equiposDetallePorEstado(
                cn, idLaboratorio, estado == null ? null : estado.name(), idTipoEquipo));
    }

    /**
     * Préstamos entre dos fechas. grupo: TODOS, PENDIENTES, ACTIVOS, VENCIDOS, FINALIZADOS o NO_ATENDIDOS.
     */
    public TablaReporte prestamosEntreFechas(Usuario operador, Integer idLaboratorio, String grupo,
                                             LocalDate desde, LocalDate hasta) {
        exigir(operador, idLaboratorio);
        if (desde == null || hasta == null) {
            throw new BusinessException("Indique la fecha inicial y la fecha final.");
        }
        if (hasta.isBefore(desde)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        return Transaccion.consultar("consultar los préstamos", cn -> repository.prestamosEntreFechas(
                cn, idLaboratorio, grupo, desde, hasta));
    }

    /** Historial de préstamos de un equipo (por su código de inventario / QR). */
    public TablaReporte historialDeEquipo(Usuario operador, String codigo) {
        String limpio = codigoObligatorio(codigo);
        return Transaccion.consultar("consultar el historial del equipo", cn -> {
            exigirEquipoAccesible(cn, operador, limpio);
            return repository.historialDeEquipo(cn, limpio);
        });
    }

    /** Historial de mantenimientos de una sede, o de un equipo concreto si se indica su código. */
    public TablaReporte historialMantenimiento(Usuario operador, Integer idLaboratorio, String codigo) {
        String limpio = codigo == null || codigo.isBlank() ? null : codigo.trim();
        return Transaccion.consultar("consultar el historial de mantenimiento", cn -> {
            if (limpio != null) {
                exigirEquipoAccesible(cn, operador, limpio);
            } else {
                exigir(operador, idLaboratorio);
            }
            return repository.historialMantenimiento(cn, limpio == null ? idLaboratorio : null, limpio);
        });
    }

    private static String codigoObligatorio(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("Escriba el código de inventario del equipo.");
        }
        return codigo.trim();
    }

    /** El equipo debe existir y pertenecer a una sede sobre la que el usuario tenga permiso. */
    private void exigirEquipoAccesible(java.sql.Connection cn, Usuario operador, String codigo)
            throws java.sql.SQLException {
        FilaInventario fila = inventario.buscarPorCodigo(cn, codigo)
                .orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigo + "."));
        Permisos.exigirOperador(operador, fila.idLaboratorio());
    }

    /** ADMIN ve todo; ENCARGADO solo su sede (idLaboratorio obligatorio y propio). */
    private void exigir(Usuario operador, Integer idLaboratorio) {
        if (idLaboratorio == null) {
            Permisos.exigirAdmin(operador);
        } else {
            Permisos.exigirOperador(operador, idLaboratorio);
        }
    }
}
