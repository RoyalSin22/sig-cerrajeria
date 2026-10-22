package main

import (
	"crypto/rand"
	"encoding/base64"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

// buscarBackend devuelve la carpeta que contiene docker-compose.yml. El .exe
// puede estar en la raiz del proyecto (sig-cerrajeria\SIG-Cerrajeria.exe) o
// dentro de backend\. Si se ejecuta desde un acceso directo, os.Executable
// devuelve la ruta real del .exe, no la del acceso directo.
func buscarBackend(rutaExe string) (string, error) {
	base := filepath.Dir(rutaExe)
	for _, dir := range []string{base, filepath.Join(base, "backend")} {
		if existe(filepath.Join(dir, "docker-compose.yml")) {
			return dir, nil
		}
	}
	return "", fmt.Errorf("no se encontro el sistema junto a este programa (%s).\n"+
		"Copia SIG-Cerrajeria.exe dentro de la carpeta sig-cerrajeria (la que tiene la carpeta backend)\n"+
		"y crea el acceso directo del escritorio desde ahi", base)
}

// asegurarEnv crea backend/.env a partir de .env.example si no existe, con un
// JWT_SECRET aleatorio. No toca un .env existente. DB_PASSWORD se deja como
// viene: Postgres fija la contrasena solo al crear la base, asi que cambiarla
// aqui dejaria al backend sin poder conectarse a una base ya creada.
func asegurarEnv(dir string) (creado bool, err error) {
	env := filepath.Join(dir, ".env")
	if existe(env) {
		return false, nil
	}
	ejemplo, err := os.ReadFile(filepath.Join(dir, ".env.example"))
	if err != nil {
		return false, fmt.Errorf("falta backend\\.env y no se pudo leer .env.example: %w", err)
	}

	secreto := make([]byte, 48)
	if _, err := rand.Read(secreto); err != nil {
		return false, fmt.Errorf("no se pudo generar la clave de seguridad: %w", err)
	}

	lineas := strings.Split(strings.ReplaceAll(string(ejemplo), "\r\n", "\n"), "\n")
	reemplazado := false
	for i, l := range lineas {
		if strings.HasPrefix(l, "JWT_SECRET=") {
			lineas[i] = "JWT_SECRET=" + base64.StdEncoding.EncodeToString(secreto)
			reemplazado = true
		}
	}
	if !reemplazado {
		return false, errors.New(".env.example no tiene la linea JWT_SECRET=")
	}

	if err := os.WriteFile(env, []byte(strings.Join(lineas, "\n")), 0o600); err != nil {
		return false, fmt.Errorf("no se pudo crear backend\\.env: %w", err)
	}
	return true, nil
}

// leerEnv devuelve el valor de una variable de backend/.env ("" si no esta).
func leerEnv(dir, clave string) string {
	datos, err := os.ReadFile(filepath.Join(dir, ".env"))
	if err != nil {
		return ""
	}
	for _, l := range strings.Split(strings.ReplaceAll(string(datos), "\r\n", "\n"), "\n") {
		l = strings.TrimSpace(l)
		if valor, ok := strings.CutPrefix(l, clave+"="); ok {
			return strings.Trim(strings.TrimSpace(valor), `"'`)
		}
	}
	return ""
}

func existe(ruta string) bool {
	_, err := os.Stat(ruta)
	return err == nil
}
