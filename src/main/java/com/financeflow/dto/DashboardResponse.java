package com.financeflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(Integer month, Integer year, BigDecimal totalAccountBalance,
                                BigDecimal currentMonthIncome, BigDecimal currentMonthExpenses,
                                BigDecimal currentMonthSavings, BigDecimal savingsRate,
                                List<BudgetResponse> budgetSummary,
                                List<RecurringExpenseResponse> upcomingPayments,
                                List<SavingsGoalResponse> savingsGoals,
                                List<TransactionResponse> recentTransactions,
                                List<CategoryExpenseResponse> expenseByCategory) {
}
