<#
  build-sigelab.ps1
  Empaqueta SIGELAB completo en una carpeta con SIGELAB.exe:
    - la app Java (Swing, SQL Server, generacion y lectura de QR con ZXing),
    - el servicio facial en Python (rostro + lector de QR por camara) como facial-service.exe,
    - el modelo facial (buffalo_l) para no descargarlo en cada PC,
    - su propio Java (no depende del Java instalado en la PC).

  Antes de ejecutarlo:
    1. IntelliJ: Build > Build Artifacts > sigelab:jar > Rebuild   (deja out\artifacts\sigelab_jar\)
    2. El servicio facial debe funcionar en esta PC (setup-facial.ps1 hecho, .env configurado
       y al menos un registro/verificacion facial hecho, para que el modelo ya este descargado).

  Uso (PowerShell, desde la raiz del proyecto):
    powershell -ExecutionPolicy Bypass -File .\build-sigelab.ps1 -Consola      -> primera prueba, con consola
    powershell -ExecutionPolicy Bypass -File .\build-sigelab.ps1               -> version final
    powershell -ExecutionPolicy Bypass -File .\build-sigelab.ps1 -Instalador   -> ademas instalador .exe (requiere WiX 3.14)
#>
param(
    [string]$Jdk = "C:\Users\Romario Chajon\.jdks\openjdk-22.0.2",
    [string]$Version = "1.0.0",
    [string]$Icono = "",
    [switch]$SinModelo,
    [switch]$Consola,
    [switch]$Instalador
)

$ErrorActionPreference = "Stop"
$raiz     = $PSScriptRoot
$jarDir   = Join-Path $raiz "out\artifacts\sigelab_jar"
$facial   = Join-Path $raiz "facial-service"
$build    = Join-Path $raiz "build"
$entrada  = Join-Path $build "entrada"
$dist     = Join-Path $build "dist"
$jpackage = Join-Path $Jdk "bin\jpackage.exe"
$python   = Join-Path $facial ".venv\Scripts\python.exe"
$envFile  = Join-Path $facial ".env"

function Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }

function Leer-Env($clave, $porDefecto) {
    $linea = Get-Content $envFile | Where-Object { $_ -match "^\s*$clave\s*=" } | Select-Object -First 1
    if ($linea) {
        $valor = ($linea -split "=", 2)[1].Trim()
        if ($valor) { return $valor }
    }
    return $porDefecto
}

# ---------------------------------------------------------------- 0. Requisitos
Paso "Comprobando requisitos"
if (-not (Test-Path $jpackage)) { throw "No se encontro jpackage en $jpackage (revisa el parametro -Jdk)." }
if (-not (Test-Path (Join-Path $jarDir "sigelab.jar"))) {
    throw "No existe out\artifacts\sigelab_jar\sigelab.jar. En IntelliJ: Build > Build Artifacts > sigelab:jar > Rebuild."
}
if (-not (Test-Path $python))  { throw "No se encontro facial-service\.venv. Ejecuta primero facial-service\setup-facial.ps1." }
if (-not (Test-Path $envFile)) { throw "Falta facial-service\.env (DB_PASSWORD y FACIAL_ENCRYPTION_KEY son obligatorios)." }
if (-not (Test-Path (Join-Path $facial "run_service.py"))) { throw "Falta facial-service\run_service.py." }

# Las librerias que pide el MANIFEST deben estar junto a sigelab.jar
$manifest = Join-Path $raiz "src\main\resources\META-INF\MANIFEST.MF"
if (Test-Path $manifest) {
    $texto = (Get-Content $manifest -Raw) -replace "\r?\n ", ""
    $linea = ($texto -split "\r?\n") | Where-Object { $_ -like "Class-Path:*" } | Select-Object -First 1
    if ($linea) {
        $faltan = ($linea.Substring(11).Trim() -split "\s+") | Where-Object { $_ -and -not (Test-Path (Join-Path $jarDir $_)) }
        if ($faltan) { throw "Faltan librerias junto a sigelab.jar: $($faltan -join ', '). Vuelve a crear el artefacto con 'copy to the output directory and link via manifest'." }
    }
}

# ---------------------------------------------------------------- 1. Limpieza
Paso "Limpiando build anterior"
# Una copia anterior de SIGELAB o del servicio abierta bloquea sus archivos: se cierra primero.
$abiertos = Get-Process SIGELAB, facial-service -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -and $_.Path.StartsWith($build, [System.StringComparison]::OrdinalIgnoreCase) }
if ($abiertos) {
    Write-Host "Cerrando SIGELAB/servicio facial que estaban abiertos desde build\..."
    $abiertos | Stop-Process -Force
    Start-Sleep -Seconds 2
}
if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Path $entrada, $dist | Out-Null

# ---------------------------------------------------------------- 2. Servicio facial -> facial-service.exe
Paso "Compilando el servicio facial con PyInstaller (tarda varios minutos)"
& $python -m pip install --quiet --upgrade pyinstaller
if ($LASTEXITCODE -ne 0) { throw "No se pudo instalar PyInstaller." }

$argsPy = @(
    "-m", "PyInstaller", "run_service.py",
    "--name", "facial-service", "--onedir", "--noconfirm", "--clean",
    "--paths", ".",
    # Codigo propio: la API y los procesos hijos de camara (rostro y QR) se cargan por nombre
    "--collect-submodules", "app",
    "--collect-submodules", "scripts",
    "--hidden-import", "app.main",
    "--hidden-import", "scripts.start",
    "--hidden-import", "scripts.interactive_face",
    "--hidden-import", "scripts.qr_scan",
    "--hidden-import", "scripts.find_user",
    # Librerias con modulos dinamicos o archivos de datos
    "--collect-submodules", "uvicorn",
    "--collect-all", "insightface",
    "--collect-all", "onnxruntime",
    "--hidden-import", "pyodbc",
    "--exclude-module", "pytest",
    "--distpath", (Join-Path $build "py-dist"),
    "--workpath", (Join-Path $build "py-work"),
    "--specpath", (Join-Path $build "py-spec")
)

Push-Location $facial
try {
    & $python @argsPy
    if ($LASTEXITCODE -ne 0) { throw "PyInstaller fallo. Revisa los mensajes de arriba." }
} finally { Pop-Location }

# ---------------------------------------------------------------- 3. Carpeta de entrada para jpackage
Paso "Reuniendo archivos"
Copy-Item (Join-Path $jarDir "*.jar") $entrada
$facialDestino = Join-Path $entrada "facial-service"
Copy-Item (Join-Path $build "py-dist\facial-service") $facialDestino -Recurse
Copy-Item $envFile (Join-Path $facialDestino ".env")

if (-not $SinModelo) {
    $modelo = Leer-Env "FACE_MODEL" "buffalo_l"
    $origenModelo = Join-Path $env:USERPROFILE ".insightface\models\$modelo"
    if (Test-Path $origenModelo) {
        $destinoModelo = Join-Path $facialDestino "insightface\models\$modelo"
        New-Item -ItemType Directory -Path (Split-Path $destinoModelo) -Force | Out-Null
        Copy-Item $origenModelo $destinoModelo -Recurse
        Write-Host "Modelo facial '$modelo' incluido."
    } else {
        Write-Warning "No se encontro el modelo en $origenModelo. Cada PC lo descargara (requiere internet) la primera vez."
    }
}

# ---------------------------------------------------------------- 4. SIGELAB.exe con jpackage
Paso "Generando SIGELAB.exe con jpackage"
$argsJp = @(
    "--type", "app-image",
    "--name", "SIGELAB",
    "--app-version", $Version,
    "--input", $entrada,
    "--main-jar", "sigelab.jar",
    "--main-class", "umg.sigelab.Main",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--dest", $dist
)
if ($Consola) { $argsJp += "--win-console" }
if ($Icono)   { $argsJp += @("--icon", (Join-Path $raiz $Icono)) }

& $jpackage @argsJp
if ($LASTEXITCODE -ne 0) { throw "jpackage fallo." }

# ---------------------------------------------------------------- 5. Instalador (opcional)
if ($Instalador) {
    Paso "Generando instalador (requiere WiX Toolset 3.14 en el PATH)"
    & $jpackage --type exe --name SIGELAB --app-version $Version `
        --app-image (Join-Path $dist "SIGELAB") --dest $dist `
        --win-shortcut --win-menu --win-per-user-install --win-dir-chooser
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear el instalador. Verifica que WiX 3.14 este instalado." }
}

Paso "Listo"
Write-Host "Ejecutable: $(Join-Path $dist 'SIGELAB\SIGELAB.exe')" -ForegroundColor Green
if ($Instalador) { Write-Host "Instalador: $dist" -ForegroundColor Green }
Write-Host "Prueba rapida del servicio facial: $(Join-Path $dist 'SIGELAB\app\facial-service\facial-service.exe')"
