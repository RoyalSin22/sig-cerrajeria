package com.cerrajeria.sig.ordenes;

import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * saldo_pendiente es columna GENERATED en la BD (Fase 6); se calcula aqui en
 * Java via getSaldoPendiente() para no depender de un refresh tras cada save.
 */
@Entity
@Table(name = "ordenes_trabajo")
@Getter
@Setter
@NoArgsConstructor
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_orden", unique = true, length = 20)
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "producto_recibido", nullable = false, length = 150)
    private String productoRecibido;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problema;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.RECIBIDO;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    @Column(name = "fecha_prometida")
    private LocalDateTime fechaPrometida;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    /** Reparacion sin cobro sobre una orden ya entregada (V11): precio 0, no mueve caja. */
    @Column(name = "es_garantia", nullable = false)
    private boolean garantia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_origen_id")
    private OrdenTrabajo ordenOrigen;

    public BigDecimal getSaldoPendiente() {
        return precioTotal.subtract(montoPagado);
    }

    public void aplicarPago(BigDecimal monto) {
        this.montoPagado = this.montoPagado.add(monto);
    }
}
