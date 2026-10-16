package umg.sigelab.service;

import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.ResultadoMantenimiento;
import umg.sigelab.enums.TipoMantenimiento;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.InventarioRepository;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Inventario por sede, cambios de estado y mantenimiento.
 *
 * <p>Transiciones permitidas (PDF 6.9): DISPONIBLE -> MANTENIMIENTO (preventivo), DAÑADO -> MANTENIMIENTO,
 * MANTENIMIENTO -> DISPONIBLE (reparado) o BAJA (no reparable). También se puede reportar un equipo
 * DISPONIBLE como DAÑADO o darlo de BAJA. Un equipo PRESTADO solo cambia al devolverse.</p>
 */
public class InventarioService {

    private static final Map<EstadoEquipo, Set<EstadoEquipo>> TRANSICIONES = Map.of(
            EstadoEquipo.DISPONIBLE, Set.of(EstadoEquipo.MANTENIMIENTO, EstadoEquipo.DANADO, EstadoEquipo.BAJA),
            EstadoEquipo.DANADO, Set.of(EstadoEquipo.MANTENIMIENTO, EstadoEquipo.BAJA),
            EstadoEquipo.MANTENIMIENTO, Set.of(),
            EstadoEquipo.PRESTADO, Set.of(),
            EstadoEquipo.BAJA, Set.of());

    private final InventarioRepository repository = new InventarioRepository();
    private final PrestamoRepository prestamos = new PrestamoRepository();
    private final AuditoriaRepository auditoria = new AuditoriaRepository();

    public List<Laboratorio> laboratorios() {
        return Transaccion.consultar("consultar los laboratorios", prestamos::laboratoriosActivos);
    }

    public List<CategoriaStock> categorias(Usuario operador, int idLaboratorio) {
        Permisos.exigirOperador(operador, idLaboratorio);
        return Transaccion.consultar("consultar las categorías", cn -> repository.categoriasDeSede(cn, idLaboratorio));
    }

    public List<FilaInventario> listar(Usuario operador, int idLaboratorio, Integer idTipoEquipo, EstadoEquipo estado) {
        Permisos.exigirOperador(operador, idLaboratorio);
        return Transaccion.consultar("consultar el inventario",
                cn -> repository.listar(cn, idLaboratorio, idTipoEquipo, estado));
    }

    /** Estados a los que puede pasar un equipo desde el inventario. */
    public static Set<EstadoEquipo> transicionesDesde(EstadoEquipo estado) {
        return TRANSICIONES.getOrDefault(estado, Set.of());
    }

    /**
     * Ficha del equipo para la lectura de QR: datos + situación actual (disponible, reservado,
     * prestado en una orden, en mantenimiento...).
     */
    public String ficha(Usuario operador, int idLaboratorio, String codigoLeido) {
        Permisos.exigirOperador(operador, idLaboratorio);
        String codigo = limpiar(codigoLeido);
        return Transaccion.consultar("consultar el equipo", cn -> {
            FilaInventario e = repository.buscarPorCodigo(cn, codigo)
                    .orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigo + "."));
            StringBuilder sb = new StringBuilder();
            sb.append("Código: ").append(e.codigoInventario()).append('\n')
                    .append("Equipo: ").append(e.producto()).append(" (").append(nvl(e.marca())).append(' ')
                    .append(nvl(e.modelo())).append(")\n")
                    .append("Categoría: ").append(e.categoria()).append('\n')
                    .append("Serie: ").append(e.numeroSerie()).append('\n')
                    .append("Sede: ").append(e.laboratorio()).append('\n')
                    .append("Estado: ").append(e.estado().name()).append("\n\n");
            switch (e.estado()) {
                case DISPONIBLE -> {
                    Integer reserva = repository.solicitudQueReserva(cn, e.idEquipo());
                    sb.append(reserva == null ? "Disponible para préstamo."
                            : "Reservado en la solicitud #" + reserva + " (pendiente de despacho).");
                }
                case PRESTADO -> {
                    Optional<String[]> p = repository.prestamoActivoDelEquipo(cn, e.idEquipo());
                    sb.append(p.map(d -> "Prestado en la orden #" + d[0] + " a " + d[1]
                            + ". Devolución esperada: " + d[2] + ".").orElse("Prestado."));
                }
                case MANTENIMIENTO -> {
                    Optional<String[]> m = repository.mantenimientoEnProceso(cn, e.idEquipo());
                    sb.append(m.map(d -> "En mantenimiento " + d[1].toLowerCase(Locale.ROOT) + " desde " + d[3]
                            + " (responsable: " + d[2] + "). " + d[4]).orElse("En mantenimiento."));
                }
                case DANADO -> sb.append("Reportado como dañado: debe pasar por mantenimiento antes de prestarse.");
                case BAJA -> sb.append("Dado de baja: ya no se usa ni se presta.");
            }
            if (e.observaciones() != null && !e.observaciones().isBlank()) {
                sb.append("\n\nObservaciones: ").append(e.observaciones());
            }
            return sb.toString();
        });
    }

    public Optional<FilaInventario> buscarPorCodigo(Usuario operador, String codigoLeido) {
        String codigo = limpiar(codigoLeido);
        Optional<FilaInventario> fila = Transaccion.consultar("buscar el equipo",
                cn -> repository.buscarPorCodigo(cn, codigo));
        fila.ifPresent(f -> Permisos.exigirOperador(operador, f.idLaboratorio()));
        return fila;
    }

    /**
     * Cambia el estado de un equipo desde el inventario. Si el destino es MANTENIMIENTO se abre un
     * registro de mantenimiento (tipo, descripción y responsable obligatorios).
     */
    public void cambiarEstado(Usuario operador, int idEquipo, EstadoEquipo nuevo, TipoMantenimiento tipo,
                              String descripcion, String responsable) {
        Transaccion.ejecutar("cambiar el estado del equipo", cn -> {
            FilaInventario equipo = repository.buscarPorId(cn, idEquipo)
                    .orElseThrow(() -> new BusinessException("El equipo no existe."));
            Permisos.exigirOperador(operador, equipo.idLaboratorio());

            if (!transicionesDesde(equipo.estado()).contains(nuevo)) {
                throw new BusinessException("Un equipo " + equipo.estado().getEtiqueta().toLowerCase(Locale.ROOT)
                        + " no puede pasar a " + nuevo.getEtiqueta().toLowerCase(Locale.ROOT) + " desde aquí."
                        + (equipo.estado() == EstadoEquipo.PRESTADO
                        ? " Un equipo prestado cambia al registrar su devolución." : "")
                        + (equipo.estado() == EstadoEquipo.MANTENIMIENTO
                        ? " Ciérrelo desde la opción Mantenimiento (reparado o no reparable)." : ""));
            }
            Integer reserva = repository.solicitudQueReserva(cn, idEquipo);
            if (reserva != null) {
                throw new BusinessException("El equipo está reservado en la solicitud #" + reserva
                        + ". Despáchela o recházala antes de cambiar su estado.");
            }

            if (nuevo == EstadoEquipo.MANTENIMIENTO) {
                if (tipo == null) {
                    throw new BusinessException("Seleccione el tipo de mantenimiento.");
                }
                if (descripcion == null || descripcion.isBlank()) {
                    throw new BusinessException("Describa el motivo del mantenimiento.");
                }
                if (responsable == null || responsable.isBlank()) {
                    throw new BusinessException("Indique el responsable del mantenimiento.");
                }
            }

            if (!prestamos.cambiarEstadoEquipo(cn, idEquipo, equipo.estado(), nuevo)) {
                throw new BusinessException("El estado del equipo cambió mientras tanto. Actualice la lista.");
            }

            if (nuevo == EstadoEquipo.MANTENIMIENTO) {
                repository.abrirMantenimiento(cn, idEquipo, tipo, descripcion.trim(), responsable.trim(),
                        operador.getIdUsuario());
                auditoria.registrar(cn, operador.getIdUsuario(), equipo.idLaboratorio(),
                        AccionAuditoria.EQUIPO_ENVIADO_MANTENIMIENTO, "Equipo", idEquipo,
                        "Equipo " + equipo.codigoInventario() + " enviado a mantenimiento " + tipo.name()
                                + " por " + operador.getNombreUsuario() + ". " + descripcion.trim());
            } else {
                auditoria.registrar(cn, operador.getIdUsuario(), equipo.idLaboratorio(),
                        AccionAuditoria.EQUIPO_CAMBIO_ESTADO, "Equipo", idEquipo,
                        "Equipo " + equipo.codigoInventario() + ": " + equipo.estado().name() + " -> " + nuevo.name()
                                + " por " + operador.getNombreUsuario() + "."
                                + (descripcion == null || descripcion.isBlank() ? "" : " " + descripcion.trim()));
            }
            return null;
        });
    }

    /** Cierra el mantenimiento en proceso: REPARADO -> DISPONIBLE, NO_REPARABLE -> BAJA (RN-15). */
    public void finalizarMantenimiento(Usuario operador, int idEquipo, ResultadoMantenimiento resultado,
                                       String observaciones) {
        if (resultado == null) {
            throw new BusinessException("Seleccione el resultado del mantenimiento.");
        }
        Transaccion.ejecutar("finalizar el mantenimiento", cn -> {
            FilaInventario equipo = repository.buscarPorId(cn, idEquipo)
                    .orElseThrow(() -> new BusinessException("El equipo no existe."));
            Permisos.exigirOperador(operador, equipo.idLaboratorio());
            if (equipo.estado() != EstadoEquipo.MANTENIMIENTO) {
                throw new BusinessException("El equipo no está en mantenimiento.");
            }

            Optional<String[]> abierto = repository.mantenimientoEnProceso(cn, idEquipo);
            if (abierto.isEmpty()) {
                // Equipo en MANTENIMIENTO sin registro (dato antiguo): se regulariza para poder cerrarlo.
                repository.abrirMantenimiento(cn, idEquipo, TipoMantenimiento.CORRECTIVO,
                        "Registro regularizado al cerrar el mantenimiento", operador.getNombreUsuario(),
                        operador.getIdUsuario());
                abierto = repository.mantenimientoEnProceso(cn, idEquipo);
            }
            int idMantenimiento = Integer.parseInt(abierto.orElseThrow()[0]);
            if (!repository.cerrarMantenimiento(cn, idMantenimiento, resultado, observaciones, operador.getIdUsuario())) {
                throw new BusinessException("El mantenimiento ya fue cerrado.");
            }
            EstadoEquipo destino = resultado == ResultadoMantenimiento.REPARADO ? EstadoEquipo.DISPONIBLE : EstadoEquipo.BAJA;
            if (!prestamos.cambiarEstadoEquipo(cn, idEquipo, EstadoEquipo.MANTENIMIENTO, destino)) {
                throw new BusinessException("No se pudo actualizar el estado del equipo.");
            }
            auditoria.registrar(cn, operador.getIdUsuario(), equipo.idLaboratorio(),
                    AccionAuditoria.MANTENIMIENTO_FINALIZADO, "Equipo", idEquipo,
                    "Mantenimiento de " + equipo.codigoInventario() + " finalizado (" + resultado.name()
                            + "): pasa a " + destino.name() + ". Cierra " + operador.getNombreUsuario() + "."
                            + (observaciones == null || observaciones.isBlank() ? "" : " " + observaciones.trim()));
            return null;
        });
    }

    private static String limpiar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("Escriba o lea el código del equipo.");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String nvl(String texto) {
        return texto == null ? "" : texto;
    }
}
