package main

import (
	"net/http"
	"net/http/httptest"
	"os"
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
