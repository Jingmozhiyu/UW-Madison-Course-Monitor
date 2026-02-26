package com.jing.monitor.service;

import com.jing.monitor.core.CourseCrawler;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import com.jing.monitor.model.Task;
import com.jing.monitor.repository.FileRepository;
import com.jing.monitor.repository.TaskRepository;
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
    private final Random random = new Random();

    // Define alert actions
    enum AlertAction { NONE, SEND_OPEN_EMAIL, SEND_WAITLIST_EMAIL }

    /**
     * Main Monitoring Loop.
     * Frequency should be set conservatively (e.g., 3-5 minutes) to avoid WAF blocking.
     */
    @Scheduled(fixedDelayString = "${monitor.poll-interval-ms}")
    public void monitorTask() {
        // 1. Aggregation: Fetch all tasks and deduplicate by Course ID
        List<Task> tasks = taskRepository.findByEnabledTrue();
        Set<String> courseSet = new HashSet<>();
        for(Task task : tasks){
            courseSet.add(task.getCourseId());
        }

        if (courseSet.isEmpty()) {
            log.info("[Scheduler] No active tasks. Idle.");
            return;
        }

        log.info("[Scheduler] Starting cycle. Monitoring {} unique courses.", courseSet.size());

        int cnt = courseSet.size();
        // 2. Batch Processing: Fetch data per Course (1 Request = N Sections)
        for (String courseId : courseSet) {
            processSingleCourse(courseId);
            cnt--;
            if(cnt == 0){
                break;
            }
            try {
                long sleepTime = 120000 + random.nextInt(10000);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Fetches all sections for a given course and updates local Task states.
     * @param courseId The 6-digit course identifier (e.g., "004289")
     */
    private void processSingleCourse(String courseId) {
        try {
            // Step 1: Network I/O - Fetch course data
            List<SectionInfo> infos = crawler.fetchCourseStatus(courseId);

            if (infos == null) {
                log.warn("[Scheduler] Fetch failed or blocked for course: {}", courseId);
                return;
            }

            // Step 2: DB I/O - Fetch all tasks for this course in one query and build index by sectionId.
            Map<String, Task> taskMapBySectionId = taskRepository.findAllByCourseId(courseId).stream()
                    .collect(Collectors.toMap(Task::getSectionId, Function.identity(), (left, right) -> left, HashMap::new));

            // Step 2: Synchronization - Update Database
            for (SectionInfo info : infos) {
                StatusMapping currentStatus = info.getStatus();
                StatusMapping previousStatus = null;
                String sectionId = info.getSection();

                // O(1) lookup from in-memory index instead of querying DB per section.
                Task task = taskMapBySectionId.get(sectionId);

                // Logic: Auto-Discovery vs Update
                if (task == null) {
                    // Scenario A: New Section Discovered (Auto-add to DB)
                    // Note: This will monitor ALL sections. If this is spammy, add filtering logic here.
                    task = new Task(info.getSubject(), info.getCatalogNumber(), sectionId, courseId, info.getStatus());
                    taskMapBySectionId.put(sectionId, task);
                    log.info("[Scheduler] New section found {}. Adding to DB.", info.getSection());

                    // TODO: Optional: Send alert on discovery?
                    AlertAction action = determineAction(null, currentStatus);
                    Mail(action, info);
                } else {
                    // Scenario B: Existing Task Update
                    previousStatus = task.getLastStatus();

                    // Logging state only on changes or verbose debug can go here.

                    if (task.isEnabled()) {
                        AlertAction action = determineAction(previousStatus, currentStatus);
                        Mail(action, info);
                    }
                }

                // Step 3: Persistence
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
            log.error("[Scheduler] Error processing course {}", courseId, e);
        }
    }

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

    private void Mail(AlertAction action, SectionInfo info) {
        if (action == AlertAction.SEND_OPEN_EMAIL) {
            log.info("[Scheduler] ALERT OPEN detected for {}", info.getSection());
            mailService.sendCourseOpenAlert(info.getSection(), info.getSubject() + " " + info.getCatalogNumber());
        } else if (action == AlertAction.SEND_WAITLIST_EMAIL) {
            log.info("[Scheduler] ALERT WAITLIST detected for {}", info.getSection());
            mailService.sendCourseWaitlistedAlert(info.getSection(), info.getSubject() + " " + info.getCatalogNumber());
        }
    }
}
