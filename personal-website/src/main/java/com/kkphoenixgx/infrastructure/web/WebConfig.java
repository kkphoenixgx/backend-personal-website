package com.kkphoenixgx.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// import java.net.URI;
// import java.nio.file.Paths;

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
            // Location for the cloned git repository content
            String resourceLocation = staticLocationUri.endsWith("/") ? staticLocationUri : staticLocationUri + "/";
            
            // Ensure the path has the file: prefix if it is an absolute system path
            if (!resourceLocation.startsWith("file:") && !resourceLocation.startsWith("classpath:")) {
                resourceLocation = "file:" + resourceLocation;
            }

            // Default location for static assets bundled with the application
            String classpathLocation = "classpath:/static/";
            String templatesLocation = "classpath:/templates/";

            // Explicit handlers for specific directories to ensure correct resolution
            registry.addResourceHandler("/Study/**")
                    .addResourceLocations(resourceLocation + "Study/")
                    .setCachePeriod(0);

            registry.addResourceHandler("/Programing/**")
                    .addResourceLocations(resourceLocation + "Programing/")
                    .setCachePeriod(0);

            registry.addResourceHandler("/RPG/**")
                    .addResourceLocations(resourceLocation + "RPG/")
                    .setCachePeriod(0);

            registry.addResourceHandler("/**")
                    .addResourceLocations(resourceLocation, classpathLocation, templatesLocation)
                    .setCachePeriod(0); // Disable caching for development
        
            logger.info("Configured static resource handlers for '/**' pointing to: {}, {} and {}", 
                resourceLocation, classpathLocation, templatesLocation);
        } 
        catch (Exception e) {
            logger.error("Error configuring static resource handler for {}: {}", staticLocationUri, e.getMessage(), e);
        }

    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (isTestCorsEnabled) {
            registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("*");
        }
        else{
            registry.addMapping("/**")
              .allowedOrigins(
                "https://www.kkphoenix.com.br"
              )
              .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT")
              .allowedHeaders("*")
              .allowCredentials(true);
        }

    }
}