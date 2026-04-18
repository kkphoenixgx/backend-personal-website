package com.kkphoenixgx.infrastructure.persistence.IO;

import com.kkphoenixgx.domain.model.Pages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IOPersistenceTest {

    @TempDir
    Path tempDir;

    private IOPersistence ioPersistence;

    @BeforeEach
    void setUp() throws IOException {
        // Cria uma estrutura de diretórios temporária para o teste
        Path staticSites = tempDir.resolve("static-sites");
        Files.createDirectories(staticSites);

        Files.createDirectories(staticSites.resolve("RPG"));
        Files.createFile(staticSites.resolve("RPG/index.html"));
        Files.createFile(staticSites.resolve("RPG/personagens.html"));

        Files.createDirectories(staticSites.resolve("Study"));
        Files.createFile(staticSites.resolve("Study/conceitos.html"));
        Files.createFile(staticSites.resolve("Study/unsupported-file.txt"));

        Files.createDirectories(staticSites.resolve(".git")); // Deve ser ignorado
        Files.createFile(staticSites.resolve(".git/config"));

        Resource staticLocationResource = new FileSystemResource(staticSites.toFile());
        ioPersistence = new IOPersistence(staticLocationResource);
    }

    @Test
    void listStaticPages_shouldCorrectlyBuildPageTreeAndIgnoreGit() {
        // Act
        List<Pages> pages = ioPersistence.listStaticPages();

        // Assert
        List<String> titles = pages.stream().map(Pages::getTitle).collect(Collectors.toList());
        
        assertEquals(2, pages.size(), "Should find 2 top-level directories (RPG, Study)");
        assertTrue(titles.contains("RPG")); // Capitalizado do nome do diretório
        assertTrue(titles.contains("Study"));
        assertFalse(titles.contains(".git"), ".git directory should be ignored");

        // Valida o conteúdo do diretório RPG
        Pages rpgDir = pages.stream().filter(p -> p.getTitle().equals("RPG")).findFirst().orElse(null);
        assertNotNull(rpgDir);
        assertEquals(1, rpgDir.getItems().size(), "RPG directory should have 1 item (personagens.html)");
        assertEquals("Personagens", rpgDir.getItems().get(0).getTitle());
        assertEquals("/RPG/personagens.html", rpgDir.getItems().get(0).getPath());
        assertEquals("/RPG/index.html", rpgDir.getPath(), "Path for directory with index.html should point to it");
    }

    @Test
    void constructor_whenDirectoryDoesNotExist_shouldCreateDirectory() {
        // Arrange
        Path nonExistentPath = tempDir.resolve("non-existent-dir");
        Resource nonExistentResource = new FileSystemResource(nonExistentPath);

        // Act
        new IOPersistence(nonExistentResource);

        // Assert
        assertTrue(Files.exists(nonExistentPath), "O construtor deve criar o diretório base se não existir.");
    }

    @Test
    void listStaticPages_whenDirectoryIsDeletedAfterInitialization_shouldReturnEmptyList() throws IOException {
        // Arrange: O diretório é criado no setUp() e o ioPersistence é inicializado.

        // Act: Deleta o diretório antes de chamar listStaticPages
        Path staticSites = tempDir.resolve("static-sites");
        Files.walk(staticSites)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);

        // Assert
        List<Pages> pages = ioPersistence.listStaticPages();
        assertTrue(pages.isEmpty(), "Deve retornar uma lista vazia se o diretório foi apagado em tempo de execução");
    }

    @Test
    void listStaticPages_shouldIgnoreUnsupportedFileTypes() {
        // Act
        List<Pages> pages = ioPersistence.listStaticPages();

        // Assert
        Pages studyDir = pages.stream().filter(p -> p.getTitle().equals("Study")).findFirst().get();
        assertEquals(1, studyDir.getItems().size(), "Deve incluir apenas arquivos suportados (.html) e ignorar .txt");
    }

    @Test
    void listStaticPages_withLargeScaleTree_shouldHandle10000FilesEfficiently() throws IOException {
        // Arrange: Gera 10.000 arquivos temporários no tempDir para um teste de estresse na recursão (Memory Leak / StackOverflow)
        Path largeDir = tempDir.resolve("static-sites/LargeScale");
        Files.createDirectories(largeDir);
        for (int i = 0; i < 10000; i++) {
            Files.createFile(largeDir.resolve("page" + i + ".html"));
        }

        // Act
        long startTime = System.currentTimeMillis();
        List<Pages> pages = ioPersistence.listStaticPages();
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        Pages largeDataNode = pages.stream().filter(p -> p.getTitle().equals("LargeScale")).findFirst().orElse(null);
        assertNotNull(largeDataNode, "O diretório LargeScale deve ser lido com sucesso.");
        assertEquals(10000, largeDataNode.getItems().size(), "Deve conter 10.000 itens listados.");
        assertTrue(duration < 5000, "O tempo de processamento de 10.000 arquivos deve ser menor que 5 segundos (levou " + duration + "ms).");
    }
}