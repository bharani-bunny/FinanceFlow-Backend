package com.financeflow.dto;

import com.financeflow.entity.RecurringExpense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,
        @NotBlank @Pattern(regexp = "Food|Transportation|Education|Shopping|Entertainment|Bills|Health|Salary|Other",
                message = "Category is not supported")
        String category,
        @NotNull RecurringExpense.Frequency frequency,
        @NotNull LocalDate nextPaymentDate) {
}
