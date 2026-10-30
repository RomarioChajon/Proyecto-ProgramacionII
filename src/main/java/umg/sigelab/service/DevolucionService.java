package umg.sigelab.service;

import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.enums.CondicionDevolucion;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.InventarioRepository;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import umg.sigelab.integration.sms.TextBeeSmsService;

/**
 * Reglas de las DEVOLUCIONES (parciales o totales).
 *
 * <ul>
 *   <li>RN-10: un equipo solo se devuelve si está pendiente dentro de un préstamo activo.</li>
 *   <li>RN-11: un equipo no puede devolverse dos veces.</li>
 *   <li>RN-12 / RN-13: BUENO -> DISPONIBLE; DAÑADO -> DAÑADO.</li>
 *   <li>RN-16: el préstamo sigue activo mientras quede un equipo pendiente; al devolver el último
 *       pasa a FINALIZADO.</li>
 *   <li>Devolución parcial: se crea un bloqueo PARCIAL; el usuario no puede pedir otro préstamo
 *       hasta devolver todo. Al completar la devolución, el bloqueo se libera.</li>
 * </ul>
 */
public class DevolucionService {

    private final PrestamoRepository repository = new PrestamoRepository();
    private final InventarioRepository inventario = new InventarioRepository();
    private final AuditoriaRepository auditoria = new AuditoriaRepository();
    private final TextBeeSmsService smsService = new TextBeeSmsService();

    /** Préstamos entregados de la sede con equipos pendientes (incluye vencidos). */
    public List<PrestamoResumen> prestamosActivos(Usuario operador, int idLaboratorio) {
        Permisos.exigirOperador(operador, idLaboratorio);
        return Transaccion.consultar("consultar los préstamos activos",
                cn -> repository.activosConPendientes(cn, idLaboratorio));
    }

    public List<LineaPrestamo> lineas(Usuario operador, int idPrestamo) {
        return new PrestamoService().lineas(operador, idPrestamo);
    }

    /**
     * Busca, a partir del código de un equipo (lectura de QR), la línea pendiente de devolución.
     *
     * @return {idDetalle, idPrestamo}
     */
    public int[] buscarPendientePorCodigo(Usuario operador, int idLaboratorio, String codigoLeido) {
        Permisos.exigirOperador(operador, idLaboratorio);
        String codigo = limpiarCodigo(codigoLeido);
        return Transaccion.consultar("buscar el equipo", cn -> {
            FilaInventario equipo = inventario.buscarPorCodigo(cn, codigo)
                    .orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigo + "."));
            if (equipo.idLaboratorio() != idLaboratorio) {
                throw new BusinessException("El equipo " + codigo + " pertenece a otra sede (" + equipo.laboratorio() + ").");
            }
            Optional<int[]> linea = repository.lineaPendienteDeEquipo(cn, equipo.idEquipo());
            return linea.orElseThrow(() -> new BusinessException("El equipo " + codigo + " está "
                    + equipo.estado().getEtiqueta().toLowerCase(Locale.ROOT)
                    + " y no aparece pendiente de devolución en ningún préstamo activo (RN-10)."));
        });
    }

    /**
     * Registra la devolución de una o varias líneas (todas con la misma condición).
     *
     * @return mensaje para el usuario con el resultado (préstamo finalizado o equipos pendientes)
     */
    public String devolver(Usuario operador, List<Integer> idDetalles, CondicionDevolucion condicion,
                           String observacion) {
        if (idDetalles == null || idDetalles.isEmpty()) {
            throw new BusinessException("Seleccione al menos un equipo para devolver.");
        }
        if (condicion == null) {
            throw new BusinessException("Seleccione la condición en que regresa el equipo.");
        }
        if (condicion == CondicionDevolucion.DANADO && (observacion == null || observacion.isBlank())) {
            throw new BusinessException("Describa el daño en la observación.");
        }

        String resultadoDevolucion = Transaccion.ejecutar("registrar la devolución", cn -> {
            Set<Integer> prestamosTocados = new LinkedHashSet<>();
            int idLaboratorioDelUltimo = 0;
            int idSolicitante = 0;

            for (int idDetalle : idDetalles) {
                String[] linea = repository.lineaParaDevolver(cn, idDetalle)
                        .orElseThrow(() -> new BusinessException("El equipo seleccionado no existe en el préstamo."));
                int idPrestamo = Integer.parseInt(linea[0]);
                int idLaboratorio = Integer.parseInt(linea[2]);
                idSolicitante = Integer.parseInt(linea[3]);
                int idEquipo = Integer.parseInt(linea[4]);
                Permisos.exigirOperador(operador, idLaboratorio);

                if (!"DESPACHADO".equals(linea[1])) {
                    throw new BusinessException("El equipo no está pendiente dentro de un préstamo activo (RN-10).");
                }
                if (Integer.parseInt(linea[6]) > 0) {
                    throw new BusinessException("Ese equipo ya fue devuelto; no puede devolverse dos veces (RN-11).");
                }
                if (!EstadoEquipo.PRESTADO.name().equals(linea[5])) {
                    throw new BusinessException("El equipo no está en estado PRESTADO (estado actual: " + linea[5] + ").");
                }

                EstadoEquipo nuevo = condicion == CondicionDevolucion.BUENO ? EstadoEquipo.DISPONIBLE : EstadoEquipo.DANADO;
                repository.registrarDevolucion(cn, idDetalle, condicion, observacion, operador.getIdUsuario());
                if (!repository.cambiarEstadoEquipo(cn, idEquipo, EstadoEquipo.PRESTADO, nuevo)) {
                    throw new BusinessException("No se pudo actualizar el estado del equipo.");
                }
                String codigo = inventario.buscarPorId(cn, idEquipo).map(FilaInventario::codigoInventario)
                        .orElse(String.valueOf(idEquipo));
                auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.EQUIPO_DEVUELTO,
                        "Equipo", idEquipo, "Equipo " + codigo + " devuelto en condición " + condicion.name()
                                + " (préstamo #" + idPrestamo + "); pasa a " + nuevo.name() + ". Recibe "
                                + operador.getNombreUsuario() + "."
                                + (observacion == null || observacion.isBlank() ? "" : " Obs: " + observacion.trim()));
                prestamosTocados.add(idPrestamo);
                idLaboratorioDelUltimo = idLaboratorio;
            }

            StringBuilder mensaje = new StringBuilder("Devolución registrada.");
            for (int idPrestamo : prestamosTocados) {
                int pendientes = repository.contarPendientes(cn, idPrestamo);
                if (pendientes == 0) {
                    repository.finalizar(cn, idPrestamo);
                    repository.liberarBloqueosDePrestamo(cn, idPrestamo, operador.getIdUsuario());
                    auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorioDelUltimo,
                            AccionAuditoria.PRESTAMO_FINALIZADO, "Prestamo", idPrestamo,
                            "Préstamo #" + idPrestamo + " finalizado: todos los equipos fueron devueltos.");
                    mensaje.append("\nEl préstamo #").append(idPrestamo).append(" quedó FINALIZADO.");
                } else {
                    if (repository.crearBloqueoParcial(cn, idSolicitante, idPrestamo)) {
                        auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorioDelUltimo,
                                AccionAuditoria.BLOQUEO_CREADO, "Prestamo", idPrestamo,
                                "Devolución parcial del préstamo #" + idPrestamo + ": el usuario queda bloqueado "
                                        + "hasta devolver los " + pendientes + " equipo(s) pendiente(s).");
                    }
                    mensaje.append("\nEl préstamo #").append(idPrestamo).append(" sigue ACTIVO con ")
                            .append(pendientes).append(" equipo(s) pendiente(s). El usuario no podrá hacer otro ")
                            .append("préstamo hasta devolverlos.");
                }
            }
            return mensaje.toString();
        });
        Set<Integer> prestamosNotificados = new LinkedHashSet<>();

        for (int idDetalle : idDetalles) {
            try {
                int idPrestamo = Transaccion.consultar(
                        "consultar préstamo de devolución",
                        cn -> repository.lineaParaDevolver(cn, idDetalle)
                                .map(linea -> Integer.parseInt(linea[0]))
                                .orElse(-1)
                );

                if (idPrestamo > 0 && prestamosNotificados.add(idPrestamo)) {
                    notificarDevolucionPorSms(idPrestamo);
                }
            } catch (Exception e) {
                System.err.println("No se pudo notificar la devolución: " + e.getMessage());
            }
        }

        return resultadoDevolucion;
    }
    private void notificarDevolucionPorSms(int idPrestamo) {
        try {
            String[] datos = Transaccion.consultar(
                    "consultar datos para SMS de devolución",
                    cn -> repository.datosSmsPrestamo(cn, idPrestamo)
            );

            if (datos == null || datos[0] == null || datos[0].isBlank()) {
                System.err.println(
                        "No se envió SMS: el préstamo #" + idPrestamo
                                + " no tiene un teléfono registrado."
                );
                return;
            }

            String mensaje = "SIGELAB: Se registró la devolución de equipo(s) "
                    + "del préstamo #" + idPrestamo + ". Gracias por devolverlo.";

            smsService.enviarSms(datos[0], mensaje);

        } catch (Exception e) {
            System.err.println(
                    "No se pudo enviar el SMS de devolución del préstamo #"
                            + idPrestamo + ": " + e.getMessage()
            );
        }
    }

    private static String limpiarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("Escriba o lea el código del equipo.");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
}
