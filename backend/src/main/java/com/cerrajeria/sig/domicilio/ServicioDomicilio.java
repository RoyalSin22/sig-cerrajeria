package com.cerrajeria.sig.domicilio;

import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "servicios_domicilio")
@Getter
@Setter
@NoArgsConstructor
public class ServicioDomicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "tipo_servicio", nullable = false, length = 100)
    private String tipoServicio;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoServicio estado = EstadoServicio.PROGRAMADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Cobro al marcar REALIZADO (V9). null mientras no se realiza; 0 = sin cobro. */
    @Column(name = "monto_cobrado", precision = 12, scale = 2)
    private java.math.BigDecimal montoCobrado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private com.cerrajeria.sig.common.MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_diaria_id")
    private com.cerrajeria.sig.caja.CajaDiaria cajaDiaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realizado_por")
    private Usuario realizadoPor;

    @Column(name = "fecha_realizado")
    private LocalDateTime fechaRealizado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
