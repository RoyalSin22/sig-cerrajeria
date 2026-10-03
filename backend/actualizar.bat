@echo off
echo Reconstruyendo SIG-Cerrajeria con los ultimos cambios...
docker compose up -d --build
echo.
echo Listo.
pause
