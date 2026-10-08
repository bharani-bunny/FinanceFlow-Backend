package com.financeflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank @Pattern(regexp = "Food|Transportation|Education|Shopping|Entertainment|Bills|Health|Salary|Other",
                message = "Category is not supported")
        String category,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(2000) @Max(9999) Integer year) {
}
