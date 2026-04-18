package com.kkphoenixgx.infrastructure.persistence.git;

import com.kkphoenixgx.infrastructure.persistence.IO.IOPersistence;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitPersistenceTest {

    @TempDir
    Path tempDir;

    private IOPersistence ioPersistence;
    private GitPersistence gitPersistence;

    private File remoteRepoDir;
    private File localStaticDir;

    @BeforeEach
    void setUp() throws Exception {
        ioPersistence = mock(IOPersistence.class);
        localStaticDir = tempDir.resolve("local-static").toFile();
        when(ioPersistence.getStaticLocationPath()).thenReturn(localStaticDir.toPath());

        gitPersistence = new GitPersistence(ioPersistence);

        // Configura um repositório git local temporário que simulará nosso "Remoto" (ex: Github)
        remoteRepoDir = tempDir.resolve("remote-repo").toFile();
        try (Git git = Git.init().setDirectory(remoteRepoDir).call()) {
            Files.writeString(remoteRepoDir.toPath().resolve("content.txt"), "Conteudo do repositorio remoto");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").call();
        }

        // Injeta os valores nas variáveis @Value para apontar para o repositório mockado
        ReflectionTestUtils.setField(gitPersistence, "gitRepoUrl", remoteRepoDir.toURI().toString());
        ReflectionTestUtils.setField(gitPersistence, "gitRepoBranch", "master");
        ReflectionTestUtils.setField(gitPersistence, "gitUsername", "");
        ReflectionTestUtils.setField(gitPersistence, "gitToken", "");
    }

    @Test
    void syncRepository_whenGitFolderDoesNotExist_shouldCloneRepository() {
        // Act
        gitPersistence.syncRepository();

        // Assert
        File clonedGitFolder = new File(localStaticDir, ".git");
        File clonedFile = new File(localStaticDir, "content.txt");
        assertTrue(clonedGitFolder.exists(), "A pasta .git deve ter sido criada após a clonagem");
        assertTrue(clonedFile.exists(), "O arquivo do repositório remoto deve existir localmente");
    }

    @Test
    void syncRepository_whenGitFolderExists_shouldPullChanges() throws Exception {
        // Arrange: Clonamos a primeira vez
        gitPersistence.syncRepository();

        // Inserimos um novo arquivo e um novo commit no repositório "remoto"
        try (Git git = Git.open(remoteRepoDir)) {
            Files.writeString(remoteRepoDir.toPath().resolve("new_content.txt"), "Conteudo Novo");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Second commit").call();
        }

        // Act: Sincronizamos novamente (isso vai acionar o pullChanges ao invés do clone)
        gitPersistence.syncRepository();

        // Assert
        File pulledFile = new File(localStaticDir, "new_content.txt");
        assertTrue(pulledFile.exists(), "As alterações devem ser baixadas através do fetch + reset");
    }

    @Test
    void syncRepository_withInvalidRepo_shouldHandleExceptionGracefully() {
        // Arrange: Apontando para um local que vai falhar propositalmente
        ReflectionTestUtils.setField(gitPersistence, "gitRepoUrl", "file:///non/existent/repo");

        // Act & Assert: O método deve ser tolerante a falha e apenas registrar log, sem crashar a aplicação
        gitPersistence.syncRepository();
        assertFalse(new File(localStaticDir, ".git").exists(), "Nenhuma pasta git deveria ser criada");
    }

    @Test
    void syncRepository_whenLocalGitIsCorrupted_shouldDeleteAndReclone() throws Exception {
        // Arrange: Clonamos e corrompemos a pasta .git local propositalmente (ex: deletando o HEAD)
        gitPersistence.syncRepository();
        new File(localStaticDir, ".git/HEAD").delete(); // Corrupção severa

        // Act: A sincronização deve falhar no pullChanges, capturar a exceção genérica e re-clonar do zero
        gitPersistence.syncRepository();
        assertTrue(new File(localStaticDir, ".git/HEAD").exists(), "O sistema deve ter se auto-recuperado e clonado do zero.");
    }

    @Test
    void syncRepository_whenPullingChanges_shouldNotReclone() throws Exception {
        // Arrange: First clone
        gitPersistence.syncRepository();
        File initialContent = new File(localStaticDir, "content.txt");
        assertTrue(initialContent.exists());

        // Add a new commit to the "remote"
        try (Git git = Git.open(remoteRepoDir)) {
            Files.writeString(remoteRepoDir.toPath().resolve("new_file.txt"), "new stuff");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("second commit").call();
        }

        // Act: Sync again, which should trigger a pull, not a clone.
        // We can verify this by checking if the original file is deleted and re-added,
        // which would happen on a re-clone but not on a pull (reset).
        long lastModifiedBeforePull = initialContent.lastModified();
        
        // Add a small delay to ensure the timestamp can change if the file is rewritten
        TimeUnit.SECONDS.sleep(1);

        gitPersistence.syncRepository();

        // Assert
        assertTrue(new File(localStaticDir, "new_file.txt").exists(), "New file from pull should exist.");
        assertEquals(lastModifiedBeforePull, initialContent.lastModified(), "Existing files should not be deleted and re-cloned on a simple pull.");
    }

    @Test
    void syncRepository_whenCalledConcurrently_shouldPreventOverlappingExecutions() throws InterruptedException {
        // Limpa o histórico do Mockito pois o construtor do GitPersistence (no setUp) já invocou este método uma vez.
        clearInvocations(ioPersistence);

        // Arrange: Injetamos um pequeno atraso no mock para forçar que a primeira thread retenha o Lock
        when(ioPersistence.getStaticLocationPath()).thenAnswer(invocation -> {
            Thread.sleep(500);
            return localStaticDir.toPath();
        });

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Act: Dispara 3 sincronizações exatamente ao mesmo tempo
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> gitPersistence.syncRepository());
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        // Assert: Garante que apenas UMA thread conseguiu passar pelo "isSyncing.compareAndSet"
        verify(ioPersistence, times(1)).getStaticLocationPath();
        AtomicBoolean isSyncingLock = (AtomicBoolean) ReflectionTestUtils.getField(gitPersistence, "isSyncing");
        assertFalse(isSyncingLock.get(), "O lock deve estar liberado no final");
    }
}