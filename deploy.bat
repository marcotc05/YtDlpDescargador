@echo off
setlocal enabledelayedexpansion
title DownAll Deploy & Push to GitHub

echo ========================================================
echo         DownAll - Git Synchronizer and Push
echo ========================================================

cd /d "%~dp0"

for /d %%D in (*) do (
    if exist "%%D\app\build.gradle.kts" (
        echo [INFO] Aplanando subcarpeta anidada: %%D
        xcopy /E /Y /H /R "%%D\*" . >nul
        rd /S /Q "%%D"
    )
)

if not exist ".git" (
    echo [ERROR CRITICO] No se detecto la carpeta .git en este directorio.
    pause
    exit /b 1
)

git remote -v | findstr "origin" >nul 2>&1
if %errorlevel% neq 0 (
    echo Configurando remote origin...
    git remote add origin https://github.com/marcotc05/YtDlpDescargador.git
)

echo [Sincronizando y Forzando Rama Main...]
git checkout -B main
git add -A
git commit -m "feat: complete rewrite baseline - DownAll native engine, glassmorphic UI and CI/CD"
git push -u origin main --force

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo   PUSH REALIZADO CON EXITO A GITHUB ORIGIN/MAIN
    echo ========================================================
) else (
    echo.
    echo [ERROR] No se pudo completar el push. Revisa tus credenciales de GitHub.
)
pause