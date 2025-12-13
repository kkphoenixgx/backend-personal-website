package com.kkphoenixgx.infrastructure.persistence.IO;

import com.kkphoenixgx.domain.model.Pages;
import com.kkphoenixgx.domain.ports.out.PagesRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
// import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
// import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class IOPersistence implements PagesRepositoryPort {
  
  private static final Logger logger = LoggerFactory.getLogger(IOPersistence.class);

  private final Path staticLocationPath;

  public IOPersistence(@Value("${app.static.pages}") Resource staticLocationResource) {
    try {
      this.staticLocationPath = staticLocationResource.getFile().toPath().normalize();
      logger.info("Static content location resolved to: {}", this.staticLocationPath.toAbsolutePath());
    } catch (Exception e) {
      logger.error("Error resolving static location URI: {}", staticLocationResource, e);
      throw new IllegalStateException("Failed to initialize static content path", e);
    }
  }

  public Path getStaticLocationPath() {
    return this.staticLocationPath;
  }

  public Boolean hasStaticPagesFolder() {
    return Files.exists(staticLocationPath) && Files.isDirectory(staticLocationPath);
  }

  @Override
  public List<Pages> listStaticPages() {
    List<Pages> pages = new ArrayList<Pages>();
    if (!hasStaticPagesFolder()) {
      logger.warn("Static pages folder does not exist or is not a directory: {}", staticLocationPath.toAbsolutePath());
      return pages;
    }

    try (Stream<Path> walk = Files.walk(staticLocationPath)) {
      walk.filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".html"))
          .forEach(filePath -> {
            Path relativePath = staticLocationPath.relativize(filePath);
            String path = "/" + relativePath.toString().replace(File.separatorChar, '/'); // Ensure Unix-like path
            String title = getTitleFromPath(relativePath);
            pages.add(new Pages(title, path));
          });
    } catch (IOException e) {
      logger.error("Error listing static files from {}: {}", staticLocationPath.toAbsolutePath(), e.getMessage(), e);
    }
    return pages;
  }

  private String getTitleFromPath(Path relativePath) {
    String fileName = relativePath.getFileName().toString();
    if (fileName.equalsIgnoreCase("index.html")) {
      // If it's an index.html, use the parent directory name as title, or "Home" if it's the root index.
      Path parent = relativePath.getParent();
      if (parent == null || parent.toString().isEmpty()) {
        return "Home";
      }
      return capitalizeFirstLetter(parent.getFileName().toString().replace("-", " "));
    }
    // Remove .html extension and replace hyphens with spaces, then capitalize first letter
    String title = fileName.substring(0, fileName.lastIndexOf(".html"));
    return capitalizeFirstLetter(title.replace("-", " "));
  }

  private String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
}
