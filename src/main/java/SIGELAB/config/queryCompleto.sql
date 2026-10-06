CREATE DATABASE SIGELAB_DB;
GO

USE SIGELAB_DB;
GO

CREATE TABLE Usuario (
                         id_usuario INT IDENTITY(1,1) PRIMARY KEY,
                         nombre NVARCHAR(100) NOT NULL,
                         apellido NVARCHAR(100) NOT NULL,
                         usuario NVARCHAR(50) NOT NULL UNIQUE,
                         password_hash NVARCHAR(255) NOT NULL,
                         rol NVARCHAR(20) NOT NULL,
                         activo BIT NOT NULL DEFAULT 1,
                         fecha_creacion DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

                         CONSTRAINT CK_Usuario_Rol
                             CHECK (rol IN ('ADMIN', 'ENCARGADO', 'DOCENTE', 'ESTUDIANTE'))
);
GO

CREATE TABLE Laboratorio (
                             id_laboratorio INT IDENTITY(1,1) PRIMARY KEY,
                             nombre NVARCHAR(100) NOT NULL,
                             ubicacion NVARCHAR(150) NOT NULL,
                             descripcion NVARCHAR(255) NULL,
                             activo BIT NOT NULL DEFAULT 1,
                             fecha_creacion DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE Equipo (
                        id_equipo INT IDENTITY(1,1) PRIMARY KEY,
                        codigo NVARCHAR(50) NOT NULL UNIQUE,
                        nombre NVARCHAR(100) NOT NULL,
                        descripcion NVARCHAR(255) NULL,
                        marca NVARCHAR(100) NULL,
                        modelo NVARCHAR(100) NULL,
                        numero_serie NVARCHAR(100) NULL UNIQUE,
                        estado NVARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
                        id_laboratorio INT NOT NULL,
                        activo BIT NOT NULL DEFAULT 1,
                        fecha_creacion DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

                        CONSTRAINT FK_Equipo_Laboratorio
                            FOREIGN KEY (id_laboratorio)
                                REFERENCES Laboratorio(id_laboratorio),

                        CONSTRAINT CK_Equipo_Estado
                            CHECK (estado IN (
                                              'DISPONIBLE',
                                              'PRESTADO',
                                              'DAÑADO',
                                              'EN_MANTENIMIENTO',
                                              'DE_BAJA'
                                ))
);
GO