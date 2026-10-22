//go:build windows

package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
)

// prepararConsola pone la consola en UTF-8, por si algun mensaje de Docker
// trae caracteres no ASCII (la consola de Windows usa la pagina 850).
func prepararConsola() {
	syscall.NewLazyDLL("kernel32.dll").NewProc("SetConsoleOutputCP").Call(65001)
}

func abrirDockerDesktop() error {
	ruta := filepath.Join(os.Getenv("ProgramFiles"), "Docker", "Docker", "Docker Desktop.exe")
	return exec.Command(ruta).Start()
}

func abrirNavegador(url string) error {
	return exec.Command("rundll32", "url.dll,FileProtocolHandler", url).Start()
}

// ocultarVentana evita que cada "docker info" del sondeo abra una consola.
func ocultarVentana(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true}
}

// registrarInicioAutomatico deja en la carpeta Inicio de Windows un archivo
// que abre el lanzador minimizado ("--inicio") al iniciar sesion: el sistema
// arranca solo al prender la PC. No necesita permisos de administrador.
// Devuelve true si lo creo ahora (la primera vez).
func registrarInicioAutomatico(exe string) (bool, error) {
	carpeta := filepath.Join(os.Getenv("APPDATA"), "Microsoft", "Windows", "Start Menu", "Programs", "Startup")
	return escribirInicio(filepath.Join(carpeta, "SIG-Cerrajeria.cmd"), exe)
}

// evitarSuspension: si la PC se suspende, el celular deja de poder entrar.
// Solo cambia el tiempo "enchufada" (no el de bateria) y no apaga la pantalla.
func evitarSuspension() error {
	for _, args := range [][]string{
		{"/change", "standby-timeout-ac", "0"},
		{"/change", "hibernate-timeout-ac", "0"},
	} {
		cmd := exec.Command("powercfg", args...)
		ocultarVentana(cmd)
		if out, err := cmd.CombinedOutput(); err != nil {
			return fmt.Errorf("%v: %s", err, out)
		}
	}
	return nil
}
