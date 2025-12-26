package com.jing.monitor.model;

import jakarta.persistence.*; // Spring Boot 3 使用 jakarta，旧版用 javax
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tasks")
@Data // Lombok 自动生成 Getter/Setter/ToString
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 核心配置 (对应你的 section)
    // 这里的 @Column(unique = true) 意味着同一门课不能被重复添加两次
    @Column(nullable = false, unique = true)
    private String sectionId;

    // 2. API 参数 (对应你的 courseId)
    private String courseId;

    // 3. 展示信息 (对应 subject + catalogNumber，例如 "COMP SCI 577")
    private String courseDisplayName;

    private boolean enabled = true;

    public Task(String subject, String catalogNumber, String sectionId, String courseId) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.enabled = true;
        courseDisplayName = subject + " " + catalogNumber;
    }

    // 4. 用户相关 (暂留，V0.3 先不加，默认所有任务都归属系统)
    // private Long userId;
}