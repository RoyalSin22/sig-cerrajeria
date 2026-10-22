package com.cerrajeria.sig.negocio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Datos del negocio: una sola fila (id = 1), creada por V12. */
@Entity
@Table(name = "negocio")
@Getter
@Setter
@NoArgsConstructor
public class Negocio {

    @Id
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
