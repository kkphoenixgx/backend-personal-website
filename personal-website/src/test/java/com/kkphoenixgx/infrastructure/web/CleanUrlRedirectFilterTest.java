package com.kkphoenixgx.infrastructure.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CleanUrlRedirectFilterTest {

    private CleanUrlRedirectFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CleanUrlRedirectFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        request.setDispatcherType(DispatcherType.REQUEST);
    }

    @Test
    void doFilter_whenEndsWithHtml_shouldRedirectToCleanUrl() throws Exception {
        request.setRequestURI("/RPG/personagens.html");
        
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.MOVED_PERMANENTLY.value(), response.getStatus());
        assertEquals("/RPG/personagens", response.getHeader(HttpHeaders.LOCATION));
        verifyNoInteractions(filterChain); // Garante que interrompeu o fluxo
    }

    @Test
    void doFilter_whenIsRootIndex_shouldRedirectToRoot() throws Exception {
        request.setRequestURI("/index");
        
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.MOVED_PERMANENTLY.value(), response.getStatus());
        assertEquals("/", response.getHeader(HttpHeaders.LOCATION));
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilter_whenEndsWithDirectoryIndex_shouldRedirectToDirectory() throws Exception {
        request.setRequestURI("/Study/index");
        
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.MOVED_PERMANENTLY.value(), response.getStatus());
        assertEquals("/Study/", response.getHeader(HttpHeaders.LOCATION));
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilter_whenCleanUrlRequested_shouldContinueChainWithoutRedirect() throws Exception {
        request.setRequestURI("/Study/conceitos");
        
        filter.doFilter(request, response, filterChain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(filterChain).doFilter(request, response);
    }
}