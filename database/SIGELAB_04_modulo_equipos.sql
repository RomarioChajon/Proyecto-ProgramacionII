/* =====================================================================
   SIGELAB - Migración para la base de datos YA EXISTENTE "SIGELAB"
   Módulos de Geovanni: sedes/laboratorios, categorías, productos y equipos.

   - Es IDEMPOTENTE: se puede ejecutar varias veces; solo crea lo que falta.
   - NO borra ni modifica datos existentes.
   - Si ya tiene tabla Laboratorio (de Romario), se respeta tal cual.
   - Ejecutar con la base SIGELAB seleccionada (Azure Data Studio / SSMS).
   ===================================================================== */
USE SIGELAB;
GO
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* 1. LABORATORIO (sede) */
IF OBJECT_ID(N'dbo.Laboratorio', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Laboratorio (
        id_laboratorio INT IDENTITY(1,1) NOT NULL,
        codigo         NVARCHAR(20)  NOT NULL,
        nombre         NVARCHAR(100) NOT NULL,
        ubicacion      NVARCHAR(200) NOT NULL,
        capacidad      INT           NOT NULL,
        estado         NVARCHAR(10)  NOT NULL CONSTRAINT DF_Laboratorio_Estado DEFAULT 'ACTIVO',
        fecha_creacion DATETIME2(0)  NOT NULL CONSTRAINT DF_Laboratorio_Fecha DEFAULT SYSDATETIME(),
        CONSTRAINT PK_Laboratorio        PRIMARY KEY (id_laboratorio),
        CONSTRAINT UQ_Laboratorio_Codigo UNIQUE (codigo),
        CONSTRAINT CK_Laboratorio_Capacidad CHECK (capacidad > 0),
        CONSTRAINT CK_Laboratorio_Estado    CHECK (estado IN ('ACTIVO','INACTIVO'))
    );
END
GO

/* 2. TIPO_EQUIPO (categoría) */
IF OBJECT_ID(N'dbo.TipoEquipo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TipoEquipo (
        id_tipo_equipo INT IDENTITY(1,1) NOT NULL,
        codigo         NVARCHAR(30)  NOT NULL,
        nombre         NVARCHAR(50)  NOT NULL,
        descripcion    NVARCHAR(200) NULL,
        estado         NVARCHAR(10)  NOT NULL CONSTRAINT DF_TipoEquipo_Estado DEFAULT 'ACTIVO',
        CONSTRAINT PK_TipoEquipo        PRIMARY KEY (id_tipo_equipo),
        CONSTRAINT UQ_TipoEquipo_Nombre UNIQUE (nombre),
        CONSTRAINT UQ_TipoEquipo_Codigo UNIQUE (codigo),
        CONSTRAINT CK_TipoEquipo_Estado CHECK (estado IN ('ACTIVO','INACTIVO')),
        CONSTRAINT CK_TipoEquipo_Codigo CHECK (codigo LIKE 'CAT-[A-Z][A-Z][A-Z][A-Z]-%' AND LEN(codigo) BETWEEN 12 AND 19)
    );
END
GO

IF OBJECT_ID(N'dbo.SecuenciaTipoEquipo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SecuenciaTipoEquipo (
        id INT NOT NULL CONSTRAINT PK_SecuenciaTipoEquipo PRIMARY KEY,
        ultimo_numero INT NOT NULL,
        CONSTRAINT CK_SecuenciaTipoEquipo_Id  CHECK (id = 1),
        CONSTRAINT CK_SecuenciaTipoEquipo_Num CHECK (ultimo_numero >= 0)
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM dbo.SecuenciaTipoEquipo WHERE id = 1)
    INSERT INTO dbo.SecuenciaTipoEquipo (id, ultimo_numero) VALUES (1, 0);
GO

/* 3. PRODUCTO */
IF OBJECT_ID(N'dbo.Producto', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Producto (
        id_producto     INT IDENTITY(1,1) NOT NULL,
        codigo_producto NVARCHAR(30)  NOT NULL,
        nombre          NVARCHAR(100) NOT NULL,
        id_tipo_equipo  INT           NOT NULL,
        marca           NVARCHAR(50)  NULL,
        modelo          NVARCHAR(50)  NULL,
        descripcion     NVARCHAR(250) NULL,
        estado          NVARCHAR(10)  NOT NULL CONSTRAINT DF_Producto_Estado DEFAULT 'ACTIVO',
        CONSTRAINT PK_Producto        PRIMARY KEY (id_producto),
        CONSTRAINT UQ_Producto_Codigo UNIQUE (codigo_producto),
        CONSTRAINT FK_Producto_Tipo   FOREIGN KEY (id_tipo_equipo) REFERENCES dbo.TipoEquipo(id_tipo_equipo),
        CONSTRAINT CK_Producto_Estado CHECK (estado IN ('ACTIVO','INACTIVO')),
        CONSTRAINT CK_Producto_Codigo CHECK (LEN(LTRIM(RTRIM(codigo_producto))) > 0)
    );
END
GO

IF OBJECT_ID(N'dbo.SecuenciaProducto', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SecuenciaProducto (
        id_tipo_equipo INT NOT NULL CONSTRAINT PK_SecuenciaProducto PRIMARY KEY,
        ultimo_numero  INT NOT NULL CONSTRAINT DF_SecuenciaProducto_Num DEFAULT 0,
        CONSTRAINT FK_SecuenciaProducto_Tipo FOREIGN KEY (id_tipo_equipo) REFERENCES dbo.TipoEquipo(id_tipo_equipo),
        CONSTRAINT CK_SecuenciaProducto_Num CHECK (ultimo_numero >= 0)
    );
END
GO

/* 4. EQUIPO (unidad física) */
IF OBJECT_ID(N'dbo.Equipo', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Equipo (
        id_equipo         INT IDENTITY(1,1) NOT NULL,
        codigo_inventario NVARCHAR(30)  NOT NULL,
        id_producto       INT           NOT NULL,
        numero_serie      NVARCHAR(60)  NOT NULL,
        id_laboratorio    INT           NOT NULL,
        estado            NVARCHAR(15)  NOT NULL CONSTRAINT DF_Equipo_Estado DEFAULT 'DISPONIBLE',
        observaciones     NVARCHAR(300) NULL,
        fecha_registro    DATETIME2(0)  NOT NULL CONSTRAINT DF_Equipo_Fecha DEFAULT SYSDATETIME(),
        CONSTRAINT PK_Equipo        PRIMARY KEY (id_equipo),
        CONSTRAINT UQ_Equipo_Codigo UNIQUE (codigo_inventario),
        CONSTRAINT UQ_Equipo_Producto_Serie UNIQUE (id_producto, numero_serie),
        CONSTRAINT UQ_Equipo_Id_Lab UNIQUE (id_equipo, id_laboratorio),
        CONSTRAINT FK_Equipo_Producto FOREIGN KEY (id_producto)    REFERENCES dbo.Producto(id_producto),
        CONSTRAINT FK_Equipo_Lab      FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio),
        CONSTRAINT CK_Equipo_Estado CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA')),
        CONSTRAINT CK_Equipo_Codigo CHECK (LEN(LTRIM(RTRIM(codigo_inventario))) > 0),
        CONSTRAINT CK_Equipo_Serie  CHECK (LEN(LTRIM(RTRIM(numero_serie))) > 0)
    );
    CREATE INDEX IX_Equipo_Lab_Estado ON dbo.Equipo (id_laboratorio, estado);
    CREATE INDEX IX_Equipo_Producto   ON dbo.Equipo (id_producto, id_laboratorio);
END
GO

/* 5. Diagnóstico (solo lectura): columnas que usa el código Java.
      Si alguna sale con existe = 0, su tabla Laboratorio/Usuario difiere: avise a Romario. */
SELECT t.tabla, t.columna,
       CASE WHEN COL_LENGTH('dbo.' + t.tabla, t.columna) IS NULL THEN 0 ELSE 1 END AS existe
FROM (VALUES
  ('Laboratorio','id_laboratorio'),('Laboratorio','codigo'),('Laboratorio','nombre'),
  ('Laboratorio','ubicacion'),('Laboratorio','capacidad'),('Laboratorio','estado'),
  ('Laboratorio','fecha_creacion'),
  ('Usuario','id_laboratorio')
) AS t(tabla, columna)
ORDER BY existe, t.tabla;
GO
