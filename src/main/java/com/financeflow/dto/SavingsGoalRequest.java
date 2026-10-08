package com.financeflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin(value = "0.01", message = "Target amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Target amount must have at most 2 decimal places")
        BigDecimal targetAmount,
        @NotNull @DecimalMin(value = "0.00", message = "Current amount cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Current amount must have at most 2 decimal places")
        BigDecimal currentAmount,
        LocalDate targetDate) {
}
