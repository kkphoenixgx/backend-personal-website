package com.kkphoenixgx.infrastructure.persistence.git;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProgressMonitor implements ProgressMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingProgressMonitor.class);

    private int completedTasks;
    private int totalWork;
    private int workCompleted;
    private int lastPercent;
    private String currentTask;
  
  @Override
  public void start(int totalTasks) { // totalTasks from JGit can sometimes be an estimate.
    this.completedTasks = 0;
    logger.info("Starting Git operation with {} tasks.", totalTasks);
  }
  
  @Override
  public void update(int completed) {
        if (completed == 0) {
            return;
        }
        workCompleted += completed;
        if (totalWork > 0) {
            int percent = (100 * workCompleted) / totalWork;
            if (percent > lastPercent && percent % 10 == 0) {
                logger.info("... {}: {}% ({} / {})", currentTask, percent, workCompleted, totalWork);
                lastPercent = percent;
            }
        }
  }
  
  @Override
  public void beginTask(String title, int totalWork) {
        this.currentTask = title;
        this.totalWork = totalWork;
        this.workCompleted = 0;
        this.lastPercent = 0;
    logger.info("Beginning task: {} (Total work: {})", title, totalWork);
  }
  
  @Override
  public void endTask() {
    completedTasks++;
    logger.info("Completed task {}: {}", completedTasks, currentTask);
  }
  
  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public void showDuration(boolean enabled) {
    // This method is required by the interface but not needed for this simple implementation.
  }
  
}
