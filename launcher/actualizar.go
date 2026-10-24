package main

import (
	"context"
	"errors"
	"fmt"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

// actualizar trae los cambios nuevos de GitHub (rama main) y reconstruye el
// sistema. Antes hace un respaldo de la base. Si no hay cambios, no hace nada.
//
// No toca nada si: no hay git, la carpeta no es un clon, no esta en main, o
// alguien cambio archivos a mano (git pull los pisaria o fallaria). Si la
// reconstruccion falla, Docker deja corriendo la version anterior.
func actualizar(dir string) error {
	raiz := filepath.Dir(dir)
	if _, err := exec.LookPath("git"); err != nil {
		return errors.New("git no esta instalado: no se puede actualizar solo")
	}
	if rama, err := git(raiz, 15*time.Second, "rev-parse", "--abbrev-ref", "HEAD"); err != nil {
		return fmt.Errorf("la carpeta no es un clon de git (%v)", err)
	} else if rama != "main" {
		return fmt.Errorf("la carpeta esta en la rama %q, no en main: no se actualiza sola", rama)
	}

	paso("Buscando actualizaciones...")
	if _, err := git(raiz, 2*time.Minute, "fetch", "--quiet", "origin", "main"); err != nil {
		return fmt.Errorf("no se pudo consultar GitHub (¿hay internet?): %v", err)
	}
	pendientes, err := git(raiz, 15*time.Second, "rev-list", "--count", "HEAD..origin/main")
	if err != nil {
		return err
	}
	if n, _ := strconv.Atoi(pendientes); n == 0 {
		paso("El sistema ya esta al dia.")
		return nil
	}
	if cambios, err := git(raiz, 15*time.Second, "status", "--porcelain", "--untracked-files=no"); err != nil {
		return err
	} else if cambios != "" {
		return fmt.Errorf("hay archivos del sistema cambiados a mano en esta PC; no se actualiza para no perderlos:\n%s", cambios)
	}

	paso("Hay una version nueva. Haciendo un respaldo antes de actualizar...")
	if err := respaldar(dir); err != nil {
		return fmt.Errorf("no se actualizo porque no se pudo hacer el respaldo previo: %v", err)
	}

	paso("Descargando la version nueva...")
	if _, err := git(raiz, 5*time.Minute, "pull", "--ff-only", "--quiet", "origin", "main"); err != nil {
		return fmt.Errorf("no se pudo descargar la version nueva: %v", err)
	}

	paso("Instalando la version nueva (puede tardar unos minutos; el sistema sigue funcionando mientras tanto)...")
	if err := composeUp(dir, true); err != nil {
		return err
	}
	if err := esperarHTTP(urlSalud, esperaBackend); err != nil {
		return fmt.Errorf("la version nueva no respondio a tiempo (%v). Revisa: docker compose logs backend", err)
	}
	paso("Sistema actualizado.")
	return nil
}

func respaldar(dir string) error {
	cmd := exec.Command("docker", "compose", "exec", "-T", "backup", "sh", "/usr/local/bin/backup.sh", "ahora")
	cmd.Dir = dir
	cmd.Stdout = salida
	cmd.Stderr = salida
	ocultarVentana(cmd)
	return cmd.Run()
}

func git(raiz string, limite time.Duration, args ...string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), limite)
	defer cancel()
	cmd := exec.CommandContext(ctx, "git", append([]string{"-C", raiz}, args...)...)
	ocultarVentana(cmd)
	// Sin ventanas de usuario y contrasena: si falta la credencial, que falle y quede en el log
	cmd.Env = append(cmd.Environ(), "GIT_TERMINAL_PROMPT=0")
	out, err := cmd.CombinedOutput()
	texto := strings.TrimSpace(string(out))
	if err != nil {
		if texto != "" {
			return texto, fmt.Errorf("%v: %s", err, texto)
		}
		return texto, err
	}
	return texto, nil
}
