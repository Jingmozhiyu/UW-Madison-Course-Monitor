package com.jing.monitor.model.dto;

import com.jing.monitor.model.StatusMapping;
import lombok.Data;

@Data
public class TaskRespDto {
    private Long id;              // 用于前端修改/删除的索引
    private String sectionId;
    private String courseDisplayName; // "COMP SCI 577"
    private StatusMapping status;     // 前端变色用
    private boolean enabled;
}