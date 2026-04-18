package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.in.PagesServicePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SeoController {

    private final PagesServicePort pagesServicePort;
    private final String baseUrl;

    public SeoController(PagesServicePort pagesServicePort,
                         @Value("${app.base.url:https://api-personalwebsite.kkphoenix.com.br}") String baseUrl) {
        this.pagesServicePort = pagesServicePort;
        // Remove a barra final da URL base, caso exista, para evitar barras duplas
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        List<Pages> rootPages = pagesServicePort.getPages();
        List<String> allPaths = new ArrayList<>();
        allPaths.add("/"); // Sempre adiciona a home raiz

        for (Pages page : rootPages) {
            extractCleanPaths(page, allPaths);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String path : allPaths) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append(escapeXml(path)).append("</loc>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getRobotsTxt() {
        return "User-agent: *\n" +
               "Allow: /\n" +
               "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    private void extractCleanPaths(Pages page, List<String> allPaths) {
        if (page.getPath() != null) {
            String cleanPath = page.getPath().replace("/index.html", "/").replace(".html", "");
            if (!allPaths.contains(cleanPath)) {
                allPaths.add(cleanPath);
            }
        }
        if (page.getItems() != null) {
            for (Pages child : page.getItems()) {
                extractCleanPaths(child, allPaths);
            }
        }
    }

    private String escapeXml(String path) {
        return path.replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;").replace(">", "&gt;").replace("<", "&lt;");
    }
}