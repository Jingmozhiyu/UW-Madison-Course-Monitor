package com.jing.monitor.security;

/**
 * Lightweight authenticated principal stored in Spring Security context.
 *
 * @param id user id
 * @param email user email
 */
public record AuthenticatedUser(Long id, String email) {
}
