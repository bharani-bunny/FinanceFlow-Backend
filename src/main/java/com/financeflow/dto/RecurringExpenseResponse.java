package com.financeflow.dto;

import com.financeflow.entity.RecurringExpense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseResponse(Long id, String name, BigDecimal amount, String category,
                                       RecurringExpense.Frequency frequency, LocalDate nextPaymentDate) {
}
