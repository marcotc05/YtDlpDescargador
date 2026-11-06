@echo off
color 0A
echo =======================================================
echo   APLICANDO ACTUALIZACION V3.0 DIRECTAMENTE EN LA RAIZ
echo =======================================================
echo.
if not exist ".git" (
    echo [ERROR] No estas en la carpeta raiz YtDlpDescargador.
    pause
    exit /b
)
powershell -NoProfile -ExecutionPolicy Bypass -File "APLICAR_V3.ps1"
del "APLICAR_V3.ps1"
echo.
echo =======================================================
echo   PROCESO TERMINADO. Revisa GitHub Actions.
echo =======================================================
pause
