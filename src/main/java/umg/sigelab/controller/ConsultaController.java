package umg.sigelab.controller;

import umg.sigelab.model.FilaAuditoria;
import umg.sigelab.model.FilaBloqueo;
import umg.sigelab.model.PrestamoResumen;
import umg.sigelab.model.Usuario;
import umg.sigelab.service.ConsultaService;

import java.util.List;
import java.util.Map;

/** Coordina dashboard, reportes, bloqueos y auditoría con {@link ConsultaService}. */
public class ConsultaController {

    private final ConsultaService service = new ConsultaService();

    public Map<String, Integer> dashboard(Usuario operador, Integer idLaboratorio) {
        return service.dashboard(operador, idLaboratorio);
    }

    public List<PrestamoResumen> vencidos(Usuario operador, Integer idLaboratorio) {
        return service.vencidos(operador, idLaboratorio);
    }

    public List<FilaBloqueo> bloqueos(Usuario operador, boolean soloActivos) {
        return service.bloqueos(operador, soloActivos);
    }

    public void liberarBloqueo(Usuario admin, FilaBloqueo bloqueo) {
        service.liberarBloqueo(admin, bloqueo);
    }

    public List<FilaAuditoria> auditoria(Usuario operador, Integer idLaboratorio) {
        return service.auditoria(operador, idLaboratorio);
    }
}
