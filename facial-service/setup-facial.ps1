param()

$ErrorActionPreference = "Stop"
$serviceRoot = $PSScriptRoot
$venvPython = Join-Path $serviceRoot ".venv\Scripts\python.exe"

$pyExe = $null
$pyArgs = @()

# 1. Buscar primero en la ruta de usuario de Python 3.12
$localPy312 = Join-Path $env:LOCALAPPDATA "Programs\Python\Python312\python.exe"
if (Test-Path $localPy312) {
    $pyExe = $localPy312
}

# 2. Si no, verificar lanzador py con 3.12
if (-not $pyExe) {
    try {
        & py -3.12 -c "import sys; print(sys.version)" 2>$null
        if ($LASTEXITCODE -eq 0) {
            $pyExe = "py"
            $pyArgs = @("-3.12")
        }
    } catch {}
}

# 3. Si no, verificar python en PATH
if (-not $pyExe) {
    try {
        $ver = & python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')" 2>$null
        if ($ver -eq "3.12" -or $ver -eq "3.11") {
            $pyExe = "python"
        }
    } catch {}
}

if (-not $pyExe) {
    Write-Error "No se encontró Python 3.12 ni 3.11 instalado. Descárguelo e instálelo desde python.org."
}

Push-Location $serviceRoot
try {
    if (-not (Test-Path $venvPython)) {
        Write-Host "Creando entorno virtual .venv usando: $pyExe $pyArgs"
        & $pyExe @pyArgs -m venv .venv
        if ($LASTEXITCODE -ne 0) { throw "No se pudo crear .venv." }
    }
    Write-Host "Actualizando pip..."
    & $venvPython -m pip install --upgrade pip
    if ($LASTEXITCODE -ne 0) { throw "No se pudo actualizar pip." }

    Write-Host "Instalando librerías requeridas (esto puede demorar unos minutos)..."
    & $venvPython -m pip install -r requirements.txt
    if ($LASTEXITCODE -ne 0) { throw "No se pudieron instalar requirements.txt." }

    & $venvPython -m pip show opencv-python-headless *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Ajustando OpenCV..."
        & $venvPython -m pip uninstall -y opencv-python-headless
        if ($LASTEXITCODE -ne 0) { throw "No se pudo retirar opencv-python-headless." }
        & $venvPython -m pip install --force-reinstall --no-deps opencv-python==4.10.0.84
        if ($LASTEXITCODE -ne 0) { throw "No se pudo restaurar OpenCV con GUI." }
    }

    if (-not (Test-Path .env)) {
        Copy-Item .env.example .env
        Write-Warning "Se creó .env desde .env.example. Configure DB_PASSWORD y FACIAL_ENCRYPTION_KEY antes de usar el servicio."
    }

    Write-Host ""
    Write-Host "Diagnóstico inicial (scripts.doctor):"
    & $venvPython -m scripts.doctor
    if ($LASTEXITCODE -ne 0) { Write-Warning "El diagnóstico requiere pasos adicionales; revise la salida anterior." }
}
finally {
    Pop-Location
}
