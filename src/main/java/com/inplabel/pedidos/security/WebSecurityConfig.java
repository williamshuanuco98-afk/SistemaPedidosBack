package com.inplabel.pedidos.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File frontDir = new File("../SistemaWebPedidosFront");
        String location = frontDir.toURI().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/index.html")
                .addResourceLocations(location, "classpath:/static/").setCachePeriod(0);
        for (String directory : new String[]{"js", "css", "img", "views"}) {
            registry.addResourceHandler("/" + directory + "/**")
                    .addResourceLocations(location + directory + "/", "classpath:/static/" + directory + "/")
                    .setCachePeriod(0);
        }
    }
}
