package com.cerrajeria.sig.common.audit;

import com.cerrajeria.sig.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** RF-47: registro inmutable de acciones sensibles del sistema. */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAccion accion;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    /** JSON con descripcion, motivo y cambios (ver DetalleAuditoria); null en registros antiguos. */
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    private String detalle;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();
}
