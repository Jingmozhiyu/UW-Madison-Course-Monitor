package com.jing.monitor.model.dto;

import lombok.Data;

/**
 * Request DTO used when creating or upserting task records.
 */
@Data
public class TaskReqDto {
    /**
     * Section identifier within a course.
     */
    private String sectionId;

    private String courseDisplayName;
    private String courseId;
    private Boolean enabled;
}
