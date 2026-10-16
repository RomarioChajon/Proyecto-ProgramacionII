package umg.sigelab.model;

import umg.sigelab.enums.EstadoEquipo;

/** Una unidad de equipo con los datos que necesita el encargado para el inventario. */
public record FilaInventario(int idEquipo, int idProducto, String codigoInventario, String producto, String marca, String modelo,
                             int idTipoEquipo, String categoria, String numeroSerie, int idLaboratorio,
                             String laboratorio, EstadoEquipo estado, String observaciones) {
}
