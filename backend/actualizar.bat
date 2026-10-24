@echo off
setlocal
cd /d "%~dp0"
rem Instala la version nueva del sistema (ultimos cambios de main en GitHub).
rem El lanzador hace un respaldo antes y no pisa archivos cambiados a mano.
rem Al prender la PC esto ya pasa solo; este archivo es para hacerlo en el momento.
echo Buscando e instalando la version nueva de SIG-Cerrajeria...
echo.
if exist "..\SIG-Cerrajeria.exe" (
    "..\SIG-Cerrajeria.exe" --actualizar
) else if exist "SIG-Cerrajeria.exe" (
    "SIG-Cerrajeria.exe" --actualizar
) else (
    git -C .. pull --ff-only origin main && docker compose up -d --build
)
echo.
pause
