package main

import (
	"errors"
	"fmt"
	"net/http"
	"os"
	"os/exec"
	"time"
)

func asegurarDocker() error {
	if _, err := exec.LookPath("docker"); err != nil {
		return errors.New("Docker Desktop no esta instalado en esta PC.\n" +
			"Instalalo desde https://www.docker.com/products/docker-desktop y vuelve a intentar")
	}
	if dockerListo() {
		return nil
	}

	paso("Docker esta cerrado: abriendo Docker Desktop...")
	if err := abrirDockerDesktop(); err != nil {
		return fmt.Errorf("no se pudo abrir Docker Desktop (%v). Abrelo a mano y vuelve a intentar", err)
	}
	limite := time.Now().Add(esperaDocker)
	for time.Now().Before(limite) {
		if dockerListo() {
			return nil
		}
		time.Sleep(3 * time.Second)
	}
	return errors.New("Docker Desktop no termino de arrancar a tiempo.\n" +
		"Espera a que el icono de la ballena deje de moverse y vuelve a abrir este programa")
}

// dockerListo: el CLI puede existir con el motor detenido; "docker info" solo
// responde cuando el motor esta listo para recibir comandos.
func dockerListo() bool {
	cmd := exec.Command("docker", "info")
	ocultarVentana(cmd)
	return cmd.Run() == nil
}

func composeUp(dir string) error {
	cmd := exec.Command("docker", "compose", "up", "-d")
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	if err := cmd.Run(); err != nil {
		return fmt.Errorf("no se pudieron iniciar los servicios (%v).\n"+
			"Si es la primera vez, revisa que haya conexion a internet: Docker descarga lo necesario", err)
	}
	return nil
}

// esperarHTTP espera a que la URL responda 2xx. Se usa para no abrir el
// navegador antes de tiempo (el usuario veria una pagina de error).
func esperarHTTP(url string, max time.Duration) error {
	cliente := &http.Client{Timeout: 5 * time.Second}
	limite := time.Now().Add(max)
	var ultimo error
	for time.Now().Before(limite) {
		resp, err := cliente.Get(url)
		if err == nil {
			resp.Body.Close()
			if resp.StatusCode >= 200 && resp.StatusCode < 300 {
				return nil
			}
			ultimo = fmt.Errorf("respondio %d", resp.StatusCode)
		} else {
			ultimo = err
		}
		time.Sleep(2 * time.Second)
	}
	if ultimo == nil {
		ultimo = errors.New("sin respuesta")
	}
	return fmt.Errorf("%s: %w", url, ultimo)
}
