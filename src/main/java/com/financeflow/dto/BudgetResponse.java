package com.financeflow.dto;

import java.math.BigDecimal;

public record BudgetResponse(Long id, String category, BigDecimal amount, Integer month, Integer year,
                             BigDecimal amountSpent, BigDecimal remainingAmount,
                             BigDecimal percentageUsed, String status) {
}
