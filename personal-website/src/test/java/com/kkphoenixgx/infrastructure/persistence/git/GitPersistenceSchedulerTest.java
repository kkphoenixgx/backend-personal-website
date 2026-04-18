package com.kkphoenixgx.infrastructure.persistence.git;

import com.kkphoenixgx.Application.App;
import com.kkphoenixgx.infrastructure.persistence.IO.IOPersistence;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = App.class)
class GitPersistenceSchedulerTest {

    @SpyBean
    private GitPersistence gitPersistence;

    @SpyBean
    private IOPersistence ioPersistence;

    private static Path dummyRemoteRepo;
    private static Path localStaticSites = Path.of("./target/test-scheduler-sites/");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("git.sync.initial-delay", () -> "100");
        registry.add("git.sync.fixed-rate", () -> "500");
        // Aponta para o repositório temporário local, validando o caminho de SUCESSO do JGit
        registry.add("git.repo.url", () -> dummyRemoteRepo.toUri().toString());
        registry.add("git.repo.branch", () -> "master");
        registry.add("app.static.pages", () -> "file:" + localStaticSites.toAbsolutePath() + "/");
    }

    @BeforeAll
    static void setupDummyRepo() throws Exception {
        dummyRemoteRepo = Files.createTempDirectory("dummy-remote-repo");
        try (Git git = Git.init().setDirectory(dummyRemoteRepo.toFile()).call()) {
            Files.writeString(dummyRemoteRepo.resolve("test.txt"), "scheduler test");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init").call();
        }
    }

    @AfterAll
    static void cleanup() throws IOException {
        deleteDir(dummyRemoteRepo);
        deleteDir(localStaticSites);
    }

    @BeforeEach
    void setUp() throws IOException {
        Mockito.reset(gitPersistence, ioPersistence);
        deleteDir(localStaticSites);
        
        // Força a liberação do lock antes do teste para garantir um ambiente limpo
        Object target = AopTestUtils.getTargetObject(gitPersistence);
        AtomicBoolean lock = (AtomicBoolean) ReflectionTestUtils.getField(target, "isSyncing");
        if (lock != null) lock.set(false);
    }

    private static void deleteDir(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    void shouldTriggerSyncAndReleaseLock_whenFolderDoesNotExist() throws InterruptedException, IOException {
        // Act: Aguardamos os disparos do agendador (aumentado para 2.5s para 
        // prevenir falsos negativos em hardwares lentos como o GitHub Actions)
        Thread.sleep(2500);

        // Assert: 1. O método foi chamado pelo Spring
        verify(gitPersistence, atLeast(2)).syncRepository();
        
        // 2. A lógica INTERNA foi executada (provando que não barrou no lock do if)
        verify(ioPersistence, atLeast(2)).getStaticLocationPath();

        // 3. Ao final, a flag de trava DEVE ter sido liberada
        Object target = AopTestUtils.getTargetObject(gitPersistence);
        AtomicBoolean lock = (AtomicBoolean) ReflectionTestUtils.getField(target, "isSyncing");
        assertFalse(lock.get(), "O lock isSyncing deve estar false (liberado) após as clonagens.");
    }

    @Test
    void shouldTriggerSyncAndReleaseLock_whenFolderExists() throws InterruptedException, IOException {
        // Arrange: Força o caminho de PULL criando a pasta .git
        Files.createDirectories(localStaticSites.resolve(".git"));

        Thread.sleep(2500);

        verify(gitPersistence, atLeast(2)).syncRepository();
        verify(ioPersistence, atLeast(2)).getStaticLocationPath();
        
        Object target = AopTestUtils.getTargetObject(gitPersistence);
        AtomicBoolean lock = (AtomicBoolean) ReflectionTestUtils.getField(target, "isSyncing");
        assertFalse(lock.get(), "O lock isSyncing deve estar false (liberado) após os PULLs.");
    }
}