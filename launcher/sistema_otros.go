//go:build !windows

package main

import (
	"errors"
	"os/exec"
)

// Fuera de Windows (desarrollo y CI) el lanzador funciona igual, salvo que no
// puede abrir Docker Desktop por si solo.

func prepararConsola() {}

func abrirDockerDesktop() error {
	return errors.New("abrir Docker automaticamente solo esta disponible en Windows")
}

func abrirNavegador(url string) error {
	return exec.Command("xdg-open", url).Start()
}

func ocultarVentana(*exec.Cmd) {}
