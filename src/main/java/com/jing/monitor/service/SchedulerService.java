package com.jing.monitor.service;

import com.jing.monitor.core.CourseCrawler;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import com.jing.monitor.model.Task;
import com.jing.monitor.model.User;
import com.jing.monitor.repository.FileRepository;
import com.jing.monitor.repository.TaskRepository;
import com.jing.monitor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for scheduling course monitoring tasks.
 * Refactored V1.0: Implements Course-Level batch fetching to reduce API request frequency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final CourseCrawler crawler;
    private final MailService mailService;
    private final FileRepository fileRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    /**
     * Supported alert operations derived from status transitions.
     */
    enum AlertAction { NONE, SEND_OPEN_EMAIL, SEND_WAITLIST_EMAIL }

    /**
     * Main Monitoring Loop.
     * Frequency should be set conservatively (e.g., 3-5 minutes) to avoid WAF blocking.
     */
    @Scheduled(fixedDelayString = "${monitor.poll-interval-ms}")
    public void monitorTask() {
        // 1) Build monitoring work units by grouping enabled tasks per (user, course).
        List<Task> tasks = taskRepository.findByEnabledTrueAndUserIdIsNotNull();
        Map<Long, Set<String>> userCourseMap = new HashMap<>();
        for (Task task : tasks) {
            if (task.getUserId() == null || task.getCourseId() == null) {
                continue;
            }
            userCourseMap.computeIfAbsent(task.getUserId(), k -> new HashSet<>()).add(task.getCourseId());
        }

        if (userCourseMap.isEmpty()) {
            log.info("[Scheduler] No active tasks. Idle.");
            return;
        }

        int totalCourses = userCourseMap.values().stream().mapToInt(Set::size).sum();
        Map<Long, String> userEmailMap = userRepository.findAllById(userCourseMap.keySet()).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        log.info("[Scheduler] Starting cycle. Monitoring {} user-course groups.", totalCourses);

        int processed = 0;
        // 2) Process each course once per user to reduce external API calls.
        for (Map.Entry<Long, Set<String>> entry : userCourseMap.entrySet()) {
            Long userId = entry.getKey();
            String recipientEmail = userEmailMap.get(userId);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("[Scheduler] Skipping user {} because email is missing.", userId);
                continue;
            }
            for (String courseId : entry.getValue()) {
                processSingleCourse(userId, recipientEmail, courseId);
                processed++;
                try {
                    if (processed < totalCourses) {
                        long sleepTime = 120000 + random.nextInt(10000);
                        Thread.sleep(sleepTime);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Fetches all sections for a given course and updates local Task states.
     * @param userId The owner user id.
     * @param recipientEmail The owner user email.
     * @param courseId The 6-digit course identifier (e.g., "004289")
     */
    private void processSingleCourse(Long userId, String recipientEmail, String courseId) {
        try {
            // Step 1: Fetch all sections for this course in one external request.
            List<SectionInfo> infos = crawler.fetchCourseStatus(courseId);

            if (infos == null) {
                log.warn("[Scheduler] Fetch failed or blocked for user {} course {}", userId, courseId);
                return;
            }

            // Step 2: Build fast lookup table for user-owned tasks in this course.
            List<Task> existingTasksForCourse = taskRepository.findAllByCourseIdAndUserId(courseId, userId);
            Map<String, Task> taskMapBySectionId = existingTasksForCourse.stream()
                    .collect(Collectors.toMap(Task::getSectionId, Function.identity(), (left, right) -> left, HashMap::new));

            // Step 3: Synchronize section snapshots into owned tasks.
            for (SectionInfo info : infos) {
                StatusMapping currentStatus = info.getStatus();
                StatusMapping previousStatus = null;
                String sectionId = info.getSection();

                // O(1) lookup avoids query-per-section overhead.
                Task task = taskMapBySectionId.get(sectionId);

                // Auto-discovery creates missing sections; existing rows update transition states.
                if (task == null) {
                    // New section discovered under the same user+course ownership.
                    task = new Task(info.getSubject(), info.getCatalogNumber(), sectionId, courseId, info.getStatus());
                    task.setUserId(userId);
                    taskMapBySectionId.put(sectionId, task);
                    log.info("[Scheduler] New section found {}. Adding to DB.", info.getSection());

                    AlertAction action = determineAction(null, currentStatus);
                    dispatchMail(action, recipientEmail, info);
                } else {
                    // Existing task transition handling.
                    previousStatus = task.getLastStatus();

                    if (task.isEnabled()) {
                        AlertAction action = determineAction(previousStatus, currentStatus);
                        dispatchMail(action, recipientEmail, info);
                    }
                }

                // Persist only when status changed or task is newly inserted.
                if (previousStatus != currentStatus || task.getId() == null) {
                    if (task.getId() != null) {
                        log.info("[Scheduler] State changed: {} -> {} for {}", previousStatus, currentStatus, sectionId);
                    }
                    task.setLastStatus(currentStatus);
                    taskRepository.save(task);
                    fileRepository.save(info);
                }
            }
        } catch (Exception e) {
            log.error("[Scheduler] Error processing user {} course {}", userId, courseId, e);
        }
    }

    /**
     * Calculates whether a status transition should trigger an email alert.
     *
     * @param prev previous status from DB, or null for new sections
     * @param curr current status from crawler
     * @return alert action to execute
     */
    private AlertAction determineAction(StatusMapping prev, StatusMapping curr) {
        if (prev == null) {
            // Logic for newly discovered tasks (prevent spam on restart)
            // return AlertAction.NONE; // Uncomment to silent new task alerts
            if (curr == StatusMapping.OPEN) return AlertAction.SEND_OPEN_EMAIL;
            return AlertAction.NONE;
        }

        if (prev == curr) return AlertAction.NONE;

        switch (curr) {
            case OPEN: return AlertAction.SEND_OPEN_EMAIL;
            case WAITLISTED:
                // Only alert if upgraded from CLOSED. Downgrade from OPEN is ignored.
                return (prev == StatusMapping.CLOSED) ? AlertAction.SEND_WAITLIST_EMAIL : AlertAction.NONE;
            default: return AlertAction.NONE;
        }
    }

    /**
     * Executes email side-effects for the selected alert action.
     *
     * @param action transition action
     * @param recipientEmail recipient email
     * @param info section data used to format email content
     */
    private void dispatchMail(AlertAction action, String recipientEmail, SectionInfo info) {
        // Future RabbitMQ split: scheduler publishes alert events, dedicated consumer handles email delivery/retry.
        if (action == AlertAction.SEND_OPEN_EMAIL) {
            log.info("[Scheduler] ALERT OPEN detected for {}", info.getSection());
            mailService.sendCourseOpenAlert(recipientEmail, info.getSection(), info.getSubject() + " " + info.getCatalogNumber());
        } else if (action == AlertAction.SEND_WAITLIST_EMAIL) {
            log.info("[Scheduler] ALERT WAITLIST detected for {}", info.getSection());
            mailService.sendCourseWaitlistedAlert(recipientEmail, info.getSection(), info.getSubject() + " " + info.getCatalogNumber());
        }
    }
}
