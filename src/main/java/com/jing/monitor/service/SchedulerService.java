package com.jing.monitor.service;

import com.jing.monitor.config.AppConfig;
import com.jing.monitor.core.CourseCrawler;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import com.jing.monitor.repository.FileRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulerService {

    // 1. Create a scheduled single thread pool
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final CourseCrawler crawler;
    private final FileRepository repository;

    public SchedulerService() {
        this.crawler = new CourseCrawler();
        this.repository = new FileRepository();
    }

    /**
     * Monitoring starts
     * @param section Targeted Section ID (e.g. 76102)
     */
    public void startMonitoring(String section) {
        System.out.println("[Scheduler] Starting monitor for section: " + section);

        // 2. Define task
        Runnable task = () -> {
            try {
                // Call the crawler
                SectionInfo info = crawler.fetchCourseStatus(section);

                // log output
                if (info != null) {
                    System.out.println("[Time: " + java.time.LocalTime.now() + "] " + info);

                    repository.save(info);

                    // 3. Notify if section is open
                    if (info.getStatus() == StatusMapping.OPEN) {
                        System.out.println("\n\n🔥🚨🔥 ALERT: SECTION " + section + " IS OPEN! GO ENROLL NOW! 🔥🚨🔥\n");
                        //TODO: MailService
                    }
                    else if (info.getStatus() == StatusMapping.WAITLISTED) {
                        System.out.println("\n\n🔥🚨🔥 ALERT: SECTION " + section + " HAS WAITLIST SEATS! GO ENROLL NOW! 🔥🚨🔥\n");
                    }
                } else {
                    System.out.println("[Warning] Fetch returned null. Target section not found in this package group.");
                }

            } catch (Exception e) {
                System.err.println("Error in scheduled task: " + e.getMessage());
                e.printStackTrace();
            }
        };

        // 4. Submit task
        int interval = AppConfig.getInt("app.poll-interval-seconds");
        scheduler.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        System.out.println("[Scheduler] Stopping...");
        scheduler.shutdown();
    }
}