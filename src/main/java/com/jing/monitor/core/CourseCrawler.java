package com.jing.monitor.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component // <--- 1. 变成 Spring Bean
public class CourseCrawler {

    // 2. 使用 @Value 注入配置 (替代 AppConfig)
    @Value("${uw-api.term-id}")
    private String termId;

    @Value("${uw-api.subject-id}")
    private String subjectId;

    @Value("${uw-api.course-id}")
    private String courseId;

    // 3. 将 ObjectMapper 提升为成员变量 (性能优化，不必每次 new)
    private final ObjectMapper mapper = new ObjectMapper();

    private String buildApiUrl() {
        // 动态拼接
        return String.format("https://public.enroll.wisc.edu/api/search/v1/enrollmentPackages/%s/%s/%s",
                termId, subjectId, courseId);
    }

    public SectionInfo fetchCourseStatus(String section) {
        // System.out.println("[Crawler] Requesting UW API..."); // 暂时注释掉，减少刷屏

        try {
            // 1. Fetch JSON
            String url = buildApiUrl();
            String jsonResponse = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    // .userAgent("...") // TODO: 后续我们需要从配置里读取 User-Agent
                    .execute()
                    .body();

            // 2. Parse JSON
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

                        StatusMapping status;
                        try {
                            status = StatusMapping.valueOf(statusStr);
                        } catch (IllegalArgumentException | NullPointerException e) {
                            System.err.println("Unknown status: " + statusStr);
                            status = StatusMapping.CLOSED;
                        }

                        // 注意：这里 courseId 我们直接用成员变量即可，或者从 JSON 读也行
                        return new SectionInfo(subject, catalogNumber, section, status, this.courseId);
                    }
                }
            }
            return null;
        } catch (IOException e) {
            System.err.println("Network Error: " + e.getMessage());
            return new SectionInfo();
        }
    }
}