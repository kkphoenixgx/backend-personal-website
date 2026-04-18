package com.kkphoenixgx.infrastructure.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CleanUrlRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Intercepta apenas as requisições externas (do navegador).
        // Ignora os forwards internos para que o Spring sirva os arquivos reais livremente.
        if (DispatcherType.REQUEST.equals(req.getDispatcherType())) {
            String uri = req.getRequestURI();
            String cleanUrl = uri;
            boolean shouldRedirect = false;

            if (cleanUrl.endsWith(".html")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 5);
                shouldRedirect = true;
            }

            if (cleanUrl.equals("/index")) {
                cleanUrl = "/";
                shouldRedirect = true;
            } else if (cleanUrl.endsWith("/index")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 5);
                shouldRedirect = true;
            }

            if (shouldRedirect && !cleanUrl.equals(uri)) {
                res.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
                res.setHeader(HttpHeaders.LOCATION, cleanUrl.isEmpty() ? "/" : cleanUrl);
                return; // Interrompe a cadeia e responde o 301
            }
        }
        chain.doFilter(request, response);
    }
}