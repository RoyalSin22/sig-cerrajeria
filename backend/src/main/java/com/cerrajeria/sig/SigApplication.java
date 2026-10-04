package com.cerrajeria.sig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SigApplication {

    /**
     * La cerrajeria opera solo en Peru. Las fechas se guardan como
     * LocalDateTime (TIMESTAMP sin zona) a partir de LocalDateTime.now(), que
     * usa la zona del servidor: en un contenedor Docker (UTC) una venta de las
     * 20:00 quedaba registrada al dia siguiente y salia de los reportes del dia.
     */
    static final String ZONA_HORARIA_NEGOCIO = "America/Lima";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA_NEGOCIO));
        SpringApplication.run(SigApplication.class, args);
    }
}
