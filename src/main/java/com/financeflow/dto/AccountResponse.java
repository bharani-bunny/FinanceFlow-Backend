package com.financeflow.dto;

import com.financeflow.entity.Account.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(Long id, String name, AccountType type, BigDecimal balance, LocalDateTime createdAt) {
}
