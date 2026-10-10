# Paso 3: Login, registro, reinicio de contraseña, menú y servicio facial

## Flujo de la aplicación
`Main` -> `Navegador.mostrarLogin()` -> según el rol:

| Rol | Pantalla |
|---|---|
| ADMIN | `MenuPrincipalForm` (botones de todos los módulos) |
| ENCARGADO | `InicioEncargadoForm` ("Bienvenido usuario - nombre - ID"; la hará otro integrante) |
| DOCENTE / ESTUDIANTE | `InicioEstudianteForm` ("Bienvenido ..."; la hará otro integrante) |

Para cambiar de pantalla basta con editar `Navegador.entrar(...)`.

## Pestañas del login (`LoginForm`)
1. **Ingresar**: usuario + contraseña **+ reconocimiento facial, obligatorios para TODOS los roles**. Primero se valida la contraseña (si es incorrecta no se abre la cámara) y después el rostro. Sin el servicio facial activo nadie entra.
2. **Regístrate**: nombre, apellido, usuario, correo y contraseña; luego se abre la cámara.
   - Siempre se crea como **ESTUDIANTE**. Se muestra: "Si usted es colaborador de la institución por favor contactarse con el administrador."
   - Si el rostro no se captura, la cuenta se **deshace** (es la única eliminación física del sistema) para que pueda reintentar.
   - Si el servicio facial no está activo, ni siquiera se crea la cuenta.
3. **Reiniciar contraseña**: usuario + correo + rostro. Solo si el rostro coincide se habilitan los campos de la contraseña nueva. Cambiar de pestaña cancela el proceso.

## Seguridad
- Mensaje único "Usuario o contraseña incorrectos" (no revela cuál falló) y BCrypt se ejecuta aunque el usuario no exista.
- El hash de la contraseña solo lo lee `UsuarioRepository.buscarParaLogin`; no sale de `AuthService`.
- Java nunca envía contraseñas ni imágenes al servicio facial: solo el id del usuario.
- Usuarios INACTIVOS no pueden ingresar, ni registrar rostro, ni reiniciar contraseña.

## Servicio facial desde Java (`ServicioFacialForm`)
Botón **"Servicio facial..."** en el login y en el menú del administrador:
1. Indique la carpeta `facial-service` (se detecta sola si ejecuta desde la raíz del proyecto).
2. **1. Instalar**: ejecuta el script `setup-facial-*` (necesita internet y tarda varios minutos).
3. **2. Iniciar**: arranca el servicio y espera a que responda. **Detener**: lo apaga.

En **macOS** la instalación y el servicio se abren en la app **Terminal** (casilla "Usar Terminal"):
Homebrew puede pedir la contraseña del Mac y macOS da el permiso de **cámara** a la Terminal.
Si desmarca la casilla, el servicio lo lanza Java y la cámara depende del permiso de IntelliJ.

## Usuarios de prueba (contraseña `Sigelab2026*`)
admin (ADMIN, id 1), enc.lab01/02/03 (ENCARGADO), docente1, estudiante1, estudiante2.

## Primer ingreso: registrar los rostros de los usuarios de prueba
Como ahora el rostro es obligatorio, los usuarios de prueba (que no tienen rostro) deben registrarlo ANTES de ingresar:
1. Inicie el servicio facial (o use `./start-facial.sh`).
2. En la carpeta `facial-service`, para cada usuario: `./enroll-user.sh admin`, `./enroll-user.sh enc.lab01`, etc.
   (también acepta el id: `./enroll-user.sh --user-id 1`).
3. Los usuarios nuevos creados por un administrador se registran con "Registrar rostro" en el CRUD; los que usan "Regístrate" lo registran en el mismo proceso.
