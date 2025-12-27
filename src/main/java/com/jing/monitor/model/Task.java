package com.jing.monitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a monitoring task.
 * Maps to the "tasks" table in the MySQL database.
 */
@Entity
@Table(name = "tasks")
@Data // Lombok: Generates Getters, Setters, toString, etc.
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The 5-digit unique section ID (e.g., "60035").
     * Defined as unique to prevent duplicate monitoring tasks for the same section.
     */
    @Column(nullable = false, unique = true)
    private String sectionId;

    /**
     * The internal course ID used by UW API (e.g., "004289").
     */
    private String courseId;

    /**
     * Human-readable name (e.g., "COMP SCI 577").
     */
    private String courseDisplayName;

    /**
     * Flag to enable or disable monitoring for this task.
     */
    private boolean enabled = true;

    /**
     * The last recorded status of the section.
     * Used for state persistence and debounce logic.
     */
    @Enumerated(EnumType.STRING) // Best practice: Store Enum as String in DB for readability
    private StatusMapping lastStatus;

    public Task(String subject, String catalogNumber, String sectionId, String courseId, StatusMapping lastStatus) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.courseDisplayName = subject + " " + catalogNumber;
        this.enabled = true;
        this.lastStatus = lastStatus;
    }
}