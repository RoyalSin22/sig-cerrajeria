@echo off
setlocal
rem Publica el sistema por HTTPS dentro de tu red privada de Tailscale, para
rem usarlo desde el celular (en el local o fuera). Se ejecuta UNA sola vez:
rem Tailscale recuerda la configuracion y la reactiva al reiniciar la PC.
rem Para desactivarlo: tailscale serve reset

echo ================================================
echo   SIG-CERRAJERIA - Acceso desde el celular
echo ================================================
echo.

set "TS=tailscale"
where tailscale >nul 2>&1
if errorlevel 1 set "TS=%ProgramFiles%\Tailscale\tailscale.exe"

"%TS%" version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Tailscale no esta instalado en esta PC.
    echo Descargalo de https://tailscale.com/download/windows, inicia sesion
    echo y vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

"%TS%" status >nul 2>&1
if errorlevel 1 (
    echo ERROR: Tailscale esta instalado pero no conectado.
    echo Abre Tailscale desde el icono junto al reloj, inicia sesion
    echo y vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

echo Publicando el sistema en tu red de Tailscale...
echo Si aparece un enlace para activar HTTPS, abrelo, aceptalo y espera aqui.
echo.
"%TS%" serve --bg 8081
if errorlevel 1 (
    echo.
    echo ERROR: no se pudo activar. Si arriba aparece un enlace, abrelo para
    echo activar HTTPS en tu cuenta de Tailscale y vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================
echo   Listo. En el celular, con Tailscale conectado, abre en Chrome
echo   la direccion https://... que aparece aqui abajo:
echo ================================================
"%TS%" serve status
echo.
pause
