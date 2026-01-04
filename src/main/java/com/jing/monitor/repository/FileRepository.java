package com.jing.monitor.repository;

import com.jing.monitor.model.SectionInfo;
import org.springframework.stereotype.Repository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class FileRepository {

    private static final String FILE_PATH = "logs/history.csv";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileRepository() {
        return;
    }

    private void initFile() {
        File file = new File(FILE_PATH);
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Timestamp,Subject,CatalogNumber,Section,Status,CourseId");
                writer.newLine();
                System.out.println("[Repo] Created new history file: " + FILE_PATH);
            } catch (IOException e) {
                System.err.println("[Repo] Failed to init file: " + e.getMessage());
            }
        }
    }

    public void save(SectionInfo info) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String line = String.format("%s,%s,%s,%s,%s,%s",
                    timestamp,
                    info.getSubject(),
                    info.getCatalogNumber(),
                    info.getSection(),
                    info.getStatus(),
                    info.getCourseId()
            );
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Repo] Error writing to file: " + e.getMessage());
        }
    }
}