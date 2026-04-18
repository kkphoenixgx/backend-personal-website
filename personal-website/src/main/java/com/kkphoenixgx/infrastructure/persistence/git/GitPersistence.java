package com.kkphoenixgx.infrastructure.persistence.git;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import com.kkphoenixgx.infrastructure.persistence.IO.IOPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
// import org.eclipse.jgit.api.PullCommand;
// import org.eclipse.jgit.errors.RepositoryNotFoundException;
// import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
// import java.nio.file.Files;
// import java.nio.file.Paths;

@Component
public class GitPersistence {

  private static final Logger logger = LoggerFactory.getLogger(GitPersistence.class);

  @Value("${git.repo.url}")
  private String gitRepoUrl;

  @Value("${git.repo.branch:main}")
  private String gitRepoBranch;

  @Value("${git.username:}")
  private String gitUsername;

  @Value("${git.token:}")
  private String gitToken;

  private final IOPersistence ioPersistence;
  private final AtomicBoolean isSyncing = new AtomicBoolean(false);

  public GitPersistence(IOPersistence ioPersistence) {
    this.ioPersistence = ioPersistence;
    // Log the path for consistency
    Path localRepoPath = ioPersistence.getStaticLocationPath();
    logger.info("GitPersistence will manage the repository at: {}", localRepoPath.toAbsolutePath());
  }

  @Scheduled(initialDelayString = "${git.sync.initial-delay:10000}", fixedRateString = "${git.sync.fixed-rate:86400000}") // Run 10 seconds after startup, then every day
  @Async
  public void syncRepository() {
    if (!isSyncing.compareAndSet(false, true)) {
        logger.warn("Sincronização já está em andamento. Ignorando execução sobreposta para evitar Race Conditions.");
        return;
    }

    try {
      Path localRepoPath = ioPersistence.getStaticLocationPath();
      logger.info("Starting Git operation for repository: {}", gitRepoUrl);
      File localRepoDir = localRepoPath.toFile();
      File gitDir = new File(localRepoDir, ".git");

      if (!gitDir.exists()) {
        logger.info("Local repository .git directory not found. Cloning repository...");
        // If the directory exists but is empty or incomplete (no .git), clean it up.
        if (localRepoDir.exists()) {
          deleteDirectory(localRepoDir);
        }
        cloneRepository(localRepoDir);
        return;
      }

      logger.info("Local repository found. Attempting to pull changes.");
      try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
        pullChanges(repository);
      } catch (Exception e) {
        logger.error("Corrupção detectada ou falha ao realizar pull no repositório. Acionando Fallback: Limpando e re-clonando...", e);
        deleteDirectory(localRepoDir);
        cloneRepository(localRepoDir);
      }
    } finally {
        isSyncing.set(false); // Libera o Lock
    }
  }

  private void cloneRepository(File localRepoDir) {
    logger.info("Cloning {} into {}", gitRepoUrl, localRepoDir.getAbsolutePath());
    // Ensure parent directory exists before cloning
    if (!localRepoDir.exists()) {
      if (localRepoDir.mkdirs()) {
        logger.info("Created directory for repository: {}", localRepoDir.getAbsolutePath());
      }
    }
    try {
      CloneCommand cloneCommand = Git.cloneRepository()
        .setURI(gitRepoUrl)
        .setDirectory(localRepoDir)
        .setBranch(gitRepoBranch) // Garante que estamos clonando a branch correta
        .setCloneAllBranches(false) // Otimiza a clonagem baixando apenas a branch necessária
        .setDepth(1) // Otimização: clona apenas o último commit (shallow clone)
        .setTimeout(600) // Aumenta o timeout de conexão
        .setProgressMonitor(new LoggingProgressMonitor()); // Loga o progresso no console

      if (gitUsername != null && !gitUsername.isEmpty() && gitToken != null && !gitToken.isEmpty()) {
        cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitToken));
      }

      cloneCommand.call();
      logger.info("Repository cloned successfully.");
    } catch (GitAPIException e) {
      logger.error("Failed to clone repository {}: {}", gitRepoUrl, e.getMessage(), e);
    }
  }

private void pullChanges(Repository repository) {
  try (Git git = new Git(repository)) {
      // 1. Baixa as novidades do GitHub APENAS para a branch configurada
      org.eclipse.jgit.api.FetchCommand fetchCommand = git.fetch()
          .setRemote("origin")
          .setRefSpecs(new RefSpec("+refs/heads/" + gitRepoBranch + ":refs/remotes/origin/" + gitRepoBranch))
          .setDepth(1) // Otimização: busca apenas o último commit (shallow fetch)
          .setTimeout(600)
          .setProgressMonitor(new LoggingProgressMonitor());

      if (gitUsername != null && !gitUsername.isEmpty() && gitToken != null && !gitToken.isEmpty()) {
          fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUsername, gitToken));
      }
      
      fetchCommand.call();

      // 2. Força o servidor a ficar identico ao que acabou de baixar (o que você fez no terminal)
      git.reset()
          .setRef("origin/" + gitRepoBranch)
          .setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
          .call();

      logger.info("Sincronização forçada (Fetch + Reset) concluída com sucesso.");
  } catch (GitAPIException e) {
      logger.error("Erro ao sincronizar repositório: {}", e.getMessage(), e);
  }
}
  
  private boolean deleteDirectory(File directoryToBeDeleted) {
    Path path = directoryToBeDeleted.toPath();
    // Safety check: only proceed if the directory actually exists.
    if (!directoryToBeDeleted.exists()) {
      logger.info("Directory does not exist, no deletion necessary: {}", path);
      return true;
    }

    // Do not delete if the directory is empty
    if (directoryToBeDeleted.list().length == 0) {
      logger.info("Directory is empty, no deletion necessary: {}", path);
      return true;
    }

    try (var walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      logger.info("Successfully deleted directory: {}", path);
      return true;
    } catch (IOException e) {
      logger.error("Failed to delete directory: {}", path, e);
      return false;
    }
  }
}