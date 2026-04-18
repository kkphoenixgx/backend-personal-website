package com.kkphoenixgx.infrastructure.persistence.git;

import com.kkphoenixgx.Application.App;
import com.kkphoenixgx.infrastructure.persistence.IO.IOPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.AopTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = App.class)
@TestPropertySource(properties = {
    // Aponta para o repositório público configurado para este teste
    "git.repo.url=https://github.com/kkphoenixgx/ObsidianStaticSites",
    "git.repo.branch=master",
    // Cria um diretório de teste isolado para evitar conflito com o /static-sites local original
    "app.static.pages=file:./target/real-github-test-sites/"
})
class GitPersistenceRealGitHubIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(GitPersistenceRealGitHubIntegrationTest.class);

    @Autowired
    private GitPersistence gitPersistence;

    @Autowired
    private IOPersistence ioPersistence;

    private AtomicBoolean getIsSyncing() {
        Object target = AopTestUtils.getTargetObject(gitPersistence);
        return (AtomicBoolean) ReflectionTestUtils.getField(target, "isSyncing");
    }

    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        Path path = ioPersistence.getStaticLocationPath();
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
        // Garante que o lock não esteja preso de execuções anteriores no teste
        getIsSyncing().set(false);
    }

    @Test
    void shouldCloneAndPullFromRealGithubWithinTimeLimit() throws InterruptedException {
        Path localPath = ioPersistence.getStaticLocationPath();
        File gitFolder = new File(localPath.toFile(), ".git");

        logger.info("Iniciando teste de integração real com o GitHub (Timeout de 30 minutos)...");

        // FASE 1: Clonagem Inicial
        // Chamamos manualmente para não esperar o delay do @Scheduled
        gitPersistence.syncRepository();

        // Aguarda a thread @Async iniciar
        Thread.sleep(2000);

        int maxWaitSeconds = 1800; // Limite tolerante de 30 minutos
        boolean clonedSuccessfully = false;

        for (int i = 0; i < maxWaitSeconds; i++) {
            if (!getIsSyncing().get()) {
                if (gitFolder.exists() && localPath.toFile().list() != null && localPath.toFile().list().length > 1) {
                    clonedSuccessfully = true;
                    logger.info("Repositório clonado com sucesso em aproximadamente {} segundos.", i);
                } else {
                    logger.error("Sincronização finalizou (lock liberado), mas os arquivos não foram encontrados.");
                }
                break; // Sai do loop pois a sincronização já terminou (com sucesso ou falhou)
            }
            Thread.sleep(1000); 
        }

        assertTrue(clonedSuccessfully, "A clonagem real do GitHub travou ou não completou em 30 minutos. Verifique a conexão com a internet ou se o Git emitiu timeout.");

        // FASE 2: Atualização Diária (Force Pull)
        logger.info("Testando o force PULL (Fetch + Hard Reset) após a clonagem...");
        
        // Captura o timestamp do FETCH_HEAD antes da sincronização para provar que ele mudará
        File fetchHead = new File(gitFolder, "FETCH_HEAD");
        long lastModifiedBeforePull = fetchHead.exists() ? fetchHead.lastModified() : 0;

        // Inicia a sincronização de um repositório já existente
        gitPersistence.syncRepository();

        boolean pullFinishedSuccessfully = false;
        boolean fetchHeadUpdated = false;

        for (int i = 0; i < maxWaitSeconds; i++) {
             // Quando isSyncing for redefinida para false pela rotina, o fluxo terminou
             if (!getIsSyncing().get()) { 
                 // O lock soltou. Agora verificamos se o arquivo do Git foi realmente atualizado pela rede
                 if (fetchHead.exists() && fetchHead.lastModified() > lastModifiedBeforePull) {
                     pullFinishedSuccessfully = true;
                     fetchHeadUpdated = true;
                     logger.info("Pull concluído e validado pelo FETCH_HEAD em aproximadamente {} segundos.", i);
                     break;
                 } else if (i > 5) { // Dá uma pequena margem para o IO de disco confirmar
                     logger.error("Sincronização terminou, mas o FETCH_HEAD não foi atualizado. Falso positivo detectado!");
                     break;
                 }
             }
             Thread.sleep(1000);
        }
        
        assertTrue(fetchHeadUpdated, "O teste falhou. O pull pode ter sofrido 'Fail Fast' sem bater na rede ou o timeout de 30 min foi excedido.");
        assertTrue(gitFolder.exists(), "O diretório .git não sobreviveu ao pull. Verifique se o fetch não lançou exceção apagando o local.");
        
        logger.info("Integração real com GitHub testada com sucesso!");
    }
}