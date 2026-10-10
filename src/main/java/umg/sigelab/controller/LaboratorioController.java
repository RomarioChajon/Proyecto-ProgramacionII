package umg.sigelab.controller;

import umg.sigelab.model.Laboratorio;
import umg.sigelab.service.LaboratorioService;

import java.util.List;

/**
 * Controlador de laboratorios: intermediario DELGADO entre la vista y el Service.
 * No tiene reglas de negocio ni SQL; solo delega.
 */
public class LaboratorioController {

    private final LaboratorioService service;

    public LaboratorioController() {
        this(new LaboratorioService());
    }

    public LaboratorioController(LaboratorioService service) {
        this.service = service;
    }

    public List<Laboratorio> listarActivos() {
        return service.listarActivos();
    }
}
