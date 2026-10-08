package com.financeflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SavingsGoalProgressRequest(
        @NotNull @DecimalMin(value = "0.00", message = "Current amount cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Current amount must have at most 2 decimal places")
        BigDecimal currentAmount) {
}
