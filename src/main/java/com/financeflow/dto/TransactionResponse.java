package com.financeflow.dto;

import com.financeflow.entity.Transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(Long id, BigDecimal amount, TransactionType type, String category,
                                  String description, LocalDate date,
                                  Long accountId, String accountName) {
}
