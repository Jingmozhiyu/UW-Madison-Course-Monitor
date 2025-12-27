package com.jing.monitor.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import com.jing.monitor.model.Task;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Core component responsible for fetching data from the UW-Madison Enrollment API.
 */
@Component
public class CourseCrawler {

    // Inject configuration values
    @Value("${uw-api.term-id}")
    private String termId;

    @Value("${uw-api.subject-id}")
    private String subjectId;

    // Reuse ObjectMapper instance for performance optimization
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Fetches the real-time status of a specific course section.
     *
     * @param task The monitoring task containing course and section IDs.
     * @return SectionInfo object if successful, or null if fetch fails.
     */
    public SectionInfo fetchCourseStatus(Task task) {
        try {
            // 1. Construct the API URL
            // Note: Currently relies on 'subjectId' from config. Future refactoring will move subjectId to the Task entity.
            String url = String.format("https://public.enroll.wisc.edu/api/search/v1/enrollmentPackages/%s/%s/%s",
                    termId, subjectId, task.getCourseId());

            // 2. Execute HTTP GET Request via Jsoup
            // ignoreContentType is required because the API returns JSON, not HTML
            String jsonResponse = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .execute()
                    .body();

            // 3. Parse JSON Response
            JsonNode rootNode = mapper.readTree(jsonResponse);

            // 4. Traverse the JSON array to find the specific section
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    // Match by 'enrollmentClassNumber' (which is the 5-digit section ID)
                    if (node.path("enrollmentClassNumber").asText().equals(task.getSectionId())) {

                        // Extract course details
                        String subject = node.path("sections").path(0)
                                .path("subject").path("shortDescription")
                                .asText();

                        String catalogNumber = node.path("catalogNumber").asText();

                        String statusStr = node.path("packageEnrollmentStatus")
                                .path("status")
                                .asText();

                        // Map string status to Enum safely
                        StatusMapping status;
                        try {
                            status = StatusMapping.valueOf(statusStr);
                        } catch (IllegalArgumentException | NullPointerException e) {
                            System.err.println("Unknown status encountered: " + statusStr);
                            status = StatusMapping.CLOSED; // Default fallback
                        }

                        return new SectionInfo(subject, catalogNumber, task.getSectionId(), status, task.getCourseId());
                    }
                }
            }

            // Section not found in the response
            return null;

        } catch (IOException e) {
            System.err.println("Network/Parsing Error for task " + task.getSectionId() + ": " + e.getMessage());
            return null; // Return null to indicate failure to the scheduler
        }
    }
}