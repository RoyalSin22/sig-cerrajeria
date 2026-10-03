@echo off
setlocal

echo ================================================
echo   SIG-CERRAJERIA - Iniciando el sistema...
echo ================================================
echo.

docker compose up -d

if %errorlevel% neq 0 (
    echo.
    echo ERROR: no se pudo iniciar.
    echo Verifica que Docker Desktop este abierto y vuelve a intentar.
    echo.
    pause
    exit /b 1
)

echo.
echo Esperando a que los servicios terminen de levantar...
timeout /t 10 /nobreak >nul

start http://localhost:8081

echo.
echo ================================================
echo   Listo. Deberia haberse abierto en tu navegador.
echo   Si no se abrio solo, entra a: http://localhost:8081
echo ================================================
echo.
pause
