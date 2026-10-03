package com.cerrajeria.sig.caja;

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

    public BigDecimal saldoEsperado() {
        return montoApertura.add(montoIngresos).subtract(montoEgresos);
    }

    public void sumarIngreso(BigDecimal monto) {
        this.montoIngresos = this.montoIngresos.add(monto);
    }

    public void sumarEgreso(BigDecimal monto) {
        this.montoEgresos = this.montoEgresos.add(monto);
    }
}
