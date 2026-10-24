package main

import (
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

const ejemploEnv = "# comentario\r\nDB_PASSWORD=change_me_local_dev\r\n" +
	"JWT_SECRET=cambia-este-secreto-en-produccion-minimo-32-caracteres\r\n" +
	"FRONTEND_ORIGIN=http://localhost:8081\r\n"

func crearProyecto(t *testing.T) (raiz, backend string) {
	t.Helper()
	raiz = t.TempDir()
	backend = filepath.Join(raiz, "backend")
	if err := os.MkdirAll(backend, 0o755); err != nil {
		t.Fatal(err)
	}
	escribir(t, filepath.Join(backend, "docker-compose.yml"), "services: {}\n")
	escribir(t, filepath.Join(backend, ".env.example"), ejemploEnv)
	return raiz, backend
}

func escribir(t *testing.T, ruta, contenido string) {
	t.Helper()
	if err := os.WriteFile(ruta, []byte(contenido), 0o644); err != nil {
		t.Fatal(err)
	}
}

func TestBuscarBackend_exeEnLaRaizDelProyecto(t *testing.T) {
	raiz, backend := crearProyecto(t)
	got, err := buscarBackend(filepath.Join(raiz, "SIG-Cerrajeria.exe"))
	if err != nil || got != backend {
		t.Fatalf("got %q, %v; want %q", got, err, backend)
	}
}

func TestBuscarBackend_exeDentroDeBackend(t *testing.T) {
	_, backend := crearProyecto(t)
	got, err := buscarBackend(filepath.Join(backend, "SIG-Cerrajeria.exe"))
	if err != nil || got != backend {
		t.Fatalf("got %q, %v; want %q", got, err, backend)
	}
}

func TestBuscarBackend_exeSueltoDaErrorClaro(t *testing.T) {
	_, err := buscarBackend(filepath.Join(t.TempDir(), "SIG-Cerrajeria.exe"))
	if err == nil || !strings.Contains(err.Error(), "Copia SIG-Cerrajeria.exe dentro de la carpeta sig-cerrajeria") {
		t.Fatalf("error esperado con la instruccion para el usuario, got %v", err)
	}
}

func TestAsegurarEnv_creaConSecretoAleatorioYConservaLoDemas(t *testing.T) {
	_, backend := crearProyecto(t)

	creado, err := asegurarEnv(backend)
	if err != nil || !creado {
		t.Fatalf("creado=%v err=%v", creado, err)
	}
	env, _ := os.ReadFile(filepath.Join(backend, ".env"))
	s := string(env)
	if strings.Contains(s, "cambia-este-secreto") {
		t.Fatal("el JWT_SECRET de ejemplo no se reemplazo")
	}
	var secreto string
	for _, l := range strings.Split(s, "\n") {
		if strings.HasPrefix(l, "JWT_SECRET=") {
			secreto = strings.TrimPrefix(l, "JWT_SECRET=")
		}
	}
	if len(secreto) < 32 {
		t.Fatalf("JWT_SECRET demasiado corto: %q", secreto)
	}
	// DB_PASSWORD se conserva: cambiarlo romperia una base ya creada
	if !strings.Contains(s, "DB_PASSWORD=change_me_local_dev") || !strings.Contains(s, "FRONTEND_ORIGIN=http://localhost:8081") {
		t.Fatalf("se perdieron otras variables:\n%s", s)
	}
	if strings.Contains(s, "\r") {
		t.Fatal("el .env generado no debe tener CRLF (docker compose lo leeria en el valor)")
	}

	// Cada instalacion recibe un secreto distinto
	_, otro := crearProyecto(t)
	asegurarEnv(otro)
	env2, _ := os.ReadFile(filepath.Join(otro, ".env"))
	if string(env2) == s {
		t.Fatal("dos instalaciones generaron el mismo JWT_SECRET")
	}
}

func TestAsegurarEnv_noTocaUnEnvExistente(t *testing.T) {
	_, backend := crearProyecto(t)
	escribir(t, filepath.Join(backend, ".env"), "JWT_SECRET=el-mio\n")

	creado, err := asegurarEnv(backend)
	if err != nil || creado {
		t.Fatalf("creado=%v err=%v", creado, err)
	}
	env, _ := os.ReadFile(filepath.Join(backend, ".env"))
	if string(env) != "JWT_SECRET=el-mio\n" {
		t.Fatalf(".env modificado: %q", env)
	}
}

func TestEsperarHTTP_esperaHastaQueResponda200(t *testing.T) {
	var llamadas atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		if llamadas.Add(1) < 2 {
			w.WriteHeader(http.StatusServiceUnavailable) // arrancando
			return
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	if err := esperarHTTP(srv.URL, 10*time.Second); err != nil {
		t.Fatal(err)
	}
	if llamadas.Load() < 2 {
		t.Fatalf("no reintento: %d llamadas", llamadas.Load())
	}
}

func TestEsperarHTTP_venceSiNuncaResponde(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusServiceUnavailable)
	}))
	defer srv.Close()

	err := esperarHTTP(srv.URL, 3*time.Second)
	if err == nil || !strings.Contains(err.Error(), "503") {
		t.Fatalf("se esperaba error con el ultimo estado (503), got %v", err)
	}
}

func TestLeerModo(t *testing.T) {
	if leerModo(nil) != modoManual || leerModo([]string{"--inicio"}) != modoInicio ||
		leerModo([]string{"--actualizar"}) != modoActualizar {
		t.Fatal("modos mal leidos")
	}
}

func TestLeerEnv(t *testing.T) {
	dir := t.TempDir()
	escribir(t, filepath.Join(dir, ".env"), "# AUTO_ACTUALIZAR=si\r\nAUTO_ACTUALIZAR= no \r\nOTRA=\"x\"\n")
	if got := leerEnv(dir, "AUTO_ACTUALIZAR"); got != "no" {
		t.Fatalf("got %q, want no (la linea comentada no cuenta)", got)
	}
	if got := leerEnv(dir, "OTRA"); got != "x" {
		t.Fatalf("got %q, want x (sin comillas)", got)
	}
	if got := leerEnv(dir, "FALTA"); got != "" {
		t.Fatalf("got %q", got)
	}
}

func TestLeerEstadoTailscale(t *testing.T) {
	dominio, ok := leerEstadoTailscale(`{"BackendState":"Running","Self":{"DNSName":"desktop-945a56o.tail89f1ed.ts.net."}}`)
	if !ok || dominio != "desktop-945a56o.tail89f1ed.ts.net" {
		t.Fatalf("got %q, %v", dominio, ok)
	}
	if _, ok := leerEstadoTailscale(`{"BackendState":"NeedsLogin","Self":{"DNSName":""}}`); ok {
		t.Fatal("sin sesion no esta conectado")
	}
	if _, ok := leerEstadoTailscale("no es json"); ok {
		t.Fatal("una salida rara no es estar conectado")
	}
}

func TestServePublicado(t *testing.T) {
	publicado := `{"TCP":{"443":{"HTTPS":true}},"Web":{"pc.ts.net:443":{"Handlers":{"/":{"Proxy":"http://127.0.0.1:8081"}}}}}`
	if !servePublicado(publicado) || servePublicado(`{}`) || servePublicado(`{"Proxy":"http://127.0.0.1:3000"}`) {
		t.Fatal("deteccion de serve incorrecta")
	}
}

func TestEscribirInicio_creaUnaVezYCorrigeSiSeMovioLaCarpeta(t *testing.T) {
	ruta := filepath.Join(t.TempDir(), "SIG-Cerrajeria.cmd")
	creado, err := escribirInicio(ruta, `C:\sig-cerrajeria\SIG-Cerrajeria.exe`)
	if err != nil || !creado {
		t.Fatalf("primera vez: creado=%v err=%v", creado, err)
	}
	datos, _ := os.ReadFile(ruta)
	if !strings.Contains(string(datos), `start "SIG-Cerrajeria" /min "C:\sig-cerrajeria\SIG-Cerrajeria.exe" --inicio`) {
		t.Fatalf("contenido inesperado:\n%s", datos)
	}
	if creado, _ := escribirInicio(ruta, `C:\sig-cerrajeria\SIG-Cerrajeria.exe`); creado {
		t.Fatal("la segunda vez no es nuevo")
	}
	escribirInicio(ruta, `D:\otra\SIG-Cerrajeria.exe`)
	datos, _ = os.ReadFile(ruta)
	if !strings.Contains(string(datos), `D:\otra\SIG-Cerrajeria.exe`) {
		t.Fatal("si se movio la carpeta, se corrige la ruta")
	}
}

func TestPaginaCelular_conQRYConPasos(t *testing.T) {
	p := paginaCelular("https://pc.tail1.ts.net", nil)
	for _, esperado := range []string{"https://pc.tail1.ts.net", "data:image/png;base64,", "VPN siempre activa", "Instalar aplicación"} {
		if !strings.Contains(p, esperado) {
			t.Fatalf("falta %q en la pagina", esperado)
		}
	}
	sinTailscale := paginaCelular("", errors.New("Tailscale no esta instalado <b>"))
	if !strings.Contains(sinTailscale, "Falta un paso") || strings.Contains(sinTailscale, "<b>\"") ||
		!strings.Contains(sinTailscale, "&lt;b&gt;") {
		t.Fatal("sin Tailscale explica que falta, con el texto escapado")
	}
}

// repoConRemoto arma un remoto "origin" con main y un clon, como la PC del local.
func repoConRemoto(t *testing.T) (remoto, clon string) {
	t.Helper()
	if _, err := exec.LookPath("git"); err != nil {
		t.Skip("git no disponible")
	}
	base := t.TempDir()
	remoto, clon = filepath.Join(base, "remoto.git"), filepath.Join(base, "clon")
	correr := func(dir string, args ...string) {
		t.Helper()
		cmd := exec.Command("git", args...)
		cmd.Dir = dir
		cmd.Env = append(os.Environ(), "GIT_AUTHOR_NAME=t", "GIT_AUTHOR_EMAIL=t@t", "GIT_COMMITTER_NAME=t", "GIT_COMMITTER_EMAIL=t@t")
		if out, err := cmd.CombinedOutput(); err != nil {
			t.Fatalf("git %v: %v\n%s", args, err, out)
		}
	}
	correr(base, "init", "--bare", "-b", "main", remoto)
	correr(base, "clone", "-q", remoto, clon)
	correr(clon, "checkout", "-q", "-b", "main")
	os.MkdirAll(filepath.Join(clon, "backend"), 0o755)
	escribir(t, filepath.Join(clon, "backend", "docker-compose.yml"), "services: {}\n")
	correr(clon, "add", ".")
	correr(clon, "commit", "-q", "-m", "inicial")
	correr(clon, "push", "-q", "origin", "main")
	return remoto, clon
}

func TestActualizar_alDiaNoHaceNada(t *testing.T) {
	_, clon := repoConRemoto(t)
	salida = io.Discard
	if err := actualizar(filepath.Join(clon, "backend")); err != nil {
		t.Fatalf("sin cambios no es error: %v", err)
	}
}

func TestActualizar_noPisaCambiosHechosAMano(t *testing.T) {
	remoto, clon := repoConRemoto(t)
	// Otro clon sube un cambio nuevo a main
	otro := filepath.Join(t.TempDir(), "otro")
	if out, err := exec.Command("git", "clone", "-q", remoto, otro).CombinedOutput(); err != nil {
		t.Fatalf("%v %s", err, out)
	}
	escribir(t, filepath.Join(otro, "nuevo.txt"), "x")
	for _, args := range [][]string{{"add", "."}, {"-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "nuevo"}, {"push", "-q", "origin", "main"}} {
		cmd := exec.Command("git", args...)
		cmd.Dir = otro
		if out, err := cmd.CombinedOutput(); err != nil {
			t.Fatalf("%v %s", err, out)
		}
	}
	// En la PC alguien edito un archivo del sistema
	escribir(t, filepath.Join(clon, "backend", "docker-compose.yml"), "services: {cambiado: {}}\n")

	salida = io.Discard
	err := actualizar(filepath.Join(clon, "backend"))
	if err == nil || !strings.Contains(err.Error(), "cambiados a mano") {
		t.Fatalf("debe negarse a actualizar, got %v", err)
	}
	if existe(filepath.Join(clon, "nuevo.txt")) {
		t.Fatal("no debio descargar nada")
	}
}
