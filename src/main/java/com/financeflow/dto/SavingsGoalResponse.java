package com.financeflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount,
                                  LocalDate targetDate, BigDecimal progressPercentage) {
}
