/* ============================================================================
   SIGELAB_08_hora_guatemala.sql
   Azure SQL siempre trabaja en hora UTC (6 horas adelante de Guatemala).
   Este script hace que las fechas que pone la BASE (valores por defecto y la
   vista de préstamos) usen la hora de Guatemala, igual que el código Java.

   - NO borra tablas ni datos. Solo cambia valores por defecto y una vista.
   - Se puede ejecutar más de una vez sin problema.
   - COORDINAR CON EL EQUIPO antes de ejecutarlo: la base es compartida.
   ============================================================================ */
SET NOCOUNT ON;
GO

DECLARE @columnas TABLE (tabla SYSNAME, columna SYSNAME, nombre_default SYSNAME);
INSERT INTO @columnas VALUES
    (N'Usuario',         N'fecha_creacion',      N'DF_Usuario_Fecha'),
    (N'Laboratorio',     N'fecha_creacion',      N'DF_Laboratorio_Fecha'),
    (N'Equipo',          N'fecha_registro',      N'DF_Equipo_Fecha'),
    (N'PerfilFacial',    N'fecha_registro',      N'DF_PerfilFacial_FechaRegistro'),
    (N'PerfilFacial',    N'fecha_actualizacion', N'DF_PerfilFacial_FechaActualizacion'),
    (N'Prestamo',        N'fecha_prestamo',      N'DF_Prestamo_Fecha'),
    (N'Devolucion',      N'fecha_devolucion',    N'DF_Devolucion_Fecha'),
    (N'BloqueoUsuario',  N'fecha_bloqueo',       N'DF_Bloqueo_Fecha'),
    (N'Mantenimiento',   N'fecha_ingreso',       N'DF_Mant_Ingreso'),
    (N'Auditoria',       N'fecha_hora',          N'DF_Auditoria_Fecha'),
    (N'AuditoriaFacial', N'fecha_hora',          N'DF_AuditoriaFacial_Fecha');

DECLARE @tabla SYSNAME, @columna SYSNAME, @nombre SYSNAME, @actual SYSNAME, @sql NVARCHAR(MAX);
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT tabla, columna, nombre_default FROM @columnas;
OPEN c;
FETCH NEXT FROM c INTO @tabla, @columna, @nombre;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF COL_LENGTH(N'dbo.' + @tabla, @columna) IS NULL
        PRINT N'Se omite dbo.' + @tabla + N'.' + @columna + N' (no existe en esta base).';
    ELSE
    BEGIN
        -- Quita el valor por defecto actual (tenga el nombre que tenga).
        SELECT @actual = dc.name
        FROM sys.default_constraints dc
        JOIN sys.columns col ON col.object_id = dc.parent_object_id AND col.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.' + @tabla) AND col.name = @columna;

        IF @actual IS NOT NULL
        BEGIN
            SET @sql = N'ALTER TABLE dbo.' + QUOTENAME(@tabla) + N' DROP CONSTRAINT ' + QUOTENAME(@actual) + N';';
            EXEC sys.sp_executesql @sql;
        END

        SET @sql = N'ALTER TABLE dbo.' + QUOTENAME(@tabla) + N' ADD CONSTRAINT ' + QUOTENAME(@nombre)
                 + N' DEFAULT CAST(SYSDATETIMEOFFSET() AT TIME ZONE ''Central America Standard Time'' AS DATETIME2(0)) FOR '
                 + QUOTENAME(@columna) + N';';
        EXEC sys.sp_executesql @sql;
        PRINT N'Actualizado dbo.' + @tabla + N'.' + @columna;
        SET @actual = NULL;
    END
    FETCH NEXT FROM c INTO @tabla, @columna, @nombre;
END
CLOSE c;
DEALLOCATE c;
GO

-- La vista marca como VENCIDO un préstamo comparando con la hora actual: también en hora de Guatemala.
CREATE OR ALTER VIEW dbo.vw_prestamos_resumen AS
SELECT  p.id_prestamo,
        p.id_laboratorio,
        l.nombre AS laboratorio,
        p.id_solicitante,
        CONCAT(u.nombre, ' ', u.apellido) AS solicitante,
        r.nombre AS rol_solicitante,
        p.estado,
        p.fecha_prestamo,
        p.fecha_despacho,
        p.dias_prestamo,
        p.fecha_esperada_devolucion,
        p.fecha_cierre,
        COUNT(d.id_detalle)                          AS total_equipos,
        COUNT(dv.id_devolucion)                      AS devueltos,
        COUNT(d.id_detalle) - COUNT(dv.id_devolucion) AS pendientes,
        CASE WHEN p.estado = 'DESPACHADO'
            AND p.fecha_esperada_devolucion < CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0))
            AND COUNT(d.id_detalle) > COUNT(dv.id_devolucion)
                 THEN 'VENCIDO' ELSE p.estado END        AS estado_calculado   -- RN-17
FROM dbo.Prestamo p
         JOIN dbo.Laboratorio l ON l.id_laboratorio = p.id_laboratorio
         JOIN dbo.Usuario     u ON u.id_usuario     = p.id_solicitante
         JOIN dbo.Rol         r ON r.id_rol         = u.id_rol
         LEFT JOIN dbo.DetallePrestamo d  ON d.id_prestamo = p.id_prestamo
         LEFT JOIN dbo.Devolucion      dv ON dv.id_detalle = d.id_detalle
GROUP BY p.id_prestamo, p.id_laboratorio, l.nombre, p.id_solicitante, u.nombre, u.apellido,
         r.nombre, p.estado, p.fecha_prestamo, p.fecha_despacho, p.dias_prestamo,
         p.fecha_esperada_devolucion, p.fecha_cierre;
GO

-- Comprobación: ambas columnas deben mostrar tu hora actual.
SELECT CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'Central America Standard Time' AS DATETIME2(0)) AS hora_guatemala,
       OBJECT_DEFINITION(OBJECT_ID(N'dbo.DF_Usuario_Fecha')) AS default_usuario;
GO

/* ----------------------------------------------------------------------------
   OPCIONAL: corregir registros que ya se guardaron en UTC.
   Solo afecta lo creado DESPUÉS de pasar la base a Azure (antes la hora ya era
   de Guatemala). Cambie la fecha por el momento en que empezaron a usar Azure,
   revise primero con el SELECT y solo entonces quite los comentarios del UPDATE.
   Las demás tablas se corrigen igual, columna por columna.
   ----------------------------------------------------------------------------
DECLARE @inicio_azure DATETIME2(0) = '2026-10-28T00:00:00';   -- en UTC
SELECT id_usuario, nombre_usuario, fecha_creacion,
       DATEADD(HOUR, -6, fecha_creacion) AS fecha_corregida
FROM dbo.Usuario WHERE fecha_creacion >= @inicio_azure;
-- UPDATE dbo.Usuario SET fecha_creacion = DATEADD(HOUR, -6, fecha_creacion) WHERE fecha_creacion >= @inicio_azure;
*/
