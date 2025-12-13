package com.kkphoenixgx.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// import java.net.URI;
// import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${app.static.pages}")
    private String staticLocationUri;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        try {
            // Location for the cloned git repository content
            String resourceLocation = staticLocationUri.endsWith("/") ? staticLocationUri : staticLocationUri + "/";
            // Default location for static assets bundled with the application
            String classpathLocation = "classpath:/static/";
            String templatesLocation = "classpath:/templates/";

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
}