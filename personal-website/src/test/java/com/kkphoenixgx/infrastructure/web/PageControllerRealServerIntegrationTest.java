package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.Application.App;
import com.kkphoenixgx.infrastructure.persistence.git.GitPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PageControllerRealServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Evita que o clone real do Git sobrescreva nossos arquivos HTML deste teste
    @MockBean
    private GitPersistence gitPersistence;

    private static final Path TEST_DIR = Path.of("target/real-server-routing-test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.static.pages", () -> "file:" + TEST_DIR.toAbsolutePath().toString() + "/");
    }

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(TEST_DIR);
        Files.writeString(TEST_DIR.resolve("index.html"), "<h1>Root Home Page</h1>");
        
        Files.createDirectories(TEST_DIR.resolve("blog"));
        Files.writeString(TEST_DIR.resolve("blog/index.html"), "<h1>Blog Index</h1>");
    }

    @AfterAll
    static void teardown() throws IOException {
        if (Files.exists(TEST_DIR)) {
            try (Stream<Path> walk = Files.walk(TEST_DIR)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }

    @Test
    void shouldServeRootIndexWithoutRedirectLoop() {
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "A requisição deve retornar 200 OK.");
        assertTrue(response.getBody() != null && response.getBody().contains("Root Home Page"), "A página deve ser servida com sucesso sem loops.");
    }

    @Test
    void shouldResolveCleanUrlIndexGracefullyAndServeRoot() {
        String url = "http://localhost:" + port + "/index";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "A requisição para /index deve seguir os redirecionamentos de forma segura e retornar 200 OK.");
        assertTrue(response.getBody() != null && response.getBody().contains("Root Home Page"), "O fallback para a Home Page deve ocorrer corretamente.");
    }

    @Test
    void shouldRedirectDotHtmlToCleanUrlAndServeContent() {
        String url = "http://localhost:" + port + "/blog/index.html";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("Blog Index"), "A requisição para .html deve redirecionar para a clean URL e servir o conteúdo final.");
    }
}