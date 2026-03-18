package com.jing.monitor.model.dto;

import com.jing.monitor.model.StatusMapping;
import lombok.Data;

/**
 * Response DTO returned to task UI clients.
 */
@Data
public class TaskRespDto {
    private Long id;
    private String sectionId;
    private String courseDisplayName;
    private StatusMapping status;
    private boolean enabled;
}
