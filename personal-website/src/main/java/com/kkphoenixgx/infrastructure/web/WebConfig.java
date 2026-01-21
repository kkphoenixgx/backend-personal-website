package com.kkphoenixgx.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${app.static.pages}")
    private String staticLocationUri;

    @Value("${app.test.cors:false}")
    private boolean isTestCorsEnabled;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            String resourceLocation = staticLocationUri.endsWith("/") ? staticLocationUri : staticLocationUri + "/";
            
            if (!resourceLocation.startsWith("file:") && !resourceLocation.startsWith("classpath:")) {
                resourceLocation = "file:" + resourceLocation;
            }

            registry.addResourceHandler("/**")
                    .addResourceLocations(resourceLocation, "classpath:/static/", "classpath:/templates/")
                    .setCachePeriod(0);
        
            logger.info("Configured static resource handlers for '/**' pointing to: {}", resourceLocation);
        } 
        catch (Exception e) {
            logger.error("Error configuring static resource handler for {}: {}", staticLocationUri, e.getMessage(), e);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (isTestCorsEnabled) {
            logger.info("Test cors is enabled");

            registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("*");
        }
        else {
            registry.addMapping("/**")
              .allowedOrigins(
                "https://www.kkphoenix.com.br",
                "https://api-personalwebsite.kkphoenix.com.br"
              )
              .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
              .allowedHeaders("*")
              .allowCredentials(true);
        }
    }
}