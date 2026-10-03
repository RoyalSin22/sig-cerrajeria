package com.cerrajeria.sig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Expone el directorio de subida de imagenes de productos como recurso
 * estatico en /uploads/**. Almacenamiento local (ver nota en application.yml).
 */
@Configuration
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
