package com.jing.monitor.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jing.monitor.config.AppConfig;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class CourseCrawler {

    private String buildApiUrl() {
        String term = AppConfig.get("app.term-id");
        String subject = AppConfig.get("app.subject-id");
        String course = AppConfig.get("app.course-id");

        // 动态拼接: https://public.enroll.wisc.edu/api/search/v1/enrollmentPackages/{term}/{subject}/{course}
        return String.format("https://public.enroll.wisc.edu/api/search/v1/enrollmentPackages/%s/%s/%s",
                term, subject, course);
    }

    public SectionInfo fetchCourseStatus(String section) {
        System.out.println("[Crawler] Requesting UW API...");

        try {
            // 1. Fetch JSON
            String url = buildApiUrl();
            String jsonResponse = Jsoup.connect(url )
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .execute()
                    .body();

            // 2. Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);

            // 3. Get infos of targeted section
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {

                    if (node.path("enrollmentClassNumber").asText().equals(section)) {
                        String subject = node.path("sections").path(0)
                                .path("subject").path("shortDescription")
                                .asText();

                        String catalogNumber = node.path("catalogNumber").asText();

                        String statusStr = node.path("packageEnrollmentStatus")
                                .path("status")
                                .asText();

                        // "OPEN", "WAITLISTED", "CLOSED"
                        StatusMapping status;
                        try {
                            status = StatusMapping.valueOf(statusStr);
                        } catch (IllegalArgumentException | NullPointerException e) {
                            System.err.println("Unknown status: " + statusStr);
                            status = StatusMapping.CLOSED;
                        }

                        String courseId = node.path("courseId").asText();

                        return new SectionInfo(subject,catalogNumber,section,status,courseId);
                    }
                }
            }
            return null;
        } catch (IOException e) {
            System.err.println("Network Error: " + e.getMessage());
            return new SectionInfo(); // Return closed on error
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        CourseCrawler crawler = new CourseCrawler();
        SectionInfo info = crawler.fetchCourseStatus("76102");
        System.out.println(info);
    }
}