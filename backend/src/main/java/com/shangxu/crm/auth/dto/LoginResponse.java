package com.shangxu.crm.auth.dto;

import java.util.List;

public record LoginResponse(
        String token,
        long expiresIn,
        Long userId,
        String username,
        String nickname,
        String avatar,
        String deptName,
        List<String> roles,
        List<String> permissions
) {
}
