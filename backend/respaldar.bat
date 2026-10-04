@echo off
echo Haciendo un respaldo de la base de datos...
docker compose exec backup sh /usr/local/bin/backup.sh ahora
if %errorlevel% neq 0 (
    echo.
    echo ERROR: no se pudo respaldar. Verifica que el sistema este iniciado.
) else (
    echo.
    echo Listo. El respaldo quedo en la carpeta "backups".
)
echo.
pause
