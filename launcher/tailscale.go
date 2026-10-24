package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// publicarParaCelular deja el sistema disponible por HTTPS en la red privada
// de Tailscale (lo mismo que celular.bat) y devuelve la direccion para el
// celular, ej. https://mi-pc.tail1234.ts.net. Tailscale recuerda la
// configuracion: si ya estaba publicado, no hace nada.
func publicarParaCelular() (string, error) {
	ts := buscarTailscale()
	if ts == "" {
		return "", errors.New("Tailscale no esta instalado en esta PC: sin el, el celular no puede entrar.\n" +
			"  Instalalo de https://tailscale.com/download/windows, inicia sesion y vuelve a abrir este programa")
	}
	estado, err := ejecutar(ts, 20*time.Second, "status", "--json")
	if err != nil {
		return "", fmt.Errorf("no se pudo consultar Tailscale: %v", err)
	}
	dominio, conectado := leerEstadoTailscale(estado)
	if !conectado || dominio == "" {
		return "", errors.New("Tailscale esta instalado pero no conectado.\n" +
			"  Abrelo desde el icono junto al reloj, inicia sesion y vuelve a abrir este programa")
	}
	url := "https://" + dominio

	if actual, err := ejecutar(ts, 20*time.Second, "serve", "status", "--json"); err == nil && servePublicado(actual) {
		return url, nil
	}
	paso("Publicando el sistema para el celular en Tailscale...")
	// La primera vez Tailscale puede pedir activar HTTPS en la cuenta: devuelve un enlace
	if salidaServe, err := ejecutar(ts, 90*time.Second, "serve", "--bg", puertoApp); err != nil {
		return "", fmt.Errorf("Tailscale no pudo publicar el sistema. Si abajo aparece un enlace, abrelo,\n"+
			"  activa HTTPS en tu cuenta de Tailscale y vuelve a abrir este programa:\n  %s", salidaServe)
	}
	return url, nil
}

func buscarTailscale() string {
	if ruta, err := exec.LookPath("tailscale"); err == nil {
		return ruta
	}
	ruta := filepath.Join(os.Getenv("ProgramFiles"), "Tailscale", "tailscale.exe")
	if existe(ruta) {
		return ruta
	}
	return ""
}

// leerEstadoTailscale saca de "tailscale status --json" el nombre de esta PC
// en la red privada (sin el punto final) y si esta conectada.
func leerEstadoTailscale(jsonEstado string) (dominio string, conectado bool) {
	var estado struct {
		BackendState string
		Self         struct{ DNSName string }
	}
	if json.Unmarshal([]byte(jsonEstado), &estado) != nil {
		return "", false
	}
	return strings.TrimSuffix(estado.Self.DNSName, "."), estado.BackendState == "Running"
}

// servePublicado: si "tailscale serve status --json" ya reenvia al puerto del sistema.
func servePublicado(jsonServe string) bool {
	return strings.Contains(jsonServe, "127.0.0.1:"+puertoApp) || strings.Contains(jsonServe, "localhost:"+puertoApp)
}

func ejecutar(programa string, limite time.Duration, args ...string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), limite)
	defer cancel()
	cmd := exec.CommandContext(ctx, programa, args...)
	ocultarVentana(cmd)
	out, err := cmd.CombinedOutput()
	return strings.TrimSpace(string(out)), err
}
