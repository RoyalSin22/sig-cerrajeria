package com.cerrajeria.sig.analisis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que hay cargado: cuantas lineas, por cuanto y de que fechas (null si no hay nada). */
public record ResumenVentasAnteriores(long filas, BigDecimal total, LocalDate desde, LocalDate hasta) {
}
