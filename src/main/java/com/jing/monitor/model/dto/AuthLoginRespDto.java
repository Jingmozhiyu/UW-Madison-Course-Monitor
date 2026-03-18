package com.jing.monitor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO returned after successful login.
 */
@Data
@AllArgsConstructor
public class AuthLoginRespDto {
    private Long userId;
    private String email;
    private String token;
}
