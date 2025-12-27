package com.jing.monitor.model.dto;

import lombok.Data;

@Data
public class TaskReqDto {
    // 必填：这是核心
    private String sectionId;

    // 选填 (V0.4 暂时必填)：为了拼接显示名称 (e.g., "COMP SCI 577")
    // 等未来有了 Metadata 数据库，这两个可以去掉，后端自动查
    private String subject;
    private String catalogNumber;

    // 选填：用户是否想暂停监控
    private Boolean enabled;
}