package com.cerrajeria.sig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Expone el directorio de subida de imagenes de productos como recurso
 * estatico en /uploads/**. Almacenamiento local (ver nota en application.yml).
 *
 * Las respuestas paginadas se serializan como {content, page: {number, size,
 * totalElements, totalPages}} (VIA_DTO), el formato estable que espera
 * frontend/src/types/pagina.ts. Sin esto, Page se serializa tal cual y
 * res.page queda undefined en todas las pantallas paginadas.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    @Value("${sig.uploads-dir}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String rutaAbsoluta = new File(uploadsDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + rutaAbsoluta + File.separator);
    }
}
