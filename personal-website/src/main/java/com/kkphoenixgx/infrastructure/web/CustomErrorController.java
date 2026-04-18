package com.kkphoenixgx.infrastructure.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                Object errorUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
                String uri = (errorUri != null) ? errorUri.toString() : request.getRequestURI();

                logger.info("404 Error detected for URI: {}", uri);

                if (uri != null) {
                    String lowerUri = uri.toLowerCase();
                    if (lowerUri.startsWith("/rpg")) {
                        return "redirect:/RPG/404.html";
                    } else if (lowerUri.startsWith("/study")) {
                        return "redirect:/Study/404.html";
                    } else if (lowerUri.startsWith("/programing")) {
                        return "redirect:/Programing/404.html";
                    }
                }
            }
        }
        return "error";
    }
}