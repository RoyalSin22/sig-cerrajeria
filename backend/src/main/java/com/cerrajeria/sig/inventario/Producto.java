package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.common.audit.BaseAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto extends BaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 30)
    private TipoProducto tipoProducto;

    @Column(length = 100)
    private String marca;

    @Column(name = "cantidad_stock", nullable = false)
    private Integer cantidadStock = 0;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "llave_perfil", length = 50)
    private String llavePerfil;

    @Column(name = "llave_color", length = 30)
    private String llaveColor;

    @Column(name = "llave_diseno_tematica", length = 50)
    private String llaveDisenoTematica;

    @Column(name = "producto_modelo", length = 100)
    private String productoModelo;

    @Column(name = "producto_tamano", length = 30)
    private String productoTamano;

    @Column(name = "producto_material", length = 50)
    private String productoMaterial;

    @Column(nullable = false)
    private Boolean activo = true;

    /** Regla de negocio 2 (Fase 1): alerta cuando el stock alcanza el minimo. */
    public boolean tieneBajoStock() {
        return this.cantidadStock <= this.stockMinimo;
    }
}
