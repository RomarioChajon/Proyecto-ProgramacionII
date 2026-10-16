/* =====================================================================
   SIGELAB - Ajuste de dbo.TipoEquipo (categorías)
   Tu tabla TipoEquipo ya existía SIN la columna "codigo" que usa el
   módulo de Categorías (formato CAT-XXXX-0001). Este script:
     1) agrega la columna "codigo",
     2) genera un código para las categorías que ya existan,
     3) la deja obligatoria, única y con el formato correcto.
   Es IDEMPOTENTE y NO borra datos. Ejecutar con la conexión a SIGELAB.
   ===================================================================== */
USE SIGELAB;
GO

IF COL_LENGTH('dbo.TipoEquipo', 'codigo') IS NULL
    ALTER TABLE dbo.TipoEquipo ADD codigo NVARCHAR(30) NULL;
GO

/* Rellena el código de las categorías existentes: CAT-<4 letras del nombre>-<número>
   Todo va dentro de BEGIN...END para que DBeaver lo ejecute como UN solo bloque. */
BEGIN
DECLARE @id INT, @nombre NVARCHAR(100), @txt NVARCHAR(100), @pref NVARCHAR(4),
        @i INT, @c NCHAR(1), @n INT;

SELECT @n = ISNULL(MAX(CAST(RIGHT(codigo, 4) AS INT)), 0)
FROM dbo.TipoEquipo
WHERE codigo LIKE 'CAT-[A-Z][A-Z][A-Z][A-Z]-[0-9][0-9][0-9][0-9]';

DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT id_tipo_equipo, nombre FROM dbo.TipoEquipo
    WHERE codigo IS NULL ORDER BY id_tipo_equipo;
OPEN cur;
FETCH NEXT FROM cur INTO @id, @nombre;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @txt = UPPER(@nombre);
    SET @txt = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@txt,
               N'Á', N'A'), N'É', N'E'), N'Í', N'I'), N'Ó', N'O'), N'Ú', N'U'), N'Ü', N'U'), N'Ñ', N'N');
    SET @pref = N'';
    SET @i = 1;
    WHILE @i <= LEN(@txt) AND LEN(@pref) < 4
    BEGIN
        SET @c = SUBSTRING(@txt, @i, 1);
        IF UNICODE(@c) BETWEEN 65 AND 90 SET @pref = @pref + @c;
        SET @i += 1;
    END
    WHILE LEN(@pref) < 4 SET @pref = @pref + N'X';
    SET @n += 1;
    UPDATE dbo.TipoEquipo
       SET codigo = N'CAT-' + @pref + N'-' + RIGHT('0000' + CAST(@n AS VARCHAR(4)), 4)
     WHERE id_tipo_equipo = @id;
    FETCH NEXT FROM cur INTO @id, @nombre;
END
CLOSE cur;
DEALLOCATE cur;
END
GO

/* Obligatoria, única y con formato */
IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID('dbo.TipoEquipo') AND name = 'codigo' AND is_nullable = 1)
    ALTER TABLE dbo.TipoEquipo ALTER COLUMN codigo NVARCHAR(30) NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.TipoEquipo') AND name = 'UQ_TipoEquipo_Codigo')
    ALTER TABLE dbo.TipoEquipo ADD CONSTRAINT UQ_TipoEquipo_Codigo UNIQUE (codigo);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID('dbo.TipoEquipo') AND name = 'CK_TipoEquipo_Codigo')
    ALTER TABLE dbo.TipoEquipo ADD CONSTRAINT CK_TipoEquipo_Codigo
        CHECK (codigo LIKE 'CAT-[A-Z][A-Z][A-Z][A-Z]-%' AND LEN(codigo) BETWEEN 12 AND 19);
GO

/* Resultado: las categorías con su código */
SELECT id_tipo_equipo, codigo, nombre, estado FROM dbo.TipoEquipo ORDER BY id_tipo_equipo;
GO



