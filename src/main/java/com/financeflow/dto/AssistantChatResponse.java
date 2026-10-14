package com.financeflow.dto;

import java.util.List;

public record AssistantChatResponse(String answer, List<String> sources) { }
