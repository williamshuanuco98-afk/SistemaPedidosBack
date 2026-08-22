$sqlPath = ".\inplabel_schema_completo.sql"
$lines = Get-Content -Path $sqlPath -Encoding UTF8
Set-Content -Path ".\inplabel_schema_clean.sql" -Value $lines -Encoding String

$mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlExe)) {
    $mysqlExe = "mysql"
}

Write-Host "Importando esquema completo de tablas e indices a MySQL..." -ForegroundColor Yellow
Get-Content ".\inplabel_schema_clean.sql" | & $mysqlExe -u root -padmin123 inplabel --default-character-set=utf8mb4

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Base de datos e indices importados con exito." -ForegroundColor Green
} else {
    Write-Host "[ERROR] Error al importar base de datos." -ForegroundColor Red
}
