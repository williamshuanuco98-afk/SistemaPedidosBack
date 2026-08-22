package com.inplabel.pedidos.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File frontDir = new File("../SistemaWebPedidosFront");
        if (frontDir.exists() && frontDir.isDirectory()) {
            String frontLocation = "file:" + frontDir.getAbsolutePath().replace('\\', '/') + "/";
            registry.addResourceHandler("/**")
                    .addResourceLocations(frontLocation, "classpath:/static/", "classpath:/public/")
                    .setCachePeriod(0);
        } else {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/", "classpath:/public/")
                    .setCachePeriod(0);
        }
    }
}
