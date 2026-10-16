package umg.sigelab.model;

/** Un modelo (producto) con unidades disponibles en una sede; es lo que el usuario agrega al carrito. */
public record ModeloDisponible(int idProducto, String codigo, String nombre, String marca, String modelo,
                               int idTipoEquipo, String categoria, int disponibles) {
}
