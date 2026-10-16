/* =====================================================================
   SIGELAB_07_modulo_prestamos.sql
   ---------------------------------------------------------------------
   OBJETIVO
     Preparar la base de datos para el módulo de PRÉSTAMOS (vista de
     docente/estudiante, panel del encargado, devoluciones y mantenimiento).

   QUÉ AGREGA (todo es idempotente: se puede ejecutar varias veces)
     1) Prestamo.dias_solicitados      -> días que pide el solicitante (1 a 30).
     2) CK_Prestamo_DiasMax30          -> el préstamo asignado no puede pasar de 30 días.
     3) DetallePrestamo.id_producto    -> modelo (producto) de cada equipo del préstamo.
     4) UQ_Detalle_Prestamo_Producto   -> en un mismo préstamo NO puede haber dos
                                          unidades del mismo modelo.
     5) Diagnóstico final.

   NO borra ni modifica datos existentes.
   ANTES DE EJECUTAR: esta base debe ser la creada con el script original
   (tablas Prestamo, DetallePrestamo, Equipo ya existen).

   CÓMO EJECUTAR EN DBEAVER (Mac): conexión activa -> abrir archivo -> Option + X
   ===================================================================== */

USE SIGELAB;
GO

/* ---------------------------------------------------------------------
   1) Días que solicita el usuario al pedir el préstamo.
   El encargado después asigna el tiempo real (dias_prestamo) con el calendario. */
IF COL_LENGTH('dbo.Prestamo', 'dias_solicitados') IS NULL
BEGIN
    ALTER TABLE dbo.Prestamo ADD dias_solicitados INT NULL;
    PRINT '1) Columna Prestamo.dias_solicitados creada.';
END
ELSE PRINT '1) Prestamo.dias_solicitados ya existe.';
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = 'CK_Prestamo_DiasSolicitados'
                 AND parent_object_id = OBJECT_ID('dbo.Prestamo'))
    ALTER TABLE dbo.Prestamo ADD CONSTRAINT CK_Prestamo_DiasSolicitados
        CHECK (dias_solicitados IS NULL OR dias_solicitados BETWEEN 1 AND 30);
GO

/* ---------------------------------------------------------------------
   2) Máximo 30 días en el tiempo asignado por el encargado.
   WITH NOCHECK: no revisa filas antiguas (por si hubiera datos de prueba con
   más de 30 días) pero SÍ se exige en todo registro nuevo. */
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = 'CK_Prestamo_DiasMax30'
                 AND parent_object_id = OBJECT_ID('dbo.Prestamo'))
    ALTER TABLE dbo.Prestamo WITH NOCHECK ADD CONSTRAINT CK_Prestamo_DiasMax30
        CHECK (dias_prestamo IS NULL OR dias_prestamo BETWEEN 1 AND 30);
GO

/* ---------------------------------------------------------------------
   3) Modelo (producto) en el detalle del préstamo.
   Se copia desde Equipo para los detalles que ya existan. */
IF COL_LENGTH('dbo.DetallePrestamo', 'id_producto') IS NULL
BEGIN
    ALTER TABLE dbo.DetallePrestamo ADD id_producto INT NULL;
    PRINT '3) Columna DetallePrestamo.id_producto creada.';
END
GO

UPDATE d
SET    d.id_producto = e.id_producto
FROM   dbo.DetallePrestamo d
JOIN   dbo.Equipo e ON e.id_equipo = d.id_equipo
WHERE  d.id_producto IS NULL;
GO

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('dbo.DetallePrestamo')
             AND name = 'id_producto' AND is_nullable = 1)
    ALTER TABLE dbo.DetallePrestamo ALTER COLUMN id_producto INT NOT NULL;
GO

/* Clave (id_equipo, id_producto) en Equipo: permite la llave foránea compuesta
   que garantiza que el modelo del detalle es realmente el del equipo. */
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints
               WHERE name = 'UQ_Equipo_Id_Producto'
                 AND parent_object_id = OBJECT_ID('dbo.Equipo'))
    ALTER TABLE dbo.Equipo ADD CONSTRAINT UQ_Equipo_Id_Producto UNIQUE (id_equipo, id_producto);
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
               WHERE name = 'FK_Detalle_EquipoProducto'
                 AND parent_object_id = OBJECT_ID('dbo.DetallePrestamo'))
    ALTER TABLE dbo.DetallePrestamo ADD CONSTRAINT FK_Detalle_EquipoProducto
        FOREIGN KEY (id_equipo, id_producto) REFERENCES dbo.Equipo (id_equipo, id_producto);
GO

/* ---------------------------------------------------------------------
   4) Un modelo por préstamo.
   Si ya hubiera datos de prueba con dos unidades del mismo modelo en un
   préstamo, el script lo informa y NO crea la restricción (corrija esos datos
   y vuelva a ejecutar). */
BEGIN
    IF EXISTS (SELECT 1 FROM sys.key_constraints
               WHERE name = 'UQ_Detalle_Prestamo_Producto'
                 AND parent_object_id = OBJECT_ID('dbo.DetallePrestamo'))
        PRINT '4) UQ_Detalle_Prestamo_Producto ya existe.';
    ELSE IF EXISTS (SELECT 1 FROM dbo.DetallePrestamo
                    GROUP BY id_prestamo, id_producto HAVING COUNT(*) > 1)
    BEGIN
        PRINT '4) ATENCION: hay préstamos con dos unidades del mismo modelo. Revise esta consulta y corrija:';
        SELECT id_prestamo, id_producto, COUNT(*) AS unidades
        FROM   dbo.DetallePrestamo
        GROUP  BY id_prestamo, id_producto
        HAVING COUNT(*) > 1;
    END
    ELSE
    BEGIN
        ALTER TABLE dbo.DetallePrestamo
            ADD CONSTRAINT UQ_Detalle_Prestamo_Producto UNIQUE (id_prestamo, id_producto);
        PRINT '4) UQ_Detalle_Prestamo_Producto creada.';
    END
END
GO

/* ---------------------------------------------------------------------
   5) Diagnóstico: debe mostrar las columnas y restricciones nuevas. */
SELECT 'Prestamo.dias_solicitados'      AS elemento, CASE WHEN COL_LENGTH('dbo.Prestamo','dias_solicitados') IS NULL THEN 'FALTA' ELSE 'OK' END AS estado
UNION ALL SELECT 'DetallePrestamo.id_producto', CASE WHEN COL_LENGTH('dbo.DetallePrestamo','id_producto') IS NULL THEN 'FALTA' ELSE 'OK' END
UNION ALL SELECT 'CK_Prestamo_DiasMax30', CASE WHEN EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_Prestamo_DiasMax30') THEN 'OK' ELSE 'FALTA' END
UNION ALL SELECT 'UQ_Detalle_Prestamo_Producto', CASE WHEN EXISTS (SELECT 1 FROM sys.key_constraints WHERE name='UQ_Detalle_Prestamo_Producto') THEN 'OK' ELSE 'FALTA' END;
GO
