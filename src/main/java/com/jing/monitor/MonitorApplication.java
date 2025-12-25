package com.jing.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling //这一行非常关键！它开启了定时任务的开关
public class MonitorApplication {

    public static void main(String[] args) {
        // 这一行启动了整个 Spring 容器
        SpringApplication.run(MonitorApplication.class, args);
    }
}