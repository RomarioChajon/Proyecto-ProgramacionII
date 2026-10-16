package umg.sigelab.model;

/** Categoría con la cantidad de unidades disponibles en la sede elegida (para el ComboBox). */
public record CategoriaStock(int idTipoEquipo, String nombre, int disponibles) {
    @Override
    public String toString() {
        return nombre + " (" + disponibles + " disp.)";
    }
}
