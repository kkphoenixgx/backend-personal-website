package com.kkphoenixgx.infrastructure.persistence.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
// import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import com.kkphoenixgx.infrastructure.persistence.IO.IOPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class GitPersistence {

  private static final Logger logger = LoggerFactory.getLogger(GitPersistence.class);

  @Value("${git.repo.url}")
  private String gitRepoUrl;

  @Value("${git.repo.branch:main}")
  private String gitRepoBranch;

  private final IOPersistence ioPersistence;

  public GitPersistence(IOPersistence ioPersistence) {
    this.ioPersistence = ioPersistence;
    // Log the path for consistency
    Path localRepoPath = ioPersistence.getStaticLocationPath();
    logger.info("GitPersistence will manage the repository at: {}", localRepoPath.toAbsolutePath());
  }

  @Scheduled(initialDelay = 1000 * 10, fixedRate = 1000 * 60 * 60 * 24) // Run 10 seconds after startup, then every day
  @Async
  public void syncRepository() {
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
    } catch (IOException | IllegalArgumentException e) {
      logger.error("Failed to open existing repository at {}. Error: {}", gitDir.getAbsolutePath(), e.getMessage(), e);
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
      Git.cloneRepository()
        .setURI(gitRepoUrl)
        .setDirectory(localRepoDir)
        .setTimeout(30) // Set timeout to 30 seconds
        .setProgressMonitor(new LoggingProgressMonitor())
        .call();
      logger.info("Repository cloned successfully.");
    } catch (GitAPIException e) {
      logger.error("Failed to clone repository {}: {}", gitRepoUrl, e.getMessage(), e);
    }
  }

  private void pullChanges(Repository repository) {
    logger.info("Local repository found. Performing pull operation in {}", repository.getDirectory().getParent());
    try (Git git = new Git(repository)) {
      git.pull()
        .setRebase(true) // Use rebase for a cleaner and often faster pull
        .setTimeout(30) // Set timeout to 30 seconds
        .setProgressMonitor(new LoggingProgressMonitor()).call();
      logger.info("Repository pull completed successfully.");
    } catch (GitAPIException e) {
      logger.error("Failed to pull repository {}: {}", gitRepoUrl, e.getMessage(), e);
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