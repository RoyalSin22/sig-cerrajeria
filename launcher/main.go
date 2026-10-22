// Lanzador de SIG-Cerrajeria para Windows (SIG-Cerrajeria.exe).
//
// Es lo unico que hay que hacer en la PC que sirve el sistema:
//
//   - Doble clic (la primera vez y cuando se quiera abrir el sistema): crea
//     backend/.env, abre Docker Desktop si esta cerrado, levanta los
//     servicios, deja todo configurado para que arranque solo al prender la
//     PC (y que la PC no se suspenda), publica el sistema para el celular con
//     Tailscale y muestra una pagina con el codigo QR para abrirlo en el
//     celular.
//   - "--inicio" (lo ejecuta Windows al iniciar sesion, minimizado): lo mismo
//     sin preguntar ni abrir nada, y ademas se actualiza si hay cambios
//     nuevos en GitHub (respaldando antes). Lo que pasa queda en
//     backend/lanzador.log.
//   - "--actualizar": la actualizacion, a pedido (actualizar.bat).
//
// Si algo falla en modo manual, dice que paso y que hacer, en vez de cerrar
// la ventana. Se compila desde Linux: GOOS=windows go build (ver README.md).
package main

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"time"
)

const (
	urlApp     = "http://localhost:8081"
	urlSalud   = "http://localhost:8080/actuator/health"
	puertoApp  = "8081"
	archivoLog = "lanzador.log"
)

// Tiempos de espera. La primera vez Docker construye las imagenes del
// backend y del frontend, lo que puede tardar varios minutos.
var (
	esperaDocker  = 5 * time.Minute
	esperaBackend = 10 * time.Minute
	esperaFront   = 2 * time.Minute
)

// salida es donde van los mensajes: la consola en modo manual, el archivo de
// log en el arranque automatico (ahi nadie mira la ventana).
var salida io.Writer = os.Stdout

type modo int

const (
	modoManual modo = iota
	modoInicio
	modoActualizar
)

func leerModo(args []string) modo {
	for _, a := range args {
		switch a {
		case "--inicio":
			return modoInicio
		case "--actualizar":
			return modoActualizar
		}
	}
	return modoManual
}

func main() {
	prepararConsola()
	m := leerModo(os.Args[1:])

	exe, err := os.Executable()
	if err != nil {
		fallar(m, fmt.Errorf("no se pudo ubicar el programa: %w", err))
	}
	dir, err := buscarBackend(exe)
	if err != nil {
		fallar(m, err)
	}

	if m == modoInicio {
		if log, err := abrirLog(filepath.Join(dir, archivoLog)); err == nil {
			defer log.Close()
			salida = log
		}
		fmt.Fprintf(salida, "\n===== %s · arranque automatico =====\n", time.Now().Format("2006-01-02 15:04:05"))
	} else {
		fmt.Fprintln(salida, "================================================")
		fmt.Fprintln(salida, "  SIG-CERRAJERIA")
		fmt.Fprintln(salida, "================================================")
		fmt.Fprintln(salida)
	}

	switch m {
	case modoInicio:
		err = arranqueAutomatico(dir)
	case modoActualizar:
		err = actualizarAPedido(dir)
	default:
		err = iniciarManual(exe, dir)
	}
	if err != nil {
		fallar(m, err)
	}

	if m == modoManual {
		fmt.Fprintln(salida)
		fmt.Fprintln(salida, "Listo. El sistema esta abierto en tu navegador:", urlApp)
		fmt.Fprintln(salida, "Desde ahora arranca solo cada vez que prendas la PC: no hace falta abrir este programa.")
		fmt.Fprintln(salida, "Esta ventana se cierra sola.")
		time.Sleep(10 * time.Second)
	}
}

// iniciarManual: el doble clic. Configura todo lo que falte y abre el sistema.
func iniciarManual(exe, dir string) error {
	if err := levantar(dir, false); err != nil {
		return err
	}

	primeraVez := false
	paso("Dejando el sistema listo para que arranque solo al prender la PC...")
	if creado, err := registrarInicioAutomatico(exe); err != nil {
		aviso("No se pudo activar el arranque automatico: %v", err)
	} else {
		primeraVez = creado
	}
	if err := evitarSuspension(); err != nil {
		aviso("No se pudo evitar que la PC se suspenda: %v. Hazlo en Configuracion > Sistema > Energia", err)
	}

	url, err := publicarParaCelular()
	if err != nil {
		aviso("%v", err)
	} else {
		paso("Para el celular: " + url)
	}
	pagina, err := escribirPaginaCelular(dir, url, err)
	if err != nil {
		aviso("No se pudo crear la pagina para el celular: %v", err)
	}

	paso("Abriendo el navegador...")
	if err := abrirNavegador(urlApp); err != nil {
		return err
	}
	// La primera vez (o si el celular todavia no esta listo) se muestra como conectarlo
	if pagina != "" && (primeraVez || url == "") {
		return abrirNavegador(pagina)
	}
	return nil
}

// arranqueAutomatico: al iniciar sesion en Windows. Nadie mira la ventana:
// todo queda en el log y no se abre el navegador.
func arranqueAutomatico(dir string) error {
	if err := levantar(dir, false); err != nil {
		return err
	}
	if leerEnv(dir, "AUTO_ACTUALIZAR") != "no" {
		if err := actualizar(dir); err != nil {
			// El sistema sigue funcionando con la version anterior
			aviso("No se actualizo: %v", err)
		}
	}
	if _, err := publicarParaCelular(); err != nil {
		aviso("%v", err)
	}
	paso("Listo.")
	return nil
}

func actualizarAPedido(dir string) error {
	if err := levantar(dir, false); err != nil {
		return err
	}
	return actualizar(dir)
}

// levantar: Docker listo, servicios arriba y backend respondiendo.
func levantar(dir string, construir bool) error {
	if creado, err := asegurarEnv(dir); err != nil {
		return err
	} else if creado {
		paso("Primera vez: se creo la configuracion (backend\\.env) con una clave de seguridad nueva.")
	}

	paso("Verificando Docker...")
	if err := asegurarDocker(); err != nil {
		return err
	}

	paso("Iniciando los servicios (la primera vez puede tardar varios minutos)...")
	if err := composeUp(dir, construir); err != nil {
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
	return nil
}

func fallar(m modo, err error) {
	fmt.Fprintln(salida)
	fmt.Fprintln(salida, "ERROR:", err)
	if m == modoInicio {
		os.Exit(1) // nadie va a presionar Enter: el detalle queda en el log
	}
	fmt.Fprintln(salida)
	fmt.Fprintln(salida, "Presiona Enter para cerrar esta ventana.")
	bufio.NewReader(os.Stdin).ReadString('\n')
	os.Exit(1)
}

func paso(msg string) {
	fmt.Fprintln(salida, "> "+msg)
}

func aviso(formato string, args ...any) {
	fmt.Fprintln(salida, "! "+fmt.Sprintf(formato, args...))
}

// abrirLog abre el log para agregar; si paso de 1 MB lo empieza de nuevo.
func abrirLog(ruta string) (*os.File, error) {
	if info, err := os.Stat(ruta); err == nil && info.Size() > 1<<20 {
		os.Remove(ruta)
	}
	return os.OpenFile(ruta, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
}
