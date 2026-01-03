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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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
    if (!hasStaticPagesFolder()) {
      logger.warn("Static pages folder does not exist or is not a directory: {}", staticLocationPath.toAbsolutePath());
      return new ArrayList<>();
    }
    return getPagesInDirectory(staticLocationPath);
  }

  private List<Pages> getPagesInDirectory(Path dir) {
    List<Pages> pages = new ArrayList<>();
    try (Stream<Path> stream = Files.list(dir)) {
      List<Path> entries = stream.sorted(Comparator.comparing(Path::getFileName)).collect(Collectors.toList());

      for (Path entry : entries) {
        if (entry.getFileName().toString().equals(".git")) {
          continue;
        }

        if (Files.isDirectory(entry)) {
          List<Pages> children = getPagesInDirectory(entry);

          String title;
          String path;

          Path indexFile = entry.resolve("index.html");
          if (Files.exists(indexFile) && Files.isRegularFile(indexFile)) {
            Path relativePath = staticLocationPath.relativize(indexFile);
            title = getTitleFromPath(relativePath);
            path = "/" + relativePath.toString().replace(File.separatorChar, '/');
          } else {
            Path relativePath = staticLocationPath.relativize(entry);
            title = capitalizeFirstLetter(entry.getFileName().toString().replace("-", " "));
            path = "/" + relativePath.toString().replace(File.separatorChar, '/');
          }

          Pages folderPage = new Pages(title, path);
          folderPage.getItems().addAll(children);
          pages.add(folderPage);
        } else if (Files.isRegularFile(entry) && (entry.toString().toLowerCase().endsWith(".html")
            || entry.toString().toLowerCase().endsWith(".jpg")
            || entry.toString().toLowerCase().endsWith(".jpeg")
            || entry.toString().toLowerCase().endsWith(".png")
            || entry.toString().toLowerCase().endsWith(".gif"))) {
          if (entry.getFileName().toString().equalsIgnoreCase("index.html") && !dir.equals(staticLocationPath)) {
            continue;
          }
          Path relativePath = staticLocationPath.relativize(entry);
          String title = getTitleFromPath(relativePath);
          String path = "/" + relativePath.toString().replace(File.separatorChar, '/');
          pages.add(new Pages(title, path));
        }
      }
    } catch (IOException e) {
      logger.error("Error listing files in directory {}: {}", dir, e.getMessage(), e);
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
    // Remove file extension and replace hyphens with spaces, then capitalize first letter
    int lastDot = fileName.lastIndexOf('.');
    String title = (lastDot >= 0) ? fileName.substring(0, lastDot) : fileName;
    return capitalizeFirstLetter(title.replace("-", " "));
  }

  private String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
}
