package com.kkphoenixgx.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** * This controller handles "clean URLs" by forwarding requests to their * corresponding .html files. * It requires 'spring.mvc.pathmatch.matching-strategy=ant_path_matcher' to be * set in application.properties. */
@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Value("${app.static.pages}")
    private Resource staticLocationResource;

    private final Map<String, Path> assetCache = new ConcurrentHashMap<>();

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

    /**
     * Resolve requisições de imagens e outros assets buscando-os em qualquer
     * subdiretório dentro do diretório raiz correspondente (ex: /Study/.../img.jpg -> /Study/img/img.jpg).
     */
    @GetMapping(value = {
        "/{root}/**/{filename:.+\\.(?:jpg|jpeg|png|gif|webp|svg|pdf)}"
    })
    @ResponseBody
    public ResponseEntity<Resource> serveAsset(
            @PathVariable String root, 
            @PathVariable String filename, 
            HttpServletRequest request) {
        
        try {
            Path staticPath = staticLocationResource.getFile().toPath().normalize();
            Path rootPath = staticPath.resolve(root).normalize();
            
            // Proteção contra Path Traversal: Garante que o caminho resolvido não escape do diretório base
            if (!rootPath.startsWith(staticPath)) {
                logger.warn("Tentativa de Path Traversal detectada para a raiz: {}", root);
                return ResponseEntity.notFound().build();
            }

            // Prevenção contra DoS: Consulta o cache em memória antes de varrer o disco
            String cacheKey = root + "/" + filename;
            Path cachedPath = assetCache.get(cacheKey);
            if (cachedPath != null && Files.exists(cachedPath)) {
                return buildAssetResponse(cachedPath, filename, request);
            }

            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                try (Stream<Path> stream = Files.walk(rootPath)) {
                    Optional<Path> found = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equals(filename))
                        .findFirst();
                        
                    if (found.isPresent()) {
                        Path resolvedPath = found.get();
                        assetCache.put(cacheKey, resolvedPath); // Salva no cache
                        return buildAssetResponse(resolvedPath, filename, request);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao servir o asset {}: {}", filename, e.getMessage());
        }
        
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<Resource> buildAssetResponse(Path assetPath, String filename, HttpServletRequest request) throws Exception {
        Resource resource = new UrlResource(assetPath.toUri());
        if (resource.exists() && resource.isReadable()) {
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }
}