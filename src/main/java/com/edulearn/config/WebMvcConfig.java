package com.edulearn.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        // Serve runtime-uploaded files from filesystem so they survive without a rebuild
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
        // Avatar uploads
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:./uploads/avatars/");
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addErrorPages(
            new ErrorPage(HttpStatus.NOT_FOUND, "/login.html")
        );
    }
}
