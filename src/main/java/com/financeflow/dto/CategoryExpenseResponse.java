package com.financeflow.dto;

import java.math.BigDecimal;

public record CategoryExpenseResponse(String category, BigDecimal amount) {
}
