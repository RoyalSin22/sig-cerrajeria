package com.cerrajeria.sig.common.audit;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.math.RoundingMode;

/**
 * Que paso, ademas de quien y cuando: una descripcion legible, el motivo si lo
 * hubo, y cada campo que cambio con su valor anterior y nuevo. Se guarda como
 * JSON en audit_log.detalle. Nunca lleva contrasenas ni tokens.
 */
public final class DetalleAuditoria {

    private final String descripcion;
    private String motivo;
    private final Map<String, Cambio> cambios = new LinkedHashMap<>();

    public record Cambio(String antes, String despues) {
    }

    private DetalleAuditoria(String descripcion) {
        this.descripcion = descripcion;
    }

    public static DetalleAuditoria de(String descripcion) {
        return new DetalleAuditoria(descripcion);
    }

    public DetalleAuditoria motivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    /** Agrega el cambio solo si el valor es distinto (los montos se comparan por valor: 2.5 = 2.50). */
    public DetalleAuditoria cambio(String campo, Object antes, Object despues) {
        if (!iguales(antes, despues)) {
            cambios.put(campo, new Cambio(texto(antes), texto(despues)));
        }
        return this;
    }

    public boolean sinCambios() {
        return cambios.isEmpty();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getMotivo() {
        return motivo;
    }

    public Map<String, Cambio> getCambios() {
        return cambios;
    }

    private static boolean iguales(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }

    private static String texto(Object valor) {
        if (valor == null) return null;
        if (valor instanceof BigDecimal monto) return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return valor.toString();
    }
}
