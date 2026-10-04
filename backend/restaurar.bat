@echo off
setlocal
echo ================================================
echo   SIG-CERRAJERIA - Restaurar un respaldo
echo ================================================
echo.
echo Respaldos disponibles (el mas reciente al final):
echo.
dir /b /o:n backups\sig_cerrajeria_*.sql.gz
echo.
set /p ARCHIVO=Escribe el nombre del archivo a restaurar: 
if "%ARCHIVO%"=="" exit /b 1
echo.
echo ATENCION: se reemplazaran TODOS los datos actuales por los de
echo %ARCHIVO%. Antes se guarda un respaldo del estado actual.
set /p CONFIRMA=Escribe SI para continuar: 
if /i not "%CONFIRMA%"=="SI" (
    echo Cancelado.
    pause
    exit /b 1
)
echo.
docker compose stop backend
docker compose exec backup sh /usr/local/bin/backup.sh restaurar %ARCHIVO%
set RESULTADO=%errorlevel%
docker compose start backend
echo.
if %RESULTADO% neq 0 (
    echo ERROR: no se pudo restaurar. Los datos actuales no se modificaron.
) else (
    echo Listo. Datos restaurados desde %ARCHIVO%.
)
echo.
pause
