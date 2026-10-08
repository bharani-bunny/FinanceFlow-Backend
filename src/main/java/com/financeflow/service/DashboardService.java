package com.financeflow.service;

import com.financeflow.dto.CategoryExpenseResponse;
import com.financeflow.dto.DashboardResponse;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final RecurringExpenseService recurringExpenseService;
    private final SavingsGoalService savingsGoalService;
    private final TransactionService transactionService;

    public DashboardService(UserRepository userRepository, AccountRepository accountRepository,
                            TransactionRepository transactionRepository, BudgetService budgetService,
                            RecurringExpenseService recurringExpenseService, SavingsGoalService savingsGoalService,
                            TransactionService transactionService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.recurringExpenseService = recurringExpenseService;
        this.savingsGoalService = savingsGoalService;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"));
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.plusMonths(1).atDay(1);

        BigDecimal totalBalance = zeroIfNull(accountRepository.sumBalancesByUserId(user.getId()));
        BigDecimal income = zeroIfNull(transactionRepository.sumByUserAndTypeAndDateRange(
                user.getId(), Transaction.TransactionType.INCOME, startDate, endDate));
        BigDecimal expenses = zeroIfNull(transactionRepository.sumByUserAndTypeAndDateRange(
                user.getId(), Transaction.TransactionType.EXPENSE, startDate, endDate));
        BigDecimal savings = income.subtract(expenses);
        BigDecimal savingsRate = income.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : savings.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP);
        var expenseByCategory = transactionRepository.sumByCategoryAndDateRange(
                        user.getId(), Transaction.TransactionType.EXPENSE, startDate, endDate)
                .stream().map(item -> new CategoryExpenseResponse(item.getCategory(), item.getAmount())).toList();

        return new DashboardResponse(currentMonth.getMonthValue(), currentMonth.getYear(), totalBalance,
                income, expenses, savings, savingsRate,
                budgetService.getBudgetsForPeriod(email, currentMonth.getMonthValue(), currentMonth.getYear()),
                recurringExpenseService.getUpcoming(email), savingsGoalService.getAll(email),
                transactionService.getRecent(email), expenseByCategory);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
