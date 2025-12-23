package com.jing.monitor.repository;

import com.jing.monitor.model.SectionInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileRepository {

    // File path: stored in the 'logs' directory under the project root
    private static final String FILE_PATH = "logs/history.csv";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FileRepository() {
        // Initialization: Create file and write header if it doesn't exist
        initFile();
    }

    private void initFile() {
        File file = new File(FILE_PATH);

        // Ensure the parent directory (logs/) exists
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            // Use BufferedWriter for efficient IO operations
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Write CSV Header
                writer.write("Timestamp,Subject,CatalogNumber,Section,Status,CourseId");
                writer.newLine();
                System.out.println("[Repo] Created new history file: " + FILE_PATH);
            } catch (IOException e) {
                System.err.println("[Repo] Failed to init file: " + e.getMessage());
            }
        }
    }

    /**
     * Appends the section info to the CSV file.
     * @param info The section info object to save.
     */
    public void save(SectionInfo info) {
        // 'true' in FileWriter constructor enables Append Mode (do not overwrite file)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {

            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

            // Format string as CSV: "Value,Value,Value..."
            // %s is the placeholder for string data
            String line = String.format("%s,%s,%s,%s,%s,%s",
                    timestamp,
                    info.getSubject(),
                    info.getCatalogNumber(),
                    info.getSection(),
                    info.getStatus(),
                    "004289" // TODO: Retrieve this from info object in future versions
            );

            writer.write(line);
            writer.newLine(); // Add line break for the next record

        } catch (IOException e) {
            System.err.println("[Repo] Error writing to file: " + e.getMessage());
        }
    }
}