package com.jing.monitor.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a monitoring task.
 * Maps to the "tasks" table in the MySQL database.
 */
@Entity
@Table(
        name = "tasks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tasks_user_section", columnNames = {"user_id", "section_id"})
        }
)
@Data // Lombok: Generates Getters, Setters, toString, etc.
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner user id.
     * Each task belongs to one user account.
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * The 5-digit unique section ID (e.g., "60035").
     * Uniqueness is enforced together with {@code user_id}.
     */
    @Column(nullable = false)
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

    /**
     * Convenience constructor used by scheduler auto-discovery.
     *
     * @param subject subject short name
     * @param catalogNumber catalog number
     * @param sectionId section id
     * @param courseId course id
     * @param lastStatus current status
     */
    public Task(String subject, String catalogNumber, String sectionId, String courseId, StatusMapping lastStatus) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.courseDisplayName = subject + " " + catalogNumber;
        this.enabled = false;
        this.lastStatus = lastStatus;
    }
}
