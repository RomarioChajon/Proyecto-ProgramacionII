/* =====================================================================
   SIGELAB - Sistema de Gestión y Control de Laboratorios Universitarios
   Curso 017 - Programación II | UMG
   Script completo de ESTRUCTURA (tablas, restricciones, índices y vistas)
   Motor objetivo: Microsoft SQL Server 2022

   EJECUCIÓN:
     Ejecutar este archivo completo en SQL Server Management Studio (SSMS).
     ADVERTENCIA: si SIGELAB ya existe, se elimina y se crea nuevamente.

   NOTAS DE DISEÑO
   - Las reglas de negocio viven en la capa Service (RN-20). Las
     restricciones de este script son la SEGUNDA línea de defensa:
     aunque el Service falle, la BD nunca acepta datos incoherentes.
   - Estados y condiciones se guardan como texto con CHECK. Se usa
     DANADO (sin Ñ) para que coincida con el enum de Java
     (EstadoEquipo.DANADO) y evitar problemas de codificación.
     En pantalla se puede mostrar "DAÑADO".
   - Contraseñas: se guarda el HASH (BCrypt), nunca el texto plano.
   - Reconocimiento facial: PerfilFacial (embedding) y AuditoriaFacial siguen
     la estructura del proyecto de ejemplo UniversidadAsistenciaDB.
   - "Sede" y "Laboratorio" se tratan como la MISMA entidad.
   ===================================================================== */

-- Los índices filtrados y las vistas exigen estas opciones. Algunos clientes
-- (por ejemplo sqlcmd sin la bandera -I) las traen en OFF y el script fallaría.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

USE master;
GO

IF DB_ID(N'SIGELAB_DB') IS NOT NULL
BEGIN
    ALTER DATABASE SIGELAB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE SIGELAB;
END
GO

CREATE DATABASE SIGELAB_DB;
GO

USE SIGELAB;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

/* =====================================================================
   1. ROL  (catálogo fijo: los IDs se usan en un CHECK de Usuario)
   ===================================================================== */
CREATE TABLE dbo.Rol (
                         id_rol       INT           NOT NULL,            -- sin IDENTITY: IDs fijos
                         nombre       NVARCHAR(30)  NOT NULL,
                         descripcion  NVARCHAR(250) NULL,
                         CONSTRAINT PK_Rol        PRIMARY KEY (id_rol),
                         CONSTRAINT UQ_Rol_Nombre UNIQUE (nombre),
                         CONSTRAINT CK_Rol_Nombre CHECK (nombre IN ('ADMIN','ENCARGADO','DOCENTE','ESTUDIANTE'))
);
GO

/* =====================================================================
   2. LABORATORIO (= sede)           RN-03: código único
   ===================================================================== */
CREATE TABLE dbo.Laboratorio (
                                 id_laboratorio INT IDENTITY(1,1) NOT NULL,
                                 codigo         NVARCHAR(20)  NOT NULL,
                                 nombre         NVARCHAR(100) NOT NULL,
                                 ubicacion      NVARCHAR(200) NOT NULL,           -- Excel: "tendrá ubicación"
                                 capacidad      INT           NOT NULL,
                                 estado         NVARCHAR(10)  NOT NULL CONSTRAINT DF_Laboratorio_Estado DEFAULT 'ACTIVO',
                                 fecha_creacion DATETIME2(0)  NOT NULL CONSTRAINT DF_Laboratorio_Fecha  DEFAULT SYSDATETIME(),
                                 CONSTRAINT PK_Laboratorio        PRIMARY KEY (id_laboratorio),
                                 CONSTRAINT UQ_Laboratorio_Codigo UNIQUE (codigo),                       -- RN-03
                                 CONSTRAINT CK_Laboratorio_Capacidad CHECK (capacidad > 0),
                                 CONSTRAINT CK_Laboratorio_Estado    CHECK (estado IN ('ACTIVO','INACTIVO')),
                                 CONSTRAINT CK_Laboratorio_Codigo    CHECK (LEN(LTRIM(RTRIM(codigo))) > 0)
);
GO

/* =====================================================================
   3. TIPO_EQUIPO (catálogo)
   ===================================================================== */
CREATE TABLE dbo.TipoEquipo (
                                id_tipo_equipo INT IDENTITY(1,1) NOT NULL,
                                nombre         NVARCHAR(50)  NOT NULL,
                                descripcion    NVARCHAR(200) NULL,
                                estado         NVARCHAR(10)  NOT NULL CONSTRAINT DF_TipoEquipo_Estado DEFAULT 'ACTIVO',
                                CONSTRAINT PK_TipoEquipo        PRIMARY KEY (id_tipo_equipo),
                                CONSTRAINT UQ_TipoEquipo_Nombre UNIQUE (nombre),
                                CONSTRAINT CK_TipoEquipo_Estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);
GO

/* =====================================================================
   4. USUARIO
   RN-01: estado ACTIVO/INACTIVO (desactivación lógica)
   Excel: el ENCARGADO debe estar asignado a UN laboratorio.
   ===================================================================== */
CREATE TABLE dbo.Usuario (
                             id_usuario     INT IDENTITY(1,1) NOT NULL,
                             nombre         NVARCHAR(60)  NOT NULL,
                             apellido       NVARCHAR(60)  NOT NULL,
                             nombre_usuario NVARCHAR(30)  NOT NULL,
                             password_hash  NVARCHAR(100) NOT NULL,           -- hash BCrypt (60 caracteres)
                             correo         NVARCHAR(120) NOT NULL,
                             id_rol         INT           NOT NULL,
                             id_laboratorio INT           NULL,               -- solo para ENCARGADO
                             estado         NVARCHAR(10)  NOT NULL CONSTRAINT DF_Usuario_Estado DEFAULT 'ACTIVO',
                             fecha_creacion DATETIME2(0)  NOT NULL CONSTRAINT DF_Usuario_Fecha  DEFAULT SYSDATETIME(),
                             CONSTRAINT PK_Usuario        PRIMARY KEY (id_usuario),
                             CONSTRAINT UQ_Usuario_Nombre UNIQUE (nombre_usuario),
                             CONSTRAINT UQ_Usuario_Correo UNIQUE (correo),    -- el correo se usa para reiniciar contraseña
                             CONSTRAINT FK_Usuario_Rol          FOREIGN KEY (id_rol)         REFERENCES dbo.Rol(id_rol),
                             CONSTRAINT FK_Usuario_Laboratorio  FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio),
                             CONSTRAINT CK_Usuario_Estado CHECK (estado IN ('ACTIVO','INACTIVO')),
                             CONSTRAINT CK_Usuario_Correo CHECK (correo LIKE '%_@_%._%'),
    -- id_rol = 2 es ENCARGADO. Solo el encargado lleva laboratorio y lo lleva obligatoriamente.
                             CONSTRAINT CK_Usuario_LabEncargado CHECK (
                                 (id_rol = 2 AND id_laboratorio IS NOT NULL)
                                     OR (id_rol <> 2 AND id_laboratorio IS NULL)
                                 )
);
GO

-- Un laboratorio tiene UN encargado de bodega ACTIVO (Excel: "tendrá un encargado de bodega").
-- Si en el futuro se permiten varios encargados por laboratorio, basta con eliminar este índice.
CREATE UNIQUE INDEX UX_Usuario_EncargadoActivoPorLab
    ON dbo.Usuario (id_laboratorio)
    WHERE id_laboratorio IS NOT NULL AND estado = 'ACTIVO';
GO

/* =====================================================================
   5. PERFIL_FACIAL  (Usuario 1 ---- 0..1 PerfilFacial)
   IDÉNTICA a la tabla que usa el servicio facial (facial-service/sql/
   001_create_facial_tables.sql): mismos tipos y tamaños, porque el servicio
   Python la lee y la escribe directamente.
   Se guarda el EMBEDDING del rostro (vector), no la foto. El servicio hace
   "actualizar si existe, si no insertar", por eso hay UN solo perfil por
   usuario (UNIQUE id_usuario). Excel: el ADMIN registra/actualiza la foto.
   ===================================================================== */
CREATE TABLE dbo.PerfilFacial (
                                  id_perfil_facial    INT IDENTITY(1,1) NOT NULL,
                                  id_usuario          INT            NOT NULL,
                                  embedding           VARBINARY(MAX) NOT NULL,
                                  modelo              NVARCHAR(100)  NOT NULL,
                                  dimension           INT            NOT NULL,
                                  fecha_registro      DATETIME2(0)   NOT NULL CONSTRAINT DF_PerfilFacial_FechaRegistro DEFAULT SYSDATETIME(),
                                  fecha_actualizacion DATETIME2(0)   NOT NULL CONSTRAINT DF_PerfilFacial_FechaActualizacion DEFAULT SYSDATETIME(),
                                  activo              BIT            NOT NULL CONSTRAINT DF_PerfilFacial_Activo DEFAULT 1,
                                  cantidad_muestras   INT            NOT NULL,
                                  quality_score       FLOAT          NULL,
                                  version_algoritmo   NVARCHAR(50)   NOT NULL,
                                  CONSTRAINT PK_PerfilFacial PRIMARY KEY (id_perfil_facial),
                                  CONSTRAINT UQ_PerfilFacial_Usuario UNIQUE (id_usuario),          -- RN-02: un perfil por usuario
                                  CONSTRAINT FK_PerfilFacial_Usuario FOREIGN KEY (id_usuario) REFERENCES dbo.Usuario(id_usuario),
                                  CONSTRAINT CK_PerfilFacial_Dimension CHECK (dimension > 0),
                                  CONSTRAINT CK_PerfilFacial_Muestras  CHECK (cantidad_muestras > 0)
);
GO

/* =====================================================================
   3B. PRODUCTO (catálogo: el "código de producto")
   Un PRODUCTO es el modelo (ej. Laptop Dell Latitude 3420) y agrupa N
   unidades físicas (Equipo), cada una con su propio número de serie.
   Stock de un producto en una sede = cantidad de Equipo de ese producto
   en esa sede (ver vista vw_stock_laboratorio). El stock NO se guarda
   como número: se cuenta, así nunca puede quedar desfasado.
   ===================================================================== */
CREATE TABLE dbo.Producto (
                              id_producto     INT IDENTITY(1,1) NOT NULL,
                              codigo_producto NVARCHAR(30)  NOT NULL,
                              nombre          NVARCHAR(100) NOT NULL,
                              id_tipo_equipo  INT           NOT NULL,           -- categoría
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
GO

/* =====================================================================
   6. EQUIPO = UNA UNIDAD FÍSICA de un producto
   RN-04: código de inventario único por unidad (es el valor del QR)
   Ingreso de stock: se registra UNA unidad a la vez, eligiendo
   producto + sede y capturando su número de serie (obligatorio).
   Estados: los cinco obligatorios
   ===================================================================== */
CREATE TABLE dbo.Equipo (
                            id_equipo         INT IDENTITY(1,1) NOT NULL,
                            codigo_inventario NVARCHAR(30)  NOT NULL,
                            id_producto       INT           NOT NULL,
                            numero_serie      NVARCHAR(60)  NOT NULL,          -- obligatorio: cada unidad tiene su serie
                            id_laboratorio    INT           NOT NULL,          -- sede donde se ingresó el stock
                            estado            NVARCHAR(15)  NOT NULL CONSTRAINT DF_Equipo_Estado DEFAULT 'DISPONIBLE',
                            observaciones     NVARCHAR(300) NULL,
                            fecha_registro    DATETIME2(0)  NOT NULL CONSTRAINT DF_Equipo_Fecha DEFAULT SYSDATETIME(),
                            CONSTRAINT PK_Equipo        PRIMARY KEY (id_equipo),
                            CONSTRAINT UQ_Equipo_Codigo UNIQUE (codigo_inventario),                  -- RN-04
    -- La misma serie no se repite dentro del mismo producto
                            CONSTRAINT UQ_Equipo_Producto_Serie UNIQUE (id_producto, numero_serie),
    -- Soporta la FK compuesta de DetallePrestamo (un préstamo = una sola sede)
                            CONSTRAINT UQ_Equipo_Id_Lab UNIQUE (id_equipo, id_laboratorio),
                            CONSTRAINT FK_Equipo_Producto FOREIGN KEY (id_producto)    REFERENCES dbo.Producto(id_producto),
                            CONSTRAINT FK_Equipo_Lab      FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio),
                            CONSTRAINT CK_Equipo_Estado CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA')),
                            CONSTRAINT CK_Equipo_Codigo CHECK (LEN(LTRIM(RTRIM(codigo_inventario))) > 0),
                            CONSTRAINT CK_Equipo_Serie  CHECK (LEN(LTRIM(RTRIM(numero_serie))) > 0)
);
GO

CREATE INDEX IX_Equipo_Lab_Estado ON dbo.Equipo (id_laboratorio, estado);
CREATE INDEX IX_Equipo_Producto   ON dbo.Equipo (id_producto, id_laboratorio);
GO

/* =====================================================================
   7. PRESTAMO (encabezado)
   FLUJO DE ESTADOS
     PENDIENTE   El docente/estudiante lo solicitó. La pantalla le muestra:
                 "Pendiente de revisión por el encargado de bodega".
        |
     EN_TRAMITE  El encargado de la sede lo abrió para revisarlo (queda
                 registrado quién y cuándo). Los equipos siguen DISPONIBLES.
        |
     DESPACHADO  El encargado lo validó, fijó los días y entregó los equipos
                 (los equipos pasan a PRESTADO). Es el antiguo "ACTIVO".
        |
     FINALIZADO  Todos los equipos fueron devueltos.

     RECHAZADO   El encargado lo rechazó (motivo obligatorio).
     CANCELADO   El propio solicitante lo canceló antes del despacho.

   "VENCIDO" NO se guarda: se calcula (RN-17) con fecha_esperada y equipos
   pendientes. "Préstamo activo" del PDF = DESPACHADO con equipos pendientes.
   Excel: el encargado pone los días; el préstamo pertenece a UNA sede.
   ===================================================================== */
CREATE TABLE dbo.Prestamo (
                              id_prestamo               INT IDENTITY(1,1) NOT NULL,
                              id_solicitante            INT           NOT NULL,   -- estudiante o docente (lo valida el Service)
                              id_laboratorio            INT           NOT NULL,   -- la sede elegida
                              fecha_prestamo            DATETIME2(0)  NOT NULL CONSTRAINT DF_Prestamo_Fecha DEFAULT SYSDATETIME(), -- fecha de solicitud
                              id_usuario_registra       INT           NOT NULL,   -- quién creó el registro
    -- Revisión (EN_TRAMITE)
                              id_usuario_revisa         INT           NULL,       -- encargado que abrió la solicitud
                              fecha_revision            DATETIME2(0)  NULL,
    -- Despacho (DESPACHADO)
                              dias_prestamo             INT           NULL,       -- lo define el encargado
                              fecha_despacho            DATETIME2(0)  NULL,       -- cuando se entregan los equipos
                              fecha_esperada_devolucion DATETIME2(0)  NULL,       -- fecha_despacho + dias_prestamo
                              id_usuario_despacha       INT           NULL,       -- encargado que valida y entrega
    -- Cierre
                              fecha_cierre              DATETIME2(0)  NULL,       -- cuando se devuelve el último equipo
                              estado                    NVARCHAR(12)  NOT NULL CONSTRAINT DF_Prestamo_Estado DEFAULT 'PENDIENTE',
                              motivo_rechazo            NVARCHAR(300) NULL,
                              observaciones             NVARCHAR(300) NULL,
                              CONSTRAINT PK_Prestamo PRIMARY KEY (id_prestamo),
    -- Soporta la FK compuesta de DetallePrestamo
                              CONSTRAINT UQ_Prestamo_Id_Lab UNIQUE (id_prestamo, id_laboratorio),
                              CONSTRAINT FK_Prestamo_Solicitante FOREIGN KEY (id_solicitante)      REFERENCES dbo.Usuario(id_usuario),
                              CONSTRAINT FK_Prestamo_Lab         FOREIGN KEY (id_laboratorio)      REFERENCES dbo.Laboratorio(id_laboratorio),
                              CONSTRAINT FK_Prestamo_Registra    FOREIGN KEY (id_usuario_registra) REFERENCES dbo.Usuario(id_usuario),
                              CONSTRAINT FK_Prestamo_Revisa      FOREIGN KEY (id_usuario_revisa)   REFERENCES dbo.Usuario(id_usuario),
                              CONSTRAINT FK_Prestamo_Despacha    FOREIGN KEY (id_usuario_despacha) REFERENCES dbo.Usuario(id_usuario),
                              CONSTRAINT CK_Prestamo_Estado CHECK (estado IN ('PENDIENTE','EN_TRAMITE','DESPACHADO','FINALIZADO','RECHAZADO','CANCELADO')),
                              CONSTRAINT CK_Prestamo_Dias   CHECK (dias_prestamo IS NULL OR dias_prestamo > 0),
    -- Todo préstamo que pasó de PENDIENTE (salvo cancelado) tiene encargado y fecha de revisión
                              CONSTRAINT CK_Prestamo_Revision CHECK (
                                  estado IN ('PENDIENTE','CANCELADO')
                                      OR (id_usuario_revisa IS NOT NULL AND fecha_revision IS NOT NULL)
                                  ),
    -- DESPACHADO o FINALIZADO ya tiene días, fechas y encargado que despachó
                              CONSTRAINT CK_Prestamo_Despachado CHECK (
                                  estado IN ('PENDIENTE','EN_TRAMITE','RECHAZADO','CANCELADO')
                                      OR (dias_prestamo IS NOT NULL AND fecha_despacho IS NOT NULL
                                      AND fecha_esperada_devolucion IS NOT NULL AND id_usuario_despacha IS NOT NULL)
                                  ),
    -- Un rechazo siempre explica el motivo; los demás estados no lo llevan
                              CONSTRAINT CK_Prestamo_Rechazo CHECK (
                                  (estado = 'RECHAZADO' AND motivo_rechazo IS NOT NULL AND LEN(LTRIM(RTRIM(motivo_rechazo))) > 0)
                                      OR (estado <> 'RECHAZADO' AND motivo_rechazo IS NULL)
                                  ),
    -- Fechas coherentes
                              CONSTRAINT CK_Prestamo_FechaRevision CHECK (fecha_revision IS NULL OR fecha_revision >= fecha_prestamo),
                              CONSTRAINT CK_Prestamo_FechaDespacho CHECK (
                                  fecha_despacho IS NULL
                                      OR (fecha_despacho >= fecha_prestamo AND (fecha_revision IS NULL OR fecha_despacho >= fecha_revision))
                                  ),
                              CONSTRAINT CK_Prestamo_FechaEsperada CHECK (fecha_esperada_devolucion IS NULL OR fecha_esperada_devolucion >= fecha_despacho),
                              CONSTRAINT CK_Prestamo_FechaCierre   CHECK (
                                  (estado = 'FINALIZADO' AND fecha_cierre IS NOT NULL AND fecha_cierre >= fecha_despacho)
                                      OR (estado <> 'FINALIZADO' AND fecha_cierre IS NULL)
                                  )
);
GO

CREATE INDEX IX_Prestamo_Solicitante ON dbo.Prestamo (id_solicitante, estado);
CREATE INDEX IX_Prestamo_Lab_Estado  ON dbo.Prestamo (id_laboratorio, estado);
CREATE INDEX IX_Prestamo_Fecha       ON dbo.Prestamo (fecha_prestamo);
-- Cola de trabajo del encargado: solicitudes por atender de su sede, las más antiguas primero
CREATE INDEX IX_Prestamo_ColaDespacho ON dbo.Prestamo (id_laboratorio, fecha_prestamo)
    WHERE estado IN ('PENDIENTE','EN_TRAMITE');
GO

/* =====================================================================
   8. DETALLE_PRESTAMO  (Prestamo 1..N DetallePrestamo N..1 Equipo)
   RN-05: el Service exige >= 1 detalle antes de confirmar
   RN-06: UNIQUE (id_prestamo, id_equipo) -> el equipo no se repite
   Excel: NO puede haber préstamos con equipos de varias sedes.
          id_laboratorio se repite a propósito: las dos FK COMPUESTAS
          obligan a que préstamo y equipo pertenezcan a la MISMA sede.
   ===================================================================== */
CREATE TABLE dbo.DetallePrestamo (
                                     id_detalle     INT IDENTITY(1,1) NOT NULL,
                                     id_prestamo    INT NOT NULL,
                                     id_equipo      INT NOT NULL,
                                     id_laboratorio INT NOT NULL,
                                     CONSTRAINT PK_DetallePrestamo PRIMARY KEY (id_detalle),
                                     CONSTRAINT UQ_Detalle_Prestamo_Equipo UNIQUE (id_prestamo, id_equipo),   -- RN-06
                                     CONSTRAINT FK_Detalle_Prestamo FOREIGN KEY (id_prestamo, id_laboratorio)
                                         REFERENCES dbo.Prestamo (id_prestamo, id_laboratorio),
                                     CONSTRAINT FK_Detalle_Equipo   FOREIGN KEY (id_equipo, id_laboratorio)
                                         REFERENCES dbo.Equipo (id_equipo, id_laboratorio)
);
GO

CREATE INDEX IX_Detalle_Equipo ON dbo.DetallePrestamo (id_equipo);
GO

/* =====================================================================
   9. DEVOLUCION  (una fila por equipo devuelto)
   RN-10/RN-11: UNIQUE (id_detalle) -> un equipo no se devuelve dos veces,
                y solo se devuelve si existe en el detalle de un préstamo.
   RN-12/RN-13: condición BUENO -> DISPONIBLE ; DANADO -> DANADO (Service)
   Equipo pendiente = detalle SIN fila en Devolucion.
   Devolución parcial = algunos detalles con fila y otros sin ella.
   ===================================================================== */
CREATE TABLE dbo.Devolucion (
                                id_devolucion    INT IDENTITY(1,1) NOT NULL,
                                id_detalle       INT           NOT NULL,
                                fecha_devolucion DATETIME2(0)  NOT NULL CONSTRAINT DF_Devolucion_Fecha DEFAULT SYSDATETIME(),
                                condicion        NVARCHAR(10)  NOT NULL,
                                observacion      NVARCHAR(300) NULL,
                                id_usuario_recibe INT          NOT NULL,           -- encargado que recibe
                                CONSTRAINT PK_Devolucion         PRIMARY KEY (id_devolucion),
                                CONSTRAINT UQ_Devolucion_Detalle UNIQUE (id_detalle),                    -- RN-11
                                CONSTRAINT FK_Devolucion_Detalle FOREIGN KEY (id_detalle)         REFERENCES dbo.DetallePrestamo(id_detalle),
                                CONSTRAINT FK_Devolucion_Recibe  FOREIGN KEY (id_usuario_recibe)  REFERENCES dbo.Usuario(id_usuario),
                                CONSTRAINT CK_Devolucion_Condicion CHECK (condicion IN ('BUENO','DANADO'))
);
GO

/* =====================================================================
   10. BLOQUEO_USUARIO
   Excel: estudiante/docente queda BLOQUEADO si
          - devuelve un equipo DAÑADO        (motivo EQUIPO_DANADO)
          - se pasa del tiempo               (motivo VENCIDO)
          - entrega parcial                  (motivo PARCIAL)
   Se desbloquea con la devolución completa (VENCIDO y PARCIAL, automático)
   o manualmente por ENCARGADO/ADMIN (EQUIPO_DANADO).
   Usuario bloqueado = tiene al menos una fila con estado ACTIVO.
   ===================================================================== */
CREATE TABLE dbo.BloqueoUsuario (
                                    id_bloqueo            INT IDENTITY(1,1) NOT NULL,
                                    id_usuario            INT           NOT NULL,
                                    id_prestamo           INT           NOT NULL,
                                    motivo                NVARCHAR(15)  NOT NULL,
                                    estado                NVARCHAR(10)  NOT NULL CONSTRAINT DF_Bloqueo_Estado DEFAULT 'ACTIVO',
                                    fecha_bloqueo         DATETIME2(0)  NOT NULL CONSTRAINT DF_Bloqueo_Fecha  DEFAULT SYSDATETIME(),
                                    fecha_liberacion      DATETIME2(0)  NULL,
                                    tipo_liberacion       NVARCHAR(20)  NULL,
                                    id_usuario_libera     INT           NULL,
                                    observacion_liberacion NVARCHAR(300) NULL,
                                    CONSTRAINT PK_BloqueoUsuario PRIMARY KEY (id_bloqueo),
                                    CONSTRAINT FK_Bloqueo_Usuario  FOREIGN KEY (id_usuario)        REFERENCES dbo.Usuario(id_usuario),
                                    CONSTRAINT FK_Bloqueo_Prestamo FOREIGN KEY (id_prestamo)       REFERENCES dbo.Prestamo(id_prestamo),
                                    CONSTRAINT FK_Bloqueo_Libera   FOREIGN KEY (id_usuario_libera) REFERENCES dbo.Usuario(id_usuario),
                                    CONSTRAINT CK_Bloqueo_Motivo CHECK (motivo IN ('EQUIPO_DANADO','VENCIDO','PARCIAL')),
                                    CONSTRAINT CK_Bloqueo_Estado CHECK (estado IN ('ACTIVO','LIBERADO')),
                                    CONSTRAINT CK_Bloqueo_Tipo   CHECK (tipo_liberacion IS NULL OR tipo_liberacion IN ('DEVOLUCION_COMPLETA','MANUAL')),
                                    CONSTRAINT CK_Bloqueo_Liberacion CHECK (
                                        (estado = 'ACTIVO'   AND fecha_liberacion IS NULL AND tipo_liberacion IS NULL AND id_usuario_libera IS NULL)
                                            OR
                                        (estado = 'LIBERADO' AND fecha_liberacion IS NOT NULL AND tipo_liberacion IS NOT NULL AND id_usuario_libera IS NOT NULL)
                                        )
);
GO

-- No se duplica un mismo bloqueo activo (mismo préstamo y mismo motivo).
CREATE UNIQUE INDEX UX_Bloqueo_ActivoPorPrestamoMotivo
    ON dbo.BloqueoUsuario (id_prestamo, motivo)
    WHERE estado = 'ACTIVO';
CREATE INDEX IX_Bloqueo_Usuario_Estado ON dbo.BloqueoUsuario (id_usuario, estado);
GO

/* =====================================================================
   11. MANTENIMIENTO  (Equipo 1..N Mantenimiento)
   RN-14: equipo en MANTENIMIENTO no se presta (Service)
   RN-15: al finalizar -> DISPONIBLE (REPARADO) o BAJA (NO_REPARABLE)
   ===================================================================== */
CREATE TABLE dbo.Mantenimiento (
                                   id_mantenimiento    INT IDENTITY(1,1) NOT NULL,
                                   id_equipo           INT           NOT NULL,
                                   tipo                NVARCHAR(12)  NOT NULL,
                                   descripcion         NVARCHAR(300) NOT NULL,
                                   responsable         NVARCHAR(100) NOT NULL,       -- técnico (puede ser externo)
                                   fecha_ingreso       DATETIME2(0)  NOT NULL CONSTRAINT DF_Mant_Ingreso DEFAULT SYSDATETIME(),
                                   fecha_salida        DATETIME2(0)  NULL,
                                   resultado           NVARCHAR(15)  NULL,
                                   estado              NVARCHAR(10)  NOT NULL CONSTRAINT DF_Mant_Estado DEFAULT 'EN_PROCESO',
                                   observaciones       NVARCHAR(300) NULL,
                                   id_usuario_registra INT           NOT NULL,
                                   id_usuario_cierra   INT           NULL,
                                   CONSTRAINT PK_Mantenimiento PRIMARY KEY (id_mantenimiento),
                                   CONSTRAINT FK_Mant_Equipo   FOREIGN KEY (id_equipo)           REFERENCES dbo.Equipo(id_equipo),
                                   CONSTRAINT FK_Mant_Registra FOREIGN KEY (id_usuario_registra) REFERENCES dbo.Usuario(id_usuario),
                                   CONSTRAINT FK_Mant_Cierra   FOREIGN KEY (id_usuario_cierra)   REFERENCES dbo.Usuario(id_usuario),
                                   CONSTRAINT CK_Mant_Tipo      CHECK (tipo IN ('PREVENTIVO','CORRECTIVO')),
                                   CONSTRAINT CK_Mant_Estado    CHECK (estado IN ('EN_PROCESO','FINALIZADO')),
                                   CONSTRAINT CK_Mant_Resultado CHECK (resultado IS NULL OR resultado IN ('REPARADO','NO_REPARABLE')),
                                   CONSTRAINT CK_Mant_Cierre CHECK (
                                       (estado = 'EN_PROCESO' AND fecha_salida IS NULL AND resultado IS NULL AND id_usuario_cierra IS NULL)
                                           OR
                                       (estado = 'FINALIZADO' AND fecha_salida IS NOT NULL AND resultado IS NOT NULL
                                           AND id_usuario_cierra IS NOT NULL AND fecha_salida >= fecha_ingreso)
                                       )
);
GO

-- Un equipo no puede tener dos mantenimientos abiertos a la vez.
CREATE UNIQUE INDEX UX_Mant_UnoEnProcesoPorEquipo
    ON dbo.Mantenimiento (id_equipo)
    WHERE estado = 'EN_PROCESO';
CREATE INDEX IX_Mant_Equipo ON dbo.Mantenimiento (id_equipo, fecha_ingreso);
GO

/* =====================================================================
   12. AUDITORIA     RN-18 + Excel: "toda transacción deja registro"
   id_usuario es NULL cuando el login falla con un usuario inexistente;
   en ese caso se guarda el texto digitado en usuario_intento.
   ===================================================================== */
CREATE TABLE dbo.Auditoria (
                               id_auditoria    BIGINT IDENTITY(1,1) NOT NULL,
                               id_usuario      INT           NULL,
                               usuario_intento NVARCHAR(60)  NULL,
                               id_laboratorio  INT           NULL,               -- sede afectada (filtrar por encargado)
                               fecha_hora      DATETIME2(0)  NOT NULL CONSTRAINT DF_Auditoria_Fecha DEFAULT SYSDATETIME(),
                               accion          NVARCHAR(50)  NOT NULL,           -- ej. PRESTAMO_CREADO (enum AccionAuditoria en Java)
                               entidad         NVARCHAR(40)  NOT NULL,           -- ej. Prestamo, Equipo, Usuario
                               id_registro     INT           NULL,
                               descripcion     NVARCHAR(500) NOT NULL,
                               CONSTRAINT PK_Auditoria PRIMARY KEY (id_auditoria),
                               CONSTRAINT FK_Auditoria_Usuario FOREIGN KEY (id_usuario)     REFERENCES dbo.Usuario(id_usuario),
                               CONSTRAINT FK_Auditoria_Lab     FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio)
);
GO

CREATE INDEX IX_Auditoria_Fecha    ON dbo.Auditoria (fecha_hora);
CREATE INDEX IX_Auditoria_Entidad  ON dbo.Auditoria (entidad, id_registro);
CREATE INDEX IX_Auditoria_Usuario  ON dbo.Auditoria (id_usuario);
GO

/* =====================================================================
   13. AUDITORIA_FACIAL  (detalle técnico de cada intento facial)
   IDÉNTICA a la del servicio facial (mismos tipos, tamaños y CHECK de accion).
   OJO: el servicio recorta detalle a 1000 y dispositivo a 200 caracteres y,
   si el INSERT falla, solo lo escribe en su log (no avisa). Por eso los
   tamaños deben coincidir exactamente: si fueran menores, se perdería
   auditoría sin ningún error visible.
   Complementa a Auditoria: Auditoria dice QUÉ pasó (LOGIN_EXITOSO /
   LOGIN_FALLIDO); AuditoriaFacial guarda score, umbral y dispositivo.
   ===================================================================== */
CREATE TABLE dbo.AuditoriaFacial (
                                     id_auditoria BIGINT IDENTITY(1,1) NOT NULL,
                                     id_usuario   INT            NULL,                 -- NULL si el usuario no se pudo identificar
                                     fecha_hora   DATETIME2(0)   NOT NULL CONSTRAINT DF_AuditoriaFacial_Fecha DEFAULT SYSDATETIME(),
                                     accion       VARCHAR(30)    NOT NULL,
                                     resultado    VARCHAR(20)    NOT NULL,
                                     score        FLOAT          NULL,
                                     threshold    FLOAT          NULL,
                                     detalle      NVARCHAR(1000) NULL,
                                     dispositivo  NVARCHAR(200)  NULL,
                                     CONSTRAINT PK_AuditoriaFacial PRIMARY KEY (id_auditoria),
                                     CONSTRAINT FK_AuditoriaFacial_Usuario FOREIGN KEY (id_usuario)
                                         REFERENCES dbo.Usuario(id_usuario) ON DELETE SET NULL,
                                     CONSTRAINT CK_AuditoriaFacial_Accion CHECK
                                         (accion IN ('ENROLL_INICIADO', 'ENROLL_EXITOSO', 'ENROLL_FALLIDO',
                                                     'VERIFY_INICIADO', 'VERIFY_EXITOSO', 'VERIFY_FALLIDO',
                                                     'LIVENESS_FALLIDO', 'CAMARA_NO_DISPONIBLE', 'PERFIL_ELIMINADO'))
);
GO

CREATE INDEX IX_AuditoriaFacial_UsuarioFecha ON dbo.AuditoriaFacial (id_usuario, fecha_hora DESC);
GO

/* =====================================================================
   VISTAS DE CONSULTA (solo SELECT; el Repository las consulta)
   ===================================================================== */

-- Excel: "ambos roles pueden consultar las sedes y el stock antes de prestar"
-- Stock = cantidad de unidades (Equipo) por sede y producto.
CREATE VIEW dbo.vw_stock_laboratorio AS
SELECT  l.id_laboratorio,
        l.codigo        AS codigo_laboratorio,
        l.nombre        AS laboratorio,
        l.ubicacion,
        l.estado        AS estado_laboratorio,
        pr.id_producto,
        pr.codigo_producto,
        pr.nombre       AS producto,
        t.id_tipo_equipo,
        t.nombre        AS tipo_equipo,
        COUNT(e.id_equipo)                                          AS total,
        SUM(CASE WHEN e.estado = 'DISPONIBLE'    THEN 1 ELSE 0 END) AS disponibles,
        SUM(CASE WHEN e.estado = 'PRESTADO'      THEN 1 ELSE 0 END) AS prestados,
        SUM(CASE WHEN e.estado = 'DANADO'        THEN 1 ELSE 0 END) AS danados,
        SUM(CASE WHEN e.estado = 'MANTENIMIENTO' THEN 1 ELSE 0 END) AS en_mantenimiento,
        SUM(CASE WHEN e.estado = 'BAJA'          THEN 1 ELSE 0 END) AS de_baja
FROM dbo.Laboratorio l
         LEFT JOIN dbo.Equipo     e  ON e.id_laboratorio = l.id_laboratorio
         LEFT JOIN dbo.Producto   pr ON pr.id_producto   = e.id_producto
         LEFT JOIN dbo.TipoEquipo t  ON t.id_tipo_equipo = pr.id_tipo_equipo
GROUP BY l.id_laboratorio, l.codigo, l.nombre, l.ubicacion, l.estado,
         pr.id_producto, pr.codigo_producto, pr.nombre, t.id_tipo_equipo, t.nombre;
GO

-- Resumen de préstamos con pendientes y estado calculado (RN-16, RN-17)
CREATE VIEW dbo.vw_prestamos_resumen AS
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
            AND p.fecha_esperada_devolucion < SYSDATETIME()
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

-- COLA DEL ENCARGADO: préstamos por atender (pendientes y en trámite).
-- El Repository filtra por la sede del encargado: WHERE id_laboratorio = ?
-- (el ADMIN consulta sin filtro). Ordenar por fecha_prestamo ASC.
CREATE VIEW dbo.vw_cola_despacho AS
SELECT  p.id_prestamo,
        p.id_laboratorio,
        l.nombre AS laboratorio,
        p.id_solicitante,
        CONCAT(u.nombre, ' ', u.apellido) AS solicitante,
        r.nombre AS rol_solicitante,
        p.estado,
        p.fecha_prestamo,
        p.id_usuario_revisa,
        CONCAT(ur.nombre, ' ', ur.apellido) AS revisado_por,
        p.fecha_revision,
        p.observaciones,
        COUNT(d.id_detalle) AS total_equipos
FROM dbo.Prestamo p
         JOIN dbo.Laboratorio l ON l.id_laboratorio = p.id_laboratorio
         JOIN dbo.Usuario     u ON u.id_usuario     = p.id_solicitante
         JOIN dbo.Rol         r ON r.id_rol         = u.id_rol
         LEFT JOIN dbo.Usuario ur ON ur.id_usuario  = p.id_usuario_revisa
         LEFT JOIN dbo.DetallePrestamo d ON d.id_prestamo = p.id_prestamo
WHERE p.estado IN ('PENDIENTE','EN_TRAMITE')
GROUP BY p.id_prestamo, p.id_laboratorio, l.nombre, p.id_solicitante, u.nombre, u.apellido,
         r.nombre, p.estado, p.fecha_prestamo, p.id_usuario_revisa, ur.nombre, ur.apellido,
         p.fecha_revision, p.observaciones;
GO

-- Historial de préstamos de un equipo (filtrar con WHERE codigo_inventario = ?)
CREATE VIEW dbo.vw_historial_equipo AS
SELECT  e.id_equipo,
        e.codigo_inventario,
        e.numero_serie,
        pr.nombre AS equipo,
        p.id_prestamo,
        p.estado AS estado_prestamo,
        CONCAT(u.nombre, ' ', u.apellido) AS solicitante,
        p.fecha_despacho,
        p.fecha_esperada_devolucion,
        dv.fecha_devolucion,
        dv.condicion,
        dv.observacion
FROM dbo.Equipo e
         JOIN dbo.Producto        pr ON pr.id_producto = e.id_producto
         JOIN dbo.DetallePrestamo d  ON d.id_equipo    = e.id_equipo
         JOIN dbo.Prestamo        p  ON p.id_prestamo  = d.id_prestamo
         JOIN dbo.Usuario         u  ON u.id_usuario   = p.id_solicitante
         LEFT JOIN dbo.Devolucion dv ON dv.id_detalle  = d.id_detalle;
GO

-- Historial de mantenimientos (filtrar por equipo o por laboratorio)
CREATE VIEW dbo.vw_historial_mantenimiento AS
SELECT  m.id_mantenimiento,
        e.id_equipo,
        e.codigo_inventario,
        e.numero_serie,
        pr.nombre AS equipo,
        e.id_laboratorio,
        m.tipo,
        m.descripcion,
        m.responsable,
        m.fecha_ingreso,
        m.fecha_salida,
        m.resultado,
        m.estado,
        m.observaciones
FROM dbo.Mantenimiento m
         JOIN dbo.Equipo   e  ON e.id_equipo   = m.id_equipo
         JOIN dbo.Producto pr ON pr.id_producto = e.id_producto;
GO