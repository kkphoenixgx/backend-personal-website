package com.kkphoenixgx.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebConfigTest {

    @Test
    void addCorsMappings_whenTestCorsEnabled_shouldAllowAll() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "isTestCorsEnabled", true);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        
        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedOrigins("*")).thenReturn(registration);
        when(registration.allowedMethods("*")).thenReturn(registration);

        webConfig.addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedOrigins("*");
        verify(registration).allowedMethods("*");
    }

    @Test
    void addCorsMappings_whenTestCorsDisabled_shouldRestrictOrigins() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "isTestCorsEnabled", false);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        
        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.allowCredentials(anyBoolean())).thenReturn(registration);

        webConfig.addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedOrigins("https://www.kkphoenix.com.br", "https://api-personalwebsite.kkphoenix.com.br");
    }

    @Test
    void addResourceHandlers_shouldPrefixWithFileAndConfigureCache() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "staticLocationUri", "./static-sites/");

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/**")).thenReturn(registration);
        when(registration.addResourceLocations(any(String[].class))).thenReturn(registration);
        when(registration.setCachePeriod(anyInt())).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry).addResourceHandler("/**");
        verify(registration).addResourceLocations("file:./static-sites/", "classpath:/static/", "classpath:/templates/");
        verify(registration).setCachePeriod(0);
    }
}