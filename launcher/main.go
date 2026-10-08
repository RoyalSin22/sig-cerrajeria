// Lanzador de SIG-Cerrajeria para Windows (SIG-Cerrajeria.exe).
//
// Hace lo mismo que backend/iniciar.bat, pero sin que el usuario tenga que
// saber nada de Docker: abre Docker Desktop si esta cerrado y espera a que
// este listo, crea backend/.env la primera vez, levanta los servicios y
// abre el navegador recien cuando el backend responde. Si algo falla, dice
// que paso y que hacer, en vez de cerrar la ventana.
//
// Se compila desde Linux: GOOS=windows GOARCH=amd64 go build (ver README.md).
package main

import (
	"bufio"
	"fmt"
	"os"
	"time"
)

const (
	urlApp   = "http://localhost:8081"
	urlSalud = "http://localhost:8080/actuator/health"
)

// Tiempos de espera. La primera vez Docker construye las imagenes del
// backend y del frontend, lo que puede tardar varios minutos.
var (
	esperaDocker  = 3 * time.Minute
	esperaBackend = 10 * time.Minute
	esperaFront   = 2 * time.Minute
)

func main() {
	prepararConsola()
	fmt.Println("================================================")
	fmt.Println("  SIG-CERRAJERIA")
	fmt.Println("================================================")
	fmt.Println()

	if err := iniciar(); err != nil {
		fmt.Println()
		fmt.Println("ERROR:", err)
		fmt.Println()
		fmt.Println("Presiona Enter para cerrar esta ventana.")
		bufio.NewReader(os.Stdin).ReadString('\n')
		os.Exit(1)
	}

	fmt.Println()
	fmt.Println("Listo. El sistema esta abierto en tu navegador:", urlApp)
	fmt.Println("Para apagarlo al final del dia, usa detener.bat (hace el respaldo de cierre).")
	fmt.Println("Esta ventana se cierra sola.")
	time.Sleep(8 * time.Second)
}

func iniciar() error {
	exe, err := os.Executable()
	if err != nil {
		return fmt.Errorf("no se pudo ubicar el programa: %w", err)
	}

	paso("Buscando la carpeta del sistema...")
	dir, err := buscarBackend(exe)
	if err != nil {
		return err
	}

	creado, err := asegurarEnv(dir)
	if err != nil {
		return err
	}
	if creado {
		paso("Primera vez: se creo la configuracion (backend\\.env) con una clave de seguridad nueva.")
	}

	paso("Verificando Docker...")
	if err := asegurarDocker(); err != nil {
		return err
	}

	paso("Iniciando los servicios (la primera vez puede tardar varios minutos)...")
	if err := composeUp(dir); err != nil {
		return err
	}

	paso("Esperando a que el sistema este listo...")
	if err := esperarHTTP(urlSalud, esperaBackend); err != nil {
		return fmt.Errorf("el sistema no respondio a tiempo (%v).\n"+
			"Revisa los mensajes de arriba. Si el problema sigue, ejecuta en la carpeta backend:\n"+
			"  docker compose logs backend", err)
	}
	if err := esperarHTTP(urlApp, esperaFront); err != nil {
		return fmt.Errorf("la pantalla del sistema no respondio a tiempo (%v)", err)
	}

	paso("Abriendo el navegador...")
	return abrirNavegador(urlApp)
}

func paso(msg string) {
	fmt.Println("> " + msg)
}
