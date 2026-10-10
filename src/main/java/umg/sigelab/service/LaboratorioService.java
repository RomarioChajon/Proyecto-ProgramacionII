package umg.sigelab.service;

import umg.sigelab.model.Laboratorio;
import umg.sigelab.repository.LaboratorioRepository;

import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio de los laboratorios (sedes).
 * En este paso solo expone consultas; las altas y ediciones llegan con el CRUD de laboratorios.
 */
public class LaboratorioService {

    private final LaboratorioRepository repository;

    public LaboratorioService() {
        this(new LaboratorioRepository());
    }

    /** Constructor que recibe el repositorio: permite sustituirlo en pruebas. */
    public LaboratorioService(LaboratorioRepository repository) {
        this.repository = repository;
    }

    public List<Laboratorio> listar() {
        return repository.listar();
    }

    /** Laboratorios que se pueden elegir al asignar un encargado o registrar un equipo. */
    public List<Laboratorio> listarActivos() {
        return repository.listarActivos();
    }

    public Optional<Laboratorio> buscarPorId(int idLaboratorio) {
        return repository.buscarPorId(idLaboratorio);
    }
}
