package com.jing.monitor.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jing.monitor.model.SectionInfo;
import com.jing.monitor.model.StatusMapping;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core component responsible for fetching data from the UW-Madison Enrollment API.
 * * Strategy:
 * 1. Uses Jsoup for lightweight HTTP requests.
 * 2. Fetches at the COURSE level (api/search/v1/enrollmentPackages/{term}/{subject}/{courseId}).
 * 3. Returns a list of all sections to reduce API call frequency.
 */
@Component
@Slf4j
public class CourseCrawler {

    @Value("${uw-api.term-id}")
    private String termId;

    @Value("${uw-api.subject-id}")
    private String subjectId;

    @Value("${uw-api.user-agent}")
    private String userAgent;

    private final ObjectMapper mapper = new ObjectMapper();

    // Persistent cookie store. Note: Will be empty on server restart.
    // Consider adding a method to inject 'aws-waf-token' externally if WAF blocks the first request.
    private Map<String, String> cookies = new HashMap<>();

    /**
     * Fetches the real-time status of ALL sections for a specific course ID.
     *
     * @param courseId The 6-digit course identifier (e.g. 004289).
     * @return List of SectionInfo objects, or null if fetch fails.
     */
    public List<SectionInfo> fetchCourseStatus(String courseId) {
        // Construct the GET endpoint for course-level details
        String url = String.format("https://public.enroll.wisc.edu/api/search/v1/enrollmentPackages/%s/%s/%s",
                termId, subjectId, courseId);

        try {
            Connection conn = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://public.enroll.wisc.edu/")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Connection", "keep-alive")
                    .method(Connection.Method.GET)
                    .timeout(15000)
                    .ignoreHttpErrors(true);

            if (!cookies.isEmpty()) {
                conn.cookies(cookies);
            }

            Connection.Response response = conn.execute();

            // Refresh cookies (Maintain session)
            cookies.putAll(response.cookies());

            int statusCode = response.statusCode();

            if (statusCode == 200) {
                String jsonBody = response.body();
                JsonNode rootNode = mapper.readTree(jsonBody);
                List<SectionInfo> sectionInfos = new ArrayList<>();

                if (rootNode.isArray()) {
                    for (JsonNode node : rootNode) {
                        // Extract Data
                        String subject = node.path("sections").path(0)
                                .path("subject").path("shortDescription").asText();
                        String catalogNumber = node.path("catalogNumber").asText();
                        String sectionId = node.path("enrollmentClassNumber").asText();
                        String statusStr = node.path("packageEnrollmentStatus").path("status").asText();

                        StatusMapping status;
                        try {
                            status = StatusMapping.valueOf(statusStr);
                        } catch (Exception e) {
                            // Fallback for unknown status strings
                            status = StatusMapping.CLOSED;
                        }

                        sectionInfos.add(new SectionInfo(subject, catalogNumber, sectionId, status, courseId));
                    }
                    return sectionInfos;
                }

                log.warn("[Crawler] API 200 but returned unexpected format for course: {}", courseId);
                return null;
            }

            // Handle WAF or Rate Limiting
            if (statusCode == 202 || statusCode == 403 || statusCode == 429) {
                log.warn("[Crawler] API status {} (blocked/rate limited). Skipping cycle.", statusCode);
                return null;
            }

            log.error("[Crawler] API error: {} | body: {}", statusCode, response.body());
            return null;

        } catch (IOException e) {
            log.error("[Crawler] Network error while fetching course {}", courseId, e);
        }

        return null;
    }

    public JsonNode searchCourse(String userQueryString) {
        String searchUrl = "https://public.enroll.wisc.edu/api/search/v1";

        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("selectedTerm", termId);
            root.put("queryString", userQueryString); // "COMP SCI 571"
            root.put("page", 1);
            root.put("pageSize", 50);
            root.put("sortOrder", "SCORE");

            ArrayNode filters = root.putArray("filters");
            ObjectNode hasChild = filters.addObject().putObject("has_child");
            hasChild.put("type", "enrollmentPackage");

            ObjectNode query = hasChild.putObject("query");
            ObjectNode bool = query.putObject("bool");
            ArrayNode must = bool.putArray("must");

            // match 1: status
            must.addObject().putObject("match")
                    .put("packageEnrollmentStatus.status", "OPEN WAITLISTED CLOSED");

            // match 2: published
            must.addObject().putObject("match")
                    .put("published", true);

            // Convert to string and ready to be sent
            String jsonPayload = mapper.writeValueAsString(root);

            // POST Request
            Connection.Response response = Jsoup.connect(searchUrl)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://public.enroll.wisc.edu/")
                    .ignoreContentType(true)
                    .timeout(10000)
                    .method(Connection.Method.POST)
                    .requestBody(jsonPayload)       // Put JSON into request body
                    .execute();

            // Handle response
            if (response.statusCode() == 200) {
                return mapper.readTree(response.body());
            } else {
                log.warn("[Crawler] Search failed with status: {}", response.statusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("[Crawler] Network error during search for query: {}", userQueryString, e);
            return null;
        }
    }

    // Helper method to manually inject cookies (e.g. from browser dev tools) if needed
    public void setCookies(Map<String, String> newCookies) {
        this.cookies.putAll(newCookies);
    }
}
