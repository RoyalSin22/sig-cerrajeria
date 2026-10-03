package com.cerrajeria.sig.domicilio.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ServicioDomicilioResponse(
    Long id,
    String cliente,
    String direccion,
    String telefono,
    LocalDate fecha,
    LocalTime hora,
    String tipoServicio,
    String observaciones,
    String estado
) {
}
