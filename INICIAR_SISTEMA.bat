@echo off
chcp 65001 >nul
title SISTEMA DE PEDIDOS INPLABEL S.A.C. - LANZADOR AUTOMATICO
color 0A

echo ===============================================================================
echo                INPLABEL S.A.C. - SISTEMA WEB DE PEDIDOS Y GESTION
echo ===============================================================================
echo [1/4] Comprobando e iniciando el servicio de MySQL...

:: 1. Intentar iniciar servicio de MySQL en Windows
net start MySQL80 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       [OK] Servicio MySQL80 activo.
) else (
    net start MySQL >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo       [OK] Servicio MySQL activo.
    ) else (
        echo       [INFO] El servicio MySQL ya esta corriendo o manejado por XAMPP.
    )
)

:: 2. Localizar ejecutable de MySQL para verificar base de datos
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) else if exist "C:\xampp\mysql\bin\mysql.exe" (
    set "MYSQL_EXE=C:\xampp\mysql\bin\mysql.exe"
) else (
    where mysql >nul 2>&1
    if %ERRORLEVEL% EQU 0 set "MYSQL_EXE=mysql"
)

echo [2/4] Verificando Base de Datos 'inplabel'...
if defined MYSQL_EXE (
    "%MYSQL_EXE%" -u root -padmin123 -e "CREATE DATABASE IF NOT EXISTS inplabel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo       [OK] Base de datos 'inplabel' verificada/creada con exito.
        
        if exist "%~dp0inplabel_schema_completo.sql" (
            "%MYSQL_EXE%" -u root -padmin123 inplabel -e "SELECT count(*) FROM usuarios;" >nul 2>&1
            if %ERRORLEVEL% NEQ 0 (
                echo       [INFO] Importando estructura inicial y datos corporativos...
                "%MYSQL_EXE%" -u root -padmin123 inplabel < "%~dp0inplabel_schema_completo.sql" >nul 2>&1
                echo       [OK] Datos y tablas importados exitosamente.
            )
        )
    ) else (
        echo       [AVISO] No se pudo conectar directamente con clave admin123. Spring Boot intentara inicializar la BD.
    )
)

echo [3/4] Iniciando Servidor Backend (Spring Boot en Puerto 8080)...
cd /d "%~dp0"
start "Inplabel Backend Spring Boot" /min powershell -ExecutionPolicy Bypass -File ".\run_backend.ps1"

echo [4/4] Iniciando Servidor Frontend y abriendo Sistema en Navegador...
cd /d "%~dp0..\SistemaWebPedidosFront"
start "Inplabel Web Server" /min powershell -ExecutionPolicy Bypass -Command "try { npx --yes serve -l 3000 . } catch { python -m http.server 3000 } "

:: Esperar 3 segundos para que los servicios esten activos
timeout /t 3 /nobreak >nul

:: Abrir el navegador en el Login del Sistema
start http://127.0.0.1:3000

echo ===============================================================================
echo    ¡SISTEMA INPLABEL INICIADO CORRECTAMENTE!
echo    - Frontend Web: http://127.0.0.1:3000
echo    - Backend API:  http://localhost:8080/api/status
echo ===============================================================================
pause
