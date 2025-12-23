package com.jing.monitor.controller;

import com.jing.monitor.config.AppConfig;
import com.jing.monitor.service.SchedulerService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   UW-Madison Course Monitor CLI v0.1    ");
        System.out.println("=========================================");

        // Instantiate
        SchedulerService service = new SchedulerService();

        // Targeted Section
        String targetSection = AppConfig.get("app.target-section");
        service.startMonitoring(targetSection);

        service.startMonitoring(targetSection);

        // Avoid Main Thread Terminate
        // Since Scheduler is running in backend, it will stop if main thread stops.
        // Using scanner to terminate by input ENTER.
        System.out.println("Monitor is running... Press [ENTER] to stop.");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        service.stop();
        System.out.println("Program exited.");
    }
}