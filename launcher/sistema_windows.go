//go:build windows

package main

import (
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
