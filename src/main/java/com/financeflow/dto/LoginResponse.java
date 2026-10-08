package com.financeflow.dto;

import com.financeflow.entity.Role;

public record LoginResponse(Long id, String name, String email, Role role, String token) {
}
