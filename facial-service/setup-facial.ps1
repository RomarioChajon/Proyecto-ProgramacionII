param()

$ErrorActionPreference = "Stop"
$serviceRoot = $PSScriptRoot
$venvPython = Join-Path $serviceRoot ".venv\Scripts\python.exe"

function Find-SupportedPython {
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
        "C:\Program Files\Python312\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe",
        "C:\Program Files\Python311\python.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    foreach ($ver in @("-3.12", "-3.11")) {
        try {
            $out = & py $ver -c "import sys; print(sys.executable)" 2>$null
            if ($LASTEXITCODE -eq 0 -and $out -and (Test-Path $out.Trim())) {
                return $out.Trim()
            }
        } catch {}
    }
    return $null
}

$basePython = Find-SupportedPython
if (-not $basePython) {
    Write-Error "No se encontró Python 3.12 ni 3.11. Instálelo desde python.org o con: winget install Python.Python.3.12"
}
Write-Host "Usando Python base: $basePython ($(& $basePython --version))"

Push-Location $serviceRoot
try {
    $wheelsDir = Join-Path $serviceRoot "wheels"
    New-Item -ItemType Directory -Path $wheelsDir -Force | Out-Null
    $scratchWheel = "C:\Users\stan\.gemini\antigravity\brain\476b4534-1e2f-41a3-82a9-a89633a52c4d\scratch\insightface-0.7.3-py3-none-any.whl"
    $localWheel = Join-Path $wheelsDir "insightface-0.7.3-py3-none-any.whl"
    if (Test-Path $scratchWheel) {
        Copy-Item $scratchWheel $localWheel -Force
    }

    if (Test-Path $venvPython) {
        $venvVer = & $venvPython -c "import sys; print(f'{sys.version_info[0]}.{sys.version_info[1]}')" 2>$null
        if ($venvVer -notin @("3.11", "3.12")) {
            Write-Host "Recreando .venv incompatible ($venvVer)..."
            Remove-Item -Recurse -Force (Join-Path $serviceRoot ".venv")
        }
    }
    if (-not (Test-Path $venvPython)) {
        & $basePython -m venv .venv
        if ($LASTEXITCODE -ne 0) { throw "No se pudo crear .venv." }
    }
    & $venvPython -m pip install --upgrade pip setuptools wheel
    if ($LASTEXITCODE -ne 0) { throw "No se pudo actualizar pip." }
    if (Test-Path $wheelsDir) {
        & $venvPython -m pip install --find-links $wheelsDir -r requirements.txt
    } else {
        & $venvPython -m pip install -r requirements.txt
    }
    if ($LASTEXITCODE -ne 0) { throw "No se pudieron instalar requirements.txt." }
    & $venvPython -m pip show opencv-python-headless *> $null
    if ($LASTEXITCODE -eq 0) {
        & $venvPython -m pip uninstall -y opencv-python-headless
        if ($LASTEXITCODE -ne 0) { throw "No se pudo retirar opencv-python-headless." }
        & $venvPython -m pip install --force-reinstall --no-deps opencv-python==4.10.0.84
        if ($LASTEXITCODE -ne 0) { throw "No se pudo restaurar OpenCV con GUI." }
    }
    if (-not (Test-Path .env)) {
        Copy-Item .env.example .env
        Write-Warning "Se creó .env desde .env.example. Configure DB_PASSWORD y FACIAL_ENCRYPTION_KEY antes de usar el servicio."
    }
    & $venvPython -m scripts.configure_from_java --generate-key
    Write-Host "Aplicando migración facial idempotente:"
    & $venvPython -m scripts.migrate
    if ($LASTEXITCODE -ne 0) { Write-Warning "No se pudo aplicar la migración ahora; verifique la conexión a SQL Server." }
    Write-Host "Diagnóstico inicial y preparación del modelo:"
    & $venvPython -m scripts.doctor --prepare-model
    if ($LASTEXITCODE -ne 0) { Write-Warning "El diagnóstico requiere pasos adicionales; revise la salida anterior." }
    Set-Content -Path (Join-Path $serviceRoot ".setup-ok") -Value "ok" -Encoding UTF8
    exit 0
}
finally {
    Pop-Location
}
