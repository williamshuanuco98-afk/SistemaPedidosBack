param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$installRoot = $PSScriptRoot
$jarPath = Join-Path $installRoot 'INPLABEL_SISTEMA.jar'
$running = @(Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($jarPath) })
if ($running.Count -eq 0) {
    $secretFile = Join-Path $installRoot 'db-password.dpapi'
    if (-not (Test-Path -LiteralPath $secretFile)) { throw 'Falta configurar la conexión protegida de la base de datos.' }
    $secret = (Get-Content -LiteralPath $secretFile -Raw).Trim() | ConvertTo-SecureString
    $previousPassword = $env:DB_PASSWORD
    try {
        $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
        $java = Join-Path $installRoot 'runtime/bin/java.exe'
        Start-Process -FilePath $java -ArgumentList @('-jar', ('"' + $jarPath + '"'), '--app.open-browser=false') -WorkingDirectory $installRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $installRoot 'server-output.log') -RedirectStandardError (Join-Path $installRoot 'server-error.log') | Out-Null
    } finally { $env:DB_PASSWORD = $previousPassword; $secret = $null }
}
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/index.html' -TimeoutSec 2
        if ($response.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Seconds 1 }
}
if (-not $ready) { throw 'El servidor no inició. Revise server-error.log y server-output.log.' }
if (-not $NoBrowser) { Start-Process 'http://localhost:8080' -WindowStyle Hidden }
Write-Output 'Sistema disponible en http://localhost:8080'
