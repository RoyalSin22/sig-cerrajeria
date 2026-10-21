package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sesion de caja diaria (Fase 1, regla de negocio 1): ninguna operacion
 * monetaria puede procesarse sin una caja ABIERTA para el usuario en sesion.
 */
@Entity
@Table(name = "cajas_diarias")
@Getter
@Setter
@NoArgsConstructor
public class CajaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_apertura", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoApertura;

    @Column(name = "monto_cierre", precision = 12, scale = 2)
    private BigDecimal montoCierre;

    @Column(name = "monto_ingresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoIngresos = BigDecimal.ZERO;

    @Column(name = "monto_egresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEgresos = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    // Neto por metodo digital (cobros - anulaciones): dinero que NO esta en el cajon.
    @Column(name = "monto_yape", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoYape = BigDecimal.ZERO;

    @Column(name = "monto_plin", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPlin = BigDecimal.ZERO;

    @Column(name = "monto_tarjeta", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTarjeta = BigDecimal.ZERO;

    /** Contado - esperado al cerrar (V11). Negativo: falto dinero. */
    @Column(name = "diferencia_cierre", precision = 12, scale = 2)
    private BigDecimal diferenciaCierre;

    /** Explicacion de quien cerro, obligatoria si la caja no cuadro (V11). */
    @Column(name = "observacion_cierre", columnDefinition = "TEXT")
    private String observacionCierre;

    /** Total de la caja, sumando todos los metodos de pago. */
    public BigDecimal saldoEsperado() {
        return montoApertura.add(montoIngresos).subtract(montoEgresos);
    }

    /** Lo que deberia haber fisicamente en el cajon: contra esto se cuadra el cierre. */
    public BigDecimal efectivoEsperado() {
        return saldoEsperado().subtract(montoYape).subtract(montoPlin).subtract(montoTarjeta);
    }

    /** Ingreso en efectivo (movimientos manuales). */
    public void sumarIngreso(BigDecimal monto) {
        sumarIngreso(monto, MetodoPago.EFECTIVO);
    }

    public void sumarIngreso(BigDecimal monto, MetodoPago metodo) {
        acumularDigital(metodo, monto); // primero: rechaza MIXTO sin tocar los totales
        this.montoIngresos = this.montoIngresos.add(monto);
    }

    /** Egreso en efectivo (movimientos manuales, compras pagadas de caja). */
    public void sumarEgreso(BigDecimal monto) {
        sumarEgreso(monto, MetodoPago.EFECTIVO);
    }

    /** Un egreso por un metodo digital es la devolucion de un cobro de ese metodo (anulacion). */
    public void sumarEgreso(BigDecimal monto, MetodoPago metodo) {
        acumularDigital(metodo, monto.negate());
        this.montoEgresos = this.montoEgresos.add(monto);
    }

    private void acumularDigital(MetodoPago metodo, BigDecimal delta) {
        switch (metodo) {
            case EFECTIVO -> { }
            case YAPE -> montoYape = montoYape.add(delta);
            case PLIN -> montoPlin = montoPlin.add(delta);
            case TARJETA -> montoTarjeta = montoTarjeta.add(delta);
            case MIXTO -> throw new IllegalArgumentException(
                "Un pago MIXTO debe registrarse en caja linea por linea, segun su desglose");
        }
    }
}
