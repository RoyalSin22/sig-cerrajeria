#!/bin/sh
# Respaldo automatico de PostgreSQL (RNF-07). Corre dentro del servicio
# "backup" de docker-compose.yml, con las variables PG* apuntando a la base.
#
#   backup.sh                 modo servicio: respalda al arrancar (si el ultimo
#                             tiene mas de BACKUP_INTERVALO_HORAS), cada
#                             BACKUP_INTERVALO_HORAS y al detenerse (detener.bat)
#   backup.sh ahora           un respaldo inmediato (respaldar.bat)
#   backup.sh restaurar F     reemplaza la base con el respaldo F (restaurar.bat)
#
# No usa una hora fija (ej. 3 a. m.): la PC del mostrador suele apagarse de
# noche y ese respaldo nunca ocurriria. Respaldar al detener cubre el cierre
# del dia, que es justo cuando se cuadra la caja.

set -eu

DIR=/backups
INTERVALO_HORAS="${BACKUP_INTERVALO_HORAS:-6}"
DIAS="${BACKUP_DIAS:-30}"

log() { echo "[backup $(date '+%Y-%m-%d %H:%M:%S')] $*"; }

respaldar() {
  nombre="sig_cerrajeria_$(date '+%Y%m%d_%H%M%S').sql.gz"
  # Dos respaldos en el mismo segundo (ej. respaldar.bat y luego detener.bat)
  # tendrian el mismo nombre: se espera para no sobrescribir el anterior.
  while [ -e "$DIR/$nombre" ]; do
    sleep 1
    nombre="sig_cerrajeria_$(date '+%Y%m%d_%H%M%S').sql.gz"
  done
  tmp="$DIR/.$nombre.tmp"
  # --clean --if-exists: al restaurar, reemplaza todo de un solo paso.
  # Se escribe a un temporal y se verifica antes de renombrar, para que un
  # corte a medias nunca deje un archivo con nombre de respaldo valido.
  if pg_dump --clean --if-exists --no-owner | gzip -9 > "$tmp" && gzip -t "$tmp"; then
    mv "$tmp" "$DIR/$nombre"
    log "OK $nombre ($(du -h "$DIR/$nombre" | cut -f1))"
  else
    rm -f "$tmp"
    log "ERROR: fallo el respaldo"
    return 1
  fi
  # Retencion: borra respaldos con mas de DIAS dias.
  find "$DIR" -maxdepth 1 -name 'sig_cerrajeria_*.sql.gz' -mtime +"$DIAS" -print -delete \
    | sed 's#.*/#  eliminado por antiguedad: #'
}

ultimo_es_reciente() {
  [ -n "$(find "$DIR" -maxdepth 1 -name 'sig_cerrajeria_*.sql.gz' -mmin -$((INTERVALO_HORAS * 60)) | head -n 1)" ]
}

restaurar() {
  archivo="$DIR/$(basename "$1")"
  [ -f "$archivo" ] || { log "ERROR: no existe $archivo"; exit 1; }
  gzip -t "$archivo" || { log "ERROR: $archivo esta danado"; exit 1; }
  log "Respaldo de seguridad del estado actual antes de restaurar..."
  respaldar
  log "Restaurando $(basename "$archivo")..."
  # Una sola transaccion: si algo falla, Postgres revierte todo (hasta los
  # DROP/CREATE) y la base queda exactamente como estaba.
  gunzip -c "$archivo" | psql --quiet --single-transaction -v ON_ERROR_STOP=1 > /dev/null
  log "OK restaurado $(basename "$archivo")"
}

mkdir -p "$DIR"

case "${1:-servicio}" in
  ahora)
    respaldar
    ;;
  restaurar)
    [ $# -eq 2 ] || { echo "uso: backup.sh restaurar <archivo.sql.gz>"; exit 2; }
    restaurar "$2"
    ;;
  servicio)
    log "Servicio iniciado: cada ${INTERVALO_HORAS} h, conserva ${DIAS} dias, en ${DIR}"
    # Al detener (docker compose down / detener.bat) llega SIGTERM: se hace un
    # ultimo respaldo antes de salir. Compose detiene este servicio antes que
    # postgres porque depende de el.
    trap 'log "Deteniendo: respaldo de cierre"; respaldar || true; exit 0' TERM INT
    ultimo_es_reciente || respaldar || true
    while true; do
      # sleep en segundo plano + wait: asi el trap atiende SIGTERM al instante
      sleep $((INTERVALO_HORAS * 3600)) &
      wait $!
      respaldar || true
    done
    ;;
  *)
    echo "uso: backup.sh [ahora | restaurar <archivo.sql.gz>]"
    exit 2
    ;;
esac
