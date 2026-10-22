package main

import (
	"encoding/base64"
	"fmt"
	"html"
	"os"
	"path/filepath"
	"strings"

	"rsc.io/qr"
)

const archivoCelular = "celular.html"

// escribirPaginaCelular deja en backend/celular.html los pasos para usar el
// sistema desde el celular, con un codigo QR de la direccion. Se puede abrir
// de nuevo cuando haga falta (doble clic en el archivo). Si Tailscale no esta
// listo, la pagina explica que falta en vez del QR.
func escribirPaginaCelular(dir, url string, problema error) (string, error) {
	ruta := filepath.Join(dir, archivoCelular)
	if err := os.WriteFile(ruta, []byte(paginaCelular(url, problema)), 0o644); err != nil {
		return "", err
	}
	return ruta, nil
}

func paginaCelular(url string, problema error) string {
	var cuerpo string
	if url == "" {
		detalle := "Tailscale no esta listo en esta PC."
		if problema != nil {
			detalle = problema.Error()
		}
		cuerpo = `<div class="alerta"><h2>Falta un paso en esta PC</h2><p>` +
			strings.ReplaceAll(html.EscapeString(detalle), "\n", "<br>") +
			`</p><p>Cuando esté listo, vuelve a abrir <b>SIG-Cerrajeria.exe</b>: esta página mostrará el código para el celular.</p></div>`
	} else {
		img := ""
		if codigo, err := qr.Encode(url, qr.M); err == nil {
			img = `<img alt="Código QR de la dirección" src="data:image/png;base64,` +
				base64.StdEncoding.EncodeToString(codigo.PNG()) + `">`
		}
		cuerpo = fmt.Sprintf(`<div class="qr">%s<p class="url">%s</p></div>
<ol>
<li><b>Instala Tailscale</b> en el celular (Play Store) y entra con la <b>misma cuenta</b> que en esta PC.</li>
<li>En los <b>Ajustes</b> del celular busca <b>"VPN"</b>, entra a <b>Tailscale</b> y activa <b>"VPN siempre activa"</b>. Así queda conectado solo, sin tener que abrir Tailscale.</li>
<li>Abre la <b>cámara</b> y apunta a este código (o escribe la dirección en Chrome).</li>
<li>En Chrome toca <b>&#8942;</b> (tres puntos) &gt; <b>"Instalar aplicación"</b>. Desde ahí, solo tocas el ícono de la llave.</li>
</ol>
<p class="nota">Esto se hace una sola vez por celular. Esta PC debe estar prendida mientras el local atiende.</p>`,
			img, html.EscapeString(url))
	}
	return `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>SIG-Cerrajería en el celular</title>
<style>
body{font-family:system-ui,sans-serif;font-size:20px;line-height:1.5;max-width:44rem;margin:2rem auto;padding:0 1rem;color:#14171a;background:#f3f5f7}
h1{font-size:2rem;margin:0 0 1rem}
.qr{background:#fff;border:2px solid #dde2e8;border-radius:16px;padding:1.5rem;text-align:center}
.qr img{width:320px;height:320px;image-rendering:pixelated}
.url{font-family:monospace;font-size:1.3rem;font-weight:bold;word-break:break-all;color:#0a3d73}
li{margin:.6rem 0}
.nota{color:#4f565d}
.alerta{background:#ffe7c7;border:2px solid #9a4a00;border-radius:16px;padding:1rem 1.5rem}
</style></head><body>
<h1>Usar el sistema en el celular</h1>
` + cuerpo + `
</body></html>
`
}
