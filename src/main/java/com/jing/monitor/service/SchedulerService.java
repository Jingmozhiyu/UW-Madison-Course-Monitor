package com.jing.monitor.service;

import com.jing.monitor.core.CourseCrawler;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import com.jing.monitor.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service // <--- 1. 声明这是业务逻辑层
public class SchedulerService {

    private final CourseCrawler crawler;
    private final FileRepository repository;

    // 从配置文件读取目标 Section
    @Value("${uw-api.target-section}")
    private String targetSection;

    // 2. 构造函数注入 (Constructor Injection)
    // Spring 会自动找到 crawler 和 repository 的实例并传进来
    public SchedulerService(CourseCrawler crawler, FileRepository repository) {
        this.crawler = crawler;
        this.repository = repository;
    }

    /**
     * 定时任务
     * fixedRateString: 从配置文件读取毫秒数 (monitor.poll-interval-ms)
     */
    @Scheduled(fixedRateString = "${monitor.poll-interval-ms}")
    public void monitorTask() {
        // System.out.println("[Scheduler] Heartbeat..."); // 调试用，确认定时器在跑

        try {
            // 调用爬虫
            SectionInfo info = crawler.fetchCourseStatus(targetSection);

            if (info != null) {
                // 打印日志
                System.out.println("[Time: " + LocalTime.now() + "] " + info);

                // 保存历史
                repository.save(info);

                // 简单的逻辑判断 (V0.3 我们会把这里升级为 NotificationService)
                if (info.getStatus() == StatusMapping.OPEN) {
                    System.out.println("\n\n🔥🚨🔥 ALERT: SECTION " + targetSection + " IS OPEN! 🔥🚨🔥\n");
                } else if (info.getStatus() == StatusMapping.WAITLISTED) {
                    System.out.println("\n\n🔥🚨🔥 ALERT: WAITLIST OPEN FOR " + targetSection + "! 🔥🚨🔥\n");
                }
            } else {
                System.out.println("[Warning] Fetch returned null. Target section not found.");
            }

        } catch (Exception e) {
            System.err.println("Error in scheduled task: " + e.getMessage());
            e.printStackTrace();
        }
    }
}