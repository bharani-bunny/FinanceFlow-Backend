package com.financeflow.dto;

import com.financeflow.entity.Role;

public record RegisterResponse(Long id, String name, String email, Role role) {
}
