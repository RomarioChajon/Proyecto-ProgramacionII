package umg.sigelab.service;

import umg.sigelab.enums.AccionAuditoria;
import umg.sigelab.model.FilaAuditoria;
import umg.sigelab.model.FilaBloqueo;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.repository.AuditoriaRepository;
import umg.sigelab.repository.ConsultaRepository;
import umg.sigelab.repository.PrestamoRepository;
import umg.sigelab.util.BusinessException;
import umg.sigelab.util.Transaccion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dashboard, reportes, bloqueos y auditoría (PDF 6.10 y 6.11). */
public class ConsultaService {

    private final ConsultaRepository repository = new ConsultaRepository();
    private final PrestamoRepository prestamos = new PrestamoRepository();
    private final AuditoriaRepository auditoria = new AuditoriaRepository();

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

    /** ADMIN ve todo; ENCARGADO solo su sede (idLaboratorio obligatorio y propio). */
    private void exigir(Usuario operador, Integer idLaboratorio) {
        if (idLaboratorio == null) {
            Permisos.exigirAdmin(operador);
        } else {
            Permisos.exigirOperador(operador, idLaboratorio);
        }
    }
}
