package com.kkphoenixgx.infrastructure.web;

import com.kkphoenixgx.Application.App;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = App.class)
@AutoConfigureMockMvc
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final Path TEST_DIR = Path.of("target/test-static-sites");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.static.pages", () -> "file:" + TEST_DIR.toAbsolutePath().toString() + "/");
    }

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(TEST_DIR.resolve("Study/img"));
        Path image = TEST_DIR.resolve("Study/img/test-image.jpg");
        Files.writeString(image, "image content");
    }

    @Test
    void forward_shouldForwardToHtmlFile_forCleanUrl() throws Exception {
        mockMvc.perform(get("/blog/my-post"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/blog/my-post.html"));
    }

    @Test
    void forward_shouldForwardToIndexHtml_forDirectoryUrl() throws Exception {
        mockMvc.perform(get("/blog/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/blog/index.html"));
    }

    @Test
    void serveAsset_shouldFindAndServeAssetFromSubdirectory() throws Exception {
        mockMvc.perform(get("/Study/assets/images/test-image.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().string("image content"));
    }

    @Test
    void serveAsset_shouldFindImageInImgFolder_whenRequestedFromDeepStudyPath() throws Exception {
        // Verifica se uma requisição de imagem "perdida" em uma sub-rota aponta corretamente para a pasta /img/
        mockMvc.perform(get("/Study/Conceitos/Biologia/test-image.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().string("image content"));
    }

    @Test
    void serveAsset_whenImageIsMissing_shouldReturn404() throws Exception {
        // Verifica se imagens realmente inexistentes retornam 404 e não quebram o sistema
        mockMvc.perform(get("/Study/img/non-existent-image.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void serveAsset_shouldPreventPathTraversal() throws Exception {
        // Verifica se o sistema bloqueia tentativas de escapar do diretório base
        mockMvc.perform(get("/../etc/passwd/test-image.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void directHtmlAccess_shouldRedirectToCleanUrl() throws Exception {
        // Previne vazamento de abstração redirecionando requisições .html para a URL limpa
        mockMvc.perform(get("/blog/my-post.html"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/blog/my-post"));
    }

    @Test
    void directIndexHtmlAccess_shouldRedirectToDirectoryCleanUrl() throws Exception {
        mockMvc.perform(get("/blog/index.html"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/blog/"));
    }
}