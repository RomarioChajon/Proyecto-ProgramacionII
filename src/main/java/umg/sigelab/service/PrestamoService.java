package umg.sigelab.service;

import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.enums.EstadoEquipo;
import umg.sigelab.enums.RolUsuario;
import umg.sigelab.model.CategoriaStock;
import umg.sigelab.model.FilaInventario;
import umg.sigelab.model.Laboratorio;
import umg.sigelab.model.LineaPrestamo;
import umg.sigelab.model.ModeloDisponible;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.InventarioRepository;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import umg.sigelab.integration.sms.TextBeeSmsService;

/**
 * Reglas de negocio de los PRÉSTAMOS (solicitud, validación del encargado, despacho).
 *
 * <p>Flujo: el docente/estudiante arma su carrito y crea una SOLICITUD (estado PENDIENTE);
 * el encargado la valida y asigna el tiempo (estado DESPACHADO). Solo en el despacho los equipos
 * pasan a PRESTADO (RN-09). Todo lo que modifica varias tablas corre en UNA transacción y deja
 * rastro en Auditoría (RN-18).</p>
 */
public class PrestamoService {

    /** Tiempo máximo de un préstamo, en días (regla del proyecto). */
    public static final int DIAS_MAXIMOS = 30;

    private final PrestamoRepository repository = new PrestamoRepository();
    private final InventarioRepository inventario = new InventarioRepository();
    private final AuditoriaRepository auditoria = new AuditoriaRepository();
    private final TextBeeSmsService smsService = new TextBeeSmsService();

    // ------------------------------------------------------------------
    // Catálogo (vista del docente / estudiante)
    // ------------------------------------------------------------------

    public List<Laboratorio> laboratoriosDisponibles() {
        return Transaccion.consultar("consultar los laboratorios", repository::laboratoriosActivos);
    }

    public List<CategoriaStock> categorias(int idLaboratorio) {
        return Transaccion.consultar("consultar las categorías", cn -> repository.categoriasConStock(cn, idLaboratorio));
    }

    public List<ModeloDisponible> modelos(int idLaboratorio, int idTipoEquipo) {
        return Transaccion.consultar("consultar los equipos",
                cn -> repository.modelosDisponibles(cn, idLaboratorio, idTipoEquipo));
    }

    /** Texto con el motivo por el que el usuario NO puede pedir otro préstamo, o null si puede. */
    public String motivoBloqueo(Usuario usuario) {
        Permisos.exigirSolicitante(usuario);
        return Transaccion.consultar("consultar sus préstamos", cn -> motivoBloqueo(cn, usuario.getIdUsuario()));
    }

    private String motivoBloqueo(Connection cn, int idUsuario) throws SQLException {
        Integer pendiente = repository.prestamoConPendientes(cn, idUsuario);
        if (pendiente != null) {
            return "Tiene equipos pendientes de devolución en el préstamo #" + pendiente
                    + ". No puede realizar otro préstamo hasta devolverlos.";
        }
        Integer bloqueo = repository.bloqueoActivo(cn, idUsuario);
        if (bloqueo != null) {
            return "Su usuario tiene un bloqueo activo por el préstamo #" + bloqueo
                    + ". Comuníquese con el encargado del laboratorio.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Solicitud del docente / estudiante
    // ------------------------------------------------------------------

    /**
     * Crea una solicitud de préstamo con los modelos elegidos.
     *
     * @param idProductos modelos del carrito: UNA unidad de cada uno (el sistema asigna la unidad libre)
     * @return número del préstamo creado
     */
    public int solicitar(Usuario usuario, int idLaboratorio, int dias, List<Integer> idProductos) {
        Permisos.exigirSolicitante(usuario);
        if (dias < 1 || dias > DIAS_MAXIMOS) {
            throw new BusinessException("El tiempo de préstamo debe ser de 1 a " + DIAS_MAXIMOS + " días.");
        }
        if (idProductos == null || idProductos.isEmpty()) {
            throw new BusinessException("El carrito está vacío: agregue al menos un equipo (RN-05).");
        }
        if (new HashSet<>(idProductos).size() != idProductos.size()) {
            throw new BusinessException("Solo puede prestar una unidad de cada modelo.");
        }

        return Transaccion.ejecutar("registrar la solicitud de préstamo", cn -> {
            validarSolicitanteActivo(cn, usuario.getIdUsuario());
            String bloqueo = motivoBloqueo(cn, usuario.getIdUsuario());
            if (bloqueo != null) {
                throw new BusinessException(bloqueo);
            }
            boolean sedeActiva = repository.laboratoriosActivos(cn).stream()
                    .anyMatch(l -> l.getIdLaboratorio() == idLaboratorio);
            if (!sedeActiva) {
                throw new BusinessException("El laboratorio seleccionado no está disponible.");
            }

            int idPrestamo = repository.crearSolicitud(cn, usuario.getIdUsuario(), idLaboratorio, dias);
            for (int idProducto : idProductos) {
                Integer idEquipo = repository.unidadLibre(cn, idLaboratorio, idProducto).orElseThrow(() ->
                        new BusinessException("Uno de los modelos del carrito ya no tiene unidades disponibles "
                                + "en esta sede (RN-07). Actualice el catálogo e inténtelo de nuevo."));
                repository.agregarDetalle(cn, idPrestamo, idEquipo, idLaboratorio, idProducto);
            }
            auditoria.registrar(cn, usuario.getIdUsuario(), idLaboratorio, AccionAuditoria.PRESTAMO_CREADO,
                    "Prestamo", idPrestamo, "Solicitud #" + idPrestamo + " de " + usuario.getNombreUsuario() + ": "
                            + idProductos.size() + " equipo(s), " + dias + " día(s) solicitados. "
                            + "Pendiente de validación del encargado.");
            return idPrestamo;
        });
    }

    private void validarSolicitanteActivo(Connection cn, int idUsuario) throws SQLException {
        String[] datos = repository.rolYEstado(cn, idUsuario)
                .orElseThrow(() -> new BusinessException("El usuario no existe."));
        int rol = Integer.parseInt(datos[0]);
        if (!"ACTIVO".equals(datos[1])) {
            throw new BusinessException("El usuario está INACTIVO y no puede solicitar préstamos.");
        }
        if (rol != RolUsuario.DOCENTE.getId() && rol != RolUsuario.ESTUDIANTE.getId()) {
            throw new BusinessException("Solo los docentes y estudiantes pueden solicitar préstamos.");
        }
    }

    /** Historial completo del usuario: sus préstamos de cualquier sede. */
    public List<PrestamoResumen> historial(Usuario usuario) {
        Permisos.exigirSolicitante(usuario);
        return Transaccion.consultar("consultar el historial", cn -> repository.historialDeUsuario(cn, usuario.getIdUsuario()));
    }

    /** Equipos de un préstamo. Un docente/estudiante solo ve los suyos; el encargado, los de su sede. */
    public List<LineaPrestamo> lineas(Usuario usuario, int idPrestamo) {
        return Transaccion.consultar("consultar el detalle del préstamo", cn -> {
            PrestamoResumen resumen = repository.buscarResumen(cn, idPrestamo)
                    .orElseThrow(() -> new BusinessException("El préstamo no existe."));
            if (usuario.getRol() == RolUsuario.DOCENTE || usuario.getRol() == RolUsuario.ESTUDIANTE) {
                if (resumen.idSolicitante() != usuario.getIdUsuario()) {
                    throw new BusinessException("Ese préstamo no le pertenece.");
                }
            } else {
                Permisos.exigirOperador(usuario, resumen.idLaboratorio());
            }
            return repository.lineas(cn, idPrestamo);
        });
    }

    // ------------------------------------------------------------------
    // Encargado: validar solicitudes
    // ------------------------------------------------------------------

    public List<PrestamoResumen> pendientesDeDespacho(Usuario operador, int idLaboratorio) {
        Permisos.exigirOperador(operador, idLaboratorio);
        return Transaccion.consultar("consultar las órdenes pendientes",
                cn -> repository.pendientesDeDespacho(cn, idLaboratorio));
    }

    /**
     * El encargado confirma una solicitud: asigna el tiempo con calendario (inicio y fin, máximo 30 días),
     * los equipos pasan a PRESTADO y queda registrado quién lo confirmó.
     */
    public void despachar(Usuario operador, int idPrestamo, LocalDate inicio, LocalDate fin) {
        Plazo plazo = Plazo.de(inicio, fin);

        Transaccion.ejecutar("despachar el préstamo", cn -> {
            String[] encabezado = repository.encabezadoParaActualizar(cn, idPrestamo)
                    .orElseThrow(() -> new BusinessException("El préstamo no existe."));
            int idLaboratorio = Integer.parseInt(encabezado[1]);
            int idSolicitante = Integer.parseInt(encabezado[2]);
            Permisos.exigirOperador(operador, idLaboratorio);


            if (!"PENDIENTE".equals(encabezado[0])) {
                throw new BusinessException("El préstamo #" + idPrestamo + " ya no está pendiente (estado: "
                        + encabezado[0] + ").");
            }
            validarSolicitanteActivo(cn, idSolicitante);
            String bloqueo = motivoBloqueo(cn, idSolicitante);
            if (bloqueo != null) {
                throw new BusinessException("No se puede despachar: el solicitante no puede recibir otro préstamo. "
                        + bloqueo);
            }

            List<LineaPrestamo> lineas = repository.lineas(cn, idPrestamo);
            if (lineas.isEmpty()) {
                throw new BusinessException("Un préstamo debe tener al menos un equipo (RN-05).");
            }
            for (LineaPrestamo linea : lineas) {
                // RN-07 / RN-09: solo un equipo DISPONIBLE puede entregarse y pasa a PRESTADO.
                if (!repository.cambiarEstadoEquipo(cn, linea.idEquipo(), EstadoEquipo.DISPONIBLE, EstadoEquipo.PRESTADO)) {
                    throw new BusinessException("El equipo " + linea.codigoInventario() + " ya no está DISPONIBLE "
                            + "(cambió de estado después de la solicitud). Rechace la solicitud para que el usuario "
                            + "pida otra.");
                }
                auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.EQUIPO_PRESTADO,
                        "Equipo", linea.idEquipo(), "Equipo " + linea.codigoInventario() + " pasa a PRESTADO "
                                + "(préstamo #" + idPrestamo + ").");
            }
            if (!repository.despachar(cn, idPrestamo, operador.getIdUsuario(), plazo.dias, plazo.inicio, plazo.fin,
                    plazo.inicioHoy)) {
                throw new BusinessException("No se pudo despachar el préstamo #" + idPrestamo + ".");
            }
            auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.PRESTAMO_DESPACHADO,
                    "Prestamo", idPrestamo, "Préstamo #" + idPrestamo + " confirmado por "
                            + operador.getNombreUsuario() + " (ID " + operador.getIdUsuario() + "): "
                            + lineas.size() + " equipo(s), " + plazo.dias + " día(s), devolución esperada "
                            + plazo.fin.toLocalDate() + ".");
            return null;
        });
    }

    /** El encargado rechaza una solicitud (debe explicar el motivo). No cambia el estado de ningún equipo. */
    public void rechazar(Usuario operador, int idPrestamo, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Escriba el motivo del rechazo.");
        }
        Transaccion.ejecutar("rechazar la solicitud", cn -> {
            String[] encabezado = repository.encabezadoParaActualizar(cn, idPrestamo)
                    .orElseThrow(() -> new BusinessException("El préstamo no existe."));
            int idLaboratorio = Integer.parseInt(encabezado[1]);
            Permisos.exigirOperador(operador, idLaboratorio);
            if (!repository.rechazar(cn, idPrestamo, operador.getIdUsuario(), motivo.trim())) {
                throw new BusinessException("El préstamo #" + idPrestamo + " ya no está pendiente.");
            }
            auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.PRESTAMO_RECHAZADO,
                    "Prestamo", idPrestamo, "Préstamo #" + idPrestamo + " rechazado por "
                            + operador.getNombreUsuario() + ". Motivo: " + motivo.trim());
            return null;
        });

        notificarPrestamoPorSms(idPrestamo);
    }

    // ------------------------------------------------------------------
    // Encargado: crear un préstamo escaneando los equipos físicos
    // ------------------------------------------------------------------

    /**
     * Crea y entrega un préstamo en el mostrador. Los equipos se identifican por el código de su QR.
     *
     * @param usuarioIdONombre ID o nombre de usuario del docente/estudiante
     * @param codigos          códigos de inventario leídos (uno por equipo)
     * @return número del préstamo creado
     */
    public int crearYDespachar(Usuario operador, int idLaboratorio, String usuarioIdONombre, List<String> codigos,
                               LocalDate inicio, LocalDate fin) {
        Permisos.exigirOperador(operador, idLaboratorio);
        if (usuarioIdONombre == null || usuarioIdONombre.isBlank()) {
            throw new BusinessException("Indique el ID o el usuario de quien recibe el préstamo.");
        }
        if (codigos == null || codigos.isEmpty()) {
            throw new BusinessException("Agregue al menos un equipo (RN-05).");
        }
        Plazo plazo = Plazo.de(inicio, fin);

        int idPrestamoResultado = Transaccion.ejecutar("registrar el préstamo", cn -> {
            String[] usuario = repository.buscarUsuario(cn, usuarioIdONombre.trim())
                    .orElseThrow(() -> new BusinessException("No existe un usuario con ese ID o nombre de usuario."));
            int idSolicitante = Integer.parseInt(usuario[0]);
            int rol = Integer.parseInt(usuario[1]);
            if (rol != RolUsuario.DOCENTE.getId() && rol != RolUsuario.ESTUDIANTE.getId()) {
                throw new BusinessException("Solo los docentes y estudiantes pueden recibir préstamos.");
            }
            if (!"ACTIVO".equals(usuario[2])) {
                throw new BusinessException("El usuario está INACTIVO.");
            }
            String bloqueo = motivoBloqueo(cn, idSolicitante);
            if (bloqueo != null) {
                throw new BusinessException(usuario[3] + ": " + bloqueo);
            }

            Set<Integer> equipos = new HashSet<>();
            Set<Integer> modelos = new HashSet<>();
            List<FilaInventario> filas = new java.util.ArrayList<>();
            for (String codigoLeido : codigos) {
                String codigo = codigoLeido.trim().toUpperCase(Locale.ROOT);
                FilaInventario fila = inventario.buscarPorCodigo(cn, codigo)
                        .orElseThrow(() -> new BusinessException("No existe un equipo con el código " + codigo + "."));
                if (fila.idLaboratorio() != idLaboratorio) {
                    throw new BusinessException("El equipo " + codigo + " pertenece a otra sede ("
                            + fila.laboratorio() + "); un préstamo sale de una sola sede.");
                }
                if (!equipos.add(fila.idEquipo())) {
                    throw new BusinessException("El equipo " + codigo + " está repetido (RN-06).");
                }
                if (!modelos.add(fila.idProducto())) {
                    throw new BusinessException("Solo se puede prestar una unidad de cada modelo: "
                            + fila.producto() + ".");
                }
                if (fila.estado() != EstadoEquipo.DISPONIBLE) {
                    throw new BusinessException("El equipo " + codigo + " está " + fila.estado().getEtiqueta()
                            .toUpperCase(Locale.ROOT) + " y no se puede prestar (RN-08).");
                }
                Integer reserva = inventario.solicitudQueReserva(cn, fila.idEquipo());
                if (reserva != null) {
                    throw new BusinessException("El equipo " + codigo + " está reservado en la solicitud #" + reserva
                            + " (pendiente de despacho).");
                }
                filas.add(fila);
            }

            int idPrestamo = repository.crearDespachado(cn, idSolicitante, idLaboratorio, operador.getIdUsuario(),
                    plazo.dias, plazo.inicio, plazo.fin, plazo.inicioHoy);
            for (FilaInventario fila : filas) {
                if (!repository.cambiarEstadoEquipo(cn, fila.idEquipo(), EstadoEquipo.DISPONIBLE, EstadoEquipo.PRESTADO)) {
                    throw new BusinessException("El equipo " + fila.codigoInventario() + " ya no está DISPONIBLE.");
                }
                repository.agregarDetalle(cn, idPrestamo, fila.idEquipo(), idLaboratorio, fila.idProducto());
                auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.EQUIPO_PRESTADO,
                        "Equipo", fila.idEquipo(), "Equipo " + fila.codigoInventario() + " pasa a PRESTADO "
                                + "(préstamo #" + idPrestamo + ").");
            }
            auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.PRESTAMO_CREADO,
                    "Prestamo", idPrestamo, "Préstamo #" + idPrestamo + " creado en mostrador por "
                            + operador.getNombreUsuario() + " para " + usuario[3] + " (ID " + idSolicitante + ").");
            auditoria.registrar(cn, operador.getIdUsuario(), idLaboratorio, AccionAuditoria.PRESTAMO_DESPACHADO,
                    "Prestamo", idPrestamo, "Préstamo #" + idPrestamo + " entregado: " + filas.size()
                            + " equipo(s), " + plazo.dias + " día(s), devolución esperada " + plazo.fin.toLocalDate() + ".");
            return idPrestamo;
        });

        notificarPrestamoPorSms(idPrestamoResultado);
        return idPrestamoResultado;
    }

    // ------------------------------------------------------------------
    // Cálculo del plazo (calendario inicio - fin)
    // ------------------------------------------------------------------

    /** Plazo validado: inicio, fin y número de días (inclusive). */
    private static final class Plazo {
        final LocalDateTime inicio;
        final LocalDateTime fin;
        final int dias;
        final boolean inicioHoy;

        private Plazo(LocalDateTime inicio, LocalDateTime fin, int dias, boolean inicioHoy) {
            this.inicio = inicio;
            this.fin = fin;
            this.dias = dias;
            this.inicioHoy = inicioHoy;
        }

        static Plazo de(LocalDate inicio, LocalDate fin) {
            if (inicio == null || fin == null) {
                throw new BusinessException("Seleccione la fecha de inicio y la fecha de fin del préstamo.");
            }
            LocalDate hoy = LocalDate.now();
            if (inicio.isBefore(hoy)) {
                throw new BusinessException("La fecha de inicio no puede ser anterior a hoy.");
            }
            if (fin.isBefore(inicio)) {
                throw new BusinessException("La fecha de fin no puede ser anterior a la de inicio.");
            }
            long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
            if (dias > DIAS_MAXIMOS) {
                throw new BusinessException("El tiempo máximo de préstamo es de " + DIAS_MAXIMOS + " días.");
            }
            boolean inicioHoy = inicio.equals(hoy);
            return new Plazo(inicioHoy ? LocalDateTime.now() : inicio.atTime(8, 0), fin.atTime(23, 59), (int) dias,
                    inicioHoy);
        }
    }

    private void notificarPrestamoPorSms(int idPrestamo) {
        try {
            String[] datos = Transaccion.consultar(
                    "consultar datos para SMS del préstamo",
                    cn -> repository.datosSmsPrestamo(cn, idPrestamo)
            );

            if (datos == null) {
                System.err.println(
                        "No se pudo enviar el SMS: no se encontraron datos del préstamo "
                                + idPrestamo
                );
                return;
            }

            String telefono = datos[0];
            String fechaDevolucion = datos[1];

            if (telefono == null || telefono.isBlank()) {
                System.err.println(
                        "El solicitante no tiene teléfono registrado. Préstamo: "
                                + idPrestamo
                );
                return;
            }

            if (fechaDevolucion == null || fechaDevolucion.isBlank()) {
                System.err.println(
                        "No hay fecha de devolución registrada para el préstamo "
                                + idPrestamo
                );
                return;
            }

            String mensaje = "SIGELAB: Tu préstamo #" + idPrestamo
                    + " ha sido despachado. Fecha esperada de devolución: "
                    + fechaDevolucion + ".";

            smsService.enviarSms(telefono, mensaje);

        } catch (Exception e) {
            // El préstamo ya fue confirmado; un fallo de SMS no debe deshacerlo.
            System.err.println(
                    "No se pudo enviar el SMS del préstamo #" + idPrestamo
                            + ": " + e.getMessage()
            );
        }
    }
}
