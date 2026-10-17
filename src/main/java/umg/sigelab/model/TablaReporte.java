package umg.sigelab.model;

import java.util.List;

/**
 * Resultado genérico de una consulta de reporte: los nombres de columna y las filas ya listas para mostrar
 * en una tabla. Evita crear un registro (record) distinto por cada consulta del módulo de reportes.
 */
public record TablaReporte(String[] columnas, List<Object[]> filas) {
}
