package main

import (
	"bytes"
	"fmt"
	"os"
)

// contenidoInicio: el .cmd de la carpeta Inicio. "start /min" abre el
// lanzador minimizado, para que no aparezca una ventana al prender la PC.
func contenidoInicio(exe string) string {
	return "@echo off\r\n" +
		"rem Arranca SIG-Cerrajeria al iniciar sesion (lo creo SIG-Cerrajeria.exe).\r\n" +
		"rem Para que no arranque solo, borra este archivo.\r\n" +
		fmt.Sprintf("start \"SIG-Cerrajeria\" /min \"%s\" --inicio\r\n", exe)
}

// escribirInicio crea o corrige el .cmd (por ejemplo, si se movio la carpeta).
// Devuelve true solo si no existia.
func escribirInicio(ruta, exe string) (bool, error) {
	nuevo := []byte(contenidoInicio(exe))
	actual, err := os.ReadFile(ruta)
	if err == nil && bytes.Equal(actual, nuevo) {
		return false, nil
	}
	if err := os.WriteFile(ruta, nuevo, 0o644); err != nil {
		return false, fmt.Errorf("no se pudo crear %s: %w", ruta, err)
	}
	return err != nil, nil
}
