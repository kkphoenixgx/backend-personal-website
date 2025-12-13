package com.kkphoenixgx.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** * This controller handles "clean URLs" by forwarding requests to their * corresponding .html files. * It requires 'spring.mvc.pathmatch.matching-strategy=ant_path_matcher' to be * set in application.properties. */
@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    /** * Forwards requests for "clean URLs" (without .html extension) to the corresponding HTML file. * - Requests for directories (ending in "/") are forwarded to "index.html" inside that directory. * - Requests for files (not ending in "/") are forwarded to the path with ".html" appended. * This handler ignores paths that contain a dot ('.'), assuming they are direct requests for assets. */
    @GetMapping(value = {
        "/",
        "/**/{path:[^\\.]+}",
        "/**/{path:[^\\.]+}/"
    })
    public String forward(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        
        if (requestUri.endsWith("/")) {
            logger.info("Path is a directory. Forwarding from {} to {}index.html", requestUri, requestUri);
            return "forward:" + requestUri + "index.html";
        }

        logger.info("Path is a clean URL. Forwarding from {} to {}.html", requestUri, requestUri);
        return "forward:" + requestUri + ".html";
    }
}