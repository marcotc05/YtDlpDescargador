@echo off
setlocal enabledelayedexpansion
color 0b
title Actualizador YtDlpDescargador v2.0

echo =======================================================
echo   INICIANDO ACTUALIZACION SEGURA A V2.0
echo =======================================================
echo.

REM 1. Verificar que estamos en la carpeta correcta
if not exist ".git" (
    echo [ERROR] No se encontro la carpeta .git.
    echo Asegurate de ejecutar este archivo DENTRO de la carpeta raiz del proyecto.
    pause
    exit /b
)

if not exist "settings.gradle.kts" (
    echo [ERROR] No se encontro settings.gradle.kts.
    echo Asegurate de estar en la carpeta correcta.
    pause
    exit /b
)

echo [OK] Estructura de carpetas verificada correctamente.
echo.

REM 2. Aplicar el parche si el archivo existe
if exist "cambios-v2.patch" (
    echo [INFO] Se encontro cambios-v2.patch. Intentando aplicar...
    git apply --ignore-whitespace cambios-v2.patch
    if !errorlevel! neq 0 (
        echo [ADVERTENCIA] El parche fallo o ya estaba aplicado. 
        echo Continuando con los archivos actuales...
    ) else (
        echo [OK] Parche aplicado correctamente.
    )
) else (
    echo [INFO] No se encontro cambios-v2.patch. Asumiendo que ya reemplazaste los archivos.
)
echo.

REM 3. Proteger el ZIP de backup y limpiar basura
findstr /C:"backup-ytdlp-codigo.zip" .gitignore >nul
if !errorlevel! neq 0 (
    echo backup-ytdlp-codigo.zip>> .gitignore
)
git rm --cached backup-ytdlp-codigo.zip >nul 2>&1

REM 4. Agregar todos los cambios
echo [INFO] Preparando archivos para GitHub...
git add -A

REM 5. Hacer el commit
echo [INFO] Creando commit con la nueva version...
git commit -m "feat: actualizacion completa v2.0 (QuickJS, UI, Logs, R8)"

REM 6. Subir a GitHub
echo [INFO] Subiendo a GitHub (origin main)...
git push origin main

if !errorlevel! equ 0 (
    echo.
    echo =======================================================
    echo   [EXITO] Cambios subidos a GitHub correctamente.
    echo   Ve a la pestana "Actions" en GitHub para ver la compilacion.
    echo =======================================================
) else (
    echo.
    echo =======================================================
    echo   [ERROR] Hubo un problema al subir los cambios.
    echo   Revisa si tienes conexion o si Git te pide credenciales.
    echo =======================================================
)

pause
