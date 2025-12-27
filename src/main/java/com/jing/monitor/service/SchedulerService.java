package com.jing.monitor.service;

import com.jing.monitor.core.CourseCrawler;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import com.jing.monitor.model.Task;
import com.jing.monitor.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Optional: Good for logging
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

/**
 * Service responsible for scheduling course monitoring tasks.
 * It polls the UW API and compares the current status with the persisted status in the database.
 */
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final CourseCrawler crawler;
    private final MailService mailService;
    private final TaskRepository taskRepository;

    // Define alert actions to determine the notification strategy
    enum AlertAction {
        NONE,
        SEND_OPEN_EMAIL,
        SEND_WAITLIST_EMAIL
    }

    /**
     * Scheduled task to poll course status.
     * Frequency is defined in application.properties under 'monitor.poll-interval-ms'.
     */
    @Scheduled(fixedRateString = "${monitor.poll-interval-ms}")
    public void monitorTask() {
        // 1. Retrieve all enabled tasks from the database
        List<Task> tasks = taskRepository.findByEnabledTrue();

        if (tasks.isEmpty()) {
            System.out.println("No active tasks...");
            return;
        }

        System.out.println("[Scheduler] Starting scan cycle. Active tasks: " + tasks.size());

        // 2. Iterate and process each task
        for (Task task : tasks) {
            processSingleTask(task);
        }
    }

    private void processSingleTask(Task task) {
        try {
            // 1. Fetch current status via Crawler
            // Note: We pass the Task object which contains courseId and sectionId
            SectionInfo info = crawler.fetchCourseStatus(task);

            // If fetching fails (network error or parsing error), skip this cycle
            if (info == null) {
                System.err.println("[Error] Fetch failed for task: " + task.getCourseDisplayName());
                return;
            }

            StatusMapping currentStatus = info.getStatus();

            // 2. Retrieve the last known status from the Database Entity
            // This ensures persistence even after server restarts
            StatusMapping previousStatus = task.getLastStatus();

            System.out.println("[Checking] " + task.getCourseDisplayName() + " [Status: " + currentStatus + "]");

            // 3. Determine if an alert needs to be sent based on state transition logic
            AlertAction action = determineAction(previousStatus, currentStatus);

            // 4. Execute Alert Action
            if (action == AlertAction.SEND_OPEN_EMAIL) {
                System.out.println("🔥 ALERT: OPEN detected for " + task.getSectionId());
                mailService.sendCourseOpenAlert(task.getSectionId(), task.getCourseDisplayName());
            } else if (action == AlertAction.SEND_WAITLIST_EMAIL) {
                System.out.println("⚠️ ALERT: WAITLIST detected for " + task.getSectionId());
                mailService.sendCourseWaitlistedAlert(task.getSectionId(), task.getCourseDisplayName());
            }

            // 5. State Persistence
            // Update the database only if the status has changed to reduce write operations
            if (previousStatus != currentStatus) {
                System.out.println("🔄 State changed: " + previousStatus + " -> " + currentStatus + ". Updating DB.");

                task.setLastStatus(currentStatus);
                taskRepository.save(task); // Performs an UPDATE SQL operation
            }

        } catch (Exception e) {
            System.err.println("Error processing task " + task.getSectionId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determines the appropriate alert action based on state transition.
     * Implements anti-spam logic to prevent repeated alerts for the same status.
     *
     * @param prev The last known status (from DB)
     * @param curr The current status (from Crawler)
     * @return The action to take
     */
    private AlertAction determineAction(StatusMapping prev, StatusMapping curr) {
        // Case 1: First run (New Task)
        if (prev == null) {
            if (curr == StatusMapping.OPEN) return AlertAction.SEND_OPEN_EMAIL;
            if (curr == StatusMapping.WAITLISTED) return AlertAction.SEND_WAITLIST_EMAIL;
            return AlertAction.NONE;
        }

        // Case 2: Anti-Spam (Status hasn't changed)
        if (prev == curr) {
            return AlertAction.NONE;
        }

        // Case 3: State Transition Logic
        switch (curr) {
            case OPEN:
                // Always alert on OPEN, regardless of previous state
                return AlertAction.SEND_OPEN_EMAIL;

            case WAITLISTED:
                // Only alert if moving from CLOSED -> WAITLISTED.
                // Downgrading from OPEN -> WAITLISTED should remain silent.
                if (prev == StatusMapping.CLOSED) {
                    return AlertAction.SEND_WAITLIST_EMAIL;
                }
                return AlertAction.NONE;

            case CLOSED:
                // Never alert on CLOSED, just update the DB cache.
                return AlertAction.NONE;

            default:
                return AlertAction.NONE;
        }
    }
}