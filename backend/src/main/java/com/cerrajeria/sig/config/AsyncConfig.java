package com.cerrajeria.sig.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita @Async: el webhook de WhatsApp responde 200 a Meta de inmediato
 * y procesa el mensaje (llamada a Claude + acciones del sistema) en otro
 * hilo, para no arriesgar un timeout del lado de Meta.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
