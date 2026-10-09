@echo off
chcp 65001 >nul
title Sistema de Pedidos INPLABEL
cd /d "%~dp0"
echo Iniciando el sistema en http://localhost:8080
echo El frontend y la API usan el mismo servidor para proteger la sesion.
echo Si no configuro DB_PASSWORD, se solicitara sin mostrarla en pantalla.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run_backend.ps1"
if errorlevel 1 pause
