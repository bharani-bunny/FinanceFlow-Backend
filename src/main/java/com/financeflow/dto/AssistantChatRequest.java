package com.financeflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssistantChatRequest(
        @NotBlank @Size(max = 1200) String question,
        @Size(max = 8) List<@Valid ChatTurn> history) {
    public record ChatTurn(@NotBlank @Size(max = 1200) String role,
                           @NotBlank @Size(max = 4000) String text) { }
}
