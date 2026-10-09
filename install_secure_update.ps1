$ErrorActionPreference = 'Stop'
$installRoot = 'C:\Inplabel'
if ((Resolve-Path -LiteralPath $installRoot).Path -ne 'C:\Inplabel') { throw 'Directorio de instalación inesperado.' }
$package = Join-Path $PSScriptRoot 'target/deployment/INPLABEL_SISTEMA.jar'
$installedJar = Join-Path $installRoot 'INPLABEL_SISTEMA.jar'
if (-not (Test-Path -LiteralPath $package)) { throw 'Falta el paquete preparado y verificado.' }

# Migrate the existing connection secret without displaying it or embedding it in the new package.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($installedJar)
try {
    $entry = $zip.GetEntry('BOOT-INF/classes/application.properties')
    $reader = [IO.StreamReader]::new($entry.Open())
    $properties = $reader.ReadToEnd()
    $reader.Dispose()
    $passwordSetting = [regex]::Match($properties, '(?m)^spring\.datasource\.password=(.*)$').Groups[1].Value.Trim()
    $nested = [regex]::Match($passwordSetting, '^\$\{SPRING_DATASOURCE_PASSWORD:\$\{DB_PASSWORD:(.*)\}\}$')
    if ($nested.Success) { $plainSecret = $nested.Groups[1].Value }
    elseif ($passwordSetting -and -not $passwordSetting.StartsWith('${')) { $plainSecret = $passwordSetting }
    elseif (Test-Path -LiteralPath (Join-Path $installRoot 'db-password.dpapi')) { $plainSecret = $null }
    else { throw 'No se encontró una configuración de conexión que se pueda migrar automáticamente.' }
} finally { $zip.Dispose(); $properties = $null; $passwordSetting = $null; $nested = $null }

$backup = Join-Path $installRoot ('backup-security-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backup | Out-Null
foreach ($file in @('INPLABEL_SISTEMA.jar','INICIAR_SISTEMA.bat','INICIAR_INPLABEL_SILENCIOSO.vbs','launch_secure.ps1','db-password.dpapi')) {
    $source = Join-Path $installRoot $file
    if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination $backup }
}
if ($plainSecret) {
    $plainSecret | ConvertTo-SecureString -AsPlainText -Force | ConvertFrom-SecureString | Set-Content -LiteralPath (Join-Path $installRoot 'db-password.dpapi') -Encoding ASCII
    $plainSecret = $null
}
$processes = @(Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($installedJar) })
foreach ($process in $processes) { Stop-Process -Id $process.ProcessId -Force }
Copy-Item -LiteralPath $package -Destination $installedJar -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'launch_secure.ps1') -Destination (Join-Path $installRoot 'launch_secure.ps1') -Force
@'
@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0launch_secure.ps1"
if errorlevel 1 pause
'@ | Set-Content -LiteralPath (Join-Path $installRoot 'INICIAR_SISTEMA.bat') -Encoding ASCII
@'
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
shell.Run "powershell -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File """ & root & "\launch_secure.ps1""", 0, False
'@ | Set-Content -LiteralPath (Join-Path $installRoot 'INICIAR_INPLABEL_SILENCIOSO.vbs') -Encoding ASCII
& (Join-Path $installRoot 'launch_secure.ps1') -NoBrowser
Write-Output ('Respaldo: ' + $backup)
