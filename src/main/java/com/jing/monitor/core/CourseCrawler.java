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
import java.util.HashMap;
import java.util.Map;

/**
 * Core component responsible for fetching data from the UW-Madison Enrollment API.
 * Features: Cookie persistence, User-Agent spoofing, and Exponential Backoff for HTTP 202.
 */
@Component
public class CourseCrawler {

    @Value("${uw-api.term-id}")
    private String termId;

    @Value("${uw-api.subject-id}")
    private String subjectId;

    @Value("${uw-api.user-agent}")
    private String userAgent;

    private final ObjectMapper mapper = new ObjectMapper();

    // Store SessionID to mimic a real browser session
    private Map<String, String> cookies = new HashMap<>();

    private static final int MAX_RETRIES = 3;

    private boolean isSessionInitialized = false;

    // 🔥 新增方法：先去主页逛一圈，骗取 Cookie
    private void initializeSession() {
        if (isSessionInitialized && !cookies.isEmpty()) return;

        try {
            System.out.println("🍪 [Init] Visiting Home Page to grab cookies...");
            Connection.Response response = Jsoup.connect("https://public.enroll.wisc.edu/") // 访问主页
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8") // 浏览器标准的 Accept
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(10000)
                    .method(Connection.Method.GET)
                    .execute();

            // 保存主页给我们的 Cookie
            cookies.putAll(response.cookies());
            isSessionInitialized = true;
            System.out.println("✅ [Init] Session Initialized. Cookies count: " + cookies.size());

            // 稍微睡一下，模拟人类打开网页后的反应时间
            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println("⚠️ Initialization failed: " + e.getMessage());
            // 就算失败了也不要 throw，硬着头皮试一下 API
        }
    }

    /**
     * Fetches the real-time status of a specific course section with retry logic.
     *
     * @param task The monitoring task containing course and section IDs.
     * @return SectionInfo object if successful, or null if fetch fails after retries.
     */
    public SectionInfo fetchCourseStatus(Task task) {

        if (!isSessionInitialized) {
            initializeSession();
        }

        String url = String.format("https://public.enroll.wisc.edu/api/search/v1/enrollmentPackages/%s/%s/%s",
                termId, subjectId, task.getCourseId());

        // Retry Loop for robustness (Handles HTTP 202 and Network Flukes)
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // 1. Anti-Bot: Random Sleep (1-3s) before every request
                try {
                    long randomSleep = 1000 + (long)(Math.random() * 2000);
                    Thread.sleep(randomSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("🔍 [Crawler] Fetching: " + url + " (Attempt " + attempt + "/" + MAX_RETRIES + ")");

                // 2. Build Connection
                Connection conn = Jsoup.connect(url)
                        .ignoreContentType(true)
                        .header("User-Agent", userAgent)
                        .header("Referer", "https://public.enroll.wisc.edu/")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Connection", "keep-alive")
                        .method(Connection.Method.GET)
                        .timeout(15000) // Increased timeout to 15s
                        .ignoreHttpErrors(true); // Vital: allows us to handle 202/404 manually

                // Apply cookies if we have them
                if (!cookies.isEmpty()) {
                    conn.cookies(cookies);
                }

                // 3. Execute Request
                Connection.Response response = conn.execute();

                // Update cookies (Session maintenance)
                cookies.putAll(response.cookies());

                int statusCode = response.statusCode();

                // 🔥 CASE: HTTP 202 (Accepted) - Server is busy/preparing data
                if (statusCode == 202) {
                    System.out.println("⏳ Server returned 202 (Accepted). Waiting 5s to retry...");
                    try { Thread.sleep(5000); } catch (InterruptedException e) {}
                    continue; // Jump to next loop iteration (retry)
                }

                // 🔥 CASE: HTTP 200 (OK) - Success
                if (statusCode == 200) {
                    String jsonBody = response.body();
                    JsonNode rootNode = mapper.readTree(jsonBody);

                    if (rootNode.isArray()) {
                        for (JsonNode node : rootNode) {
                            if (node.path("enrollmentClassNumber").asText().equals(task.getSectionId())) {
                                // Extract Data
                                String subject = node.path("sections").path(0)
                                        .path("subject").path("shortDescription").asText();
                                String catalogNumber = node.path("catalogNumber").asText();
                                String statusStr = node.path("packageEnrollmentStatus").path("status").asText();

                                StatusMapping status;
                                try {
                                    status = StatusMapping.valueOf(statusStr);
                                } catch (Exception e) {
                                    System.err.println("Unknown status: " + statusStr);
                                    status = StatusMapping.CLOSED;
                                }

                                // Success! Return immediately.
                                return new SectionInfo(subject, catalogNumber, task.getSectionId(), status, task.getCourseId());
                            }
                        }
                    }
                    // If we got 200 but didn't find the section, it might be a data mismatch.
                    // No point retrying usually, unless data is eventually consistent.
                    System.err.println("⚠️ Section " + task.getSectionId() + " not found in valid response.");
                    return null;
                }

                // 🔥 CASE: Other Errors (403, 404, 500)
                System.err.println("❌ API Error: " + statusCode + " | Body: " + response.body());
                // Depending on strategy, we could retry 5xx, but usually fail fast for 4xx
                return null;

            } catch (IOException e) {
                System.err.println("⚠️ Network Error (Attempt " + attempt + "): " + e.getMessage());
                // Loop will automatically retry
            }
        }

        System.err.println("❌ Max retries reached. Fetch failed for " + task.getCourseDisplayName());
        return null;
    }
}