package com.financeflow.service;

import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightsService {
    private static final BigDecimal SIGNIFICANT_INCREASE = new BigDecimal("1.20");

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final RecurringExpenseService recurringExpenseService;

    public InsightsService(UserRepository userRepository, TransactionRepository transactionRepository,
                           BudgetService budgetService, RecurringExpenseService recurringExpenseService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.recurringExpenseService = recurringExpenseService;
    }

    @Transactional(readOnly = true)
    public List<String> getInsights(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"));
        YearMonth currentPeriod = YearMonth.now();
        YearMonth previousPeriod = currentPeriod.minusMonths(1);

        BigDecimal currentIncome = sum(user.getId(), Transaction.TransactionType.INCOME, currentPeriod);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.TransactionType.EXPENSE, currentPeriod);
        BigDecimal previousIncome = sum(user.getId(), Transaction.TransactionType.INCOME, previousPeriod);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.TransactionType.EXPENSE, previousPeriod);

        List<String> insights = new ArrayList<>();

        if (currentExpenses.compareTo(previousExpenses) > 0) {
            insights.add("Your expenses increased compared with last month.");
        }

        Map<String, BigDecimal> previousByCategory = new HashMap<>();
        transactionRepository.sumByCategoryAndDateRange(user.getId(), Transaction.TransactionType.EXPENSE,
                        previousPeriod.atDay(1), previousPeriod.plusMonths(1).atDay(1))
                .forEach(item -> previousByCategory.put(item.getCategory(), item.getAmount()));
        transactionRepository.sumByCategoryAndDateRange(user.getId(), Transaction.TransactionType.EXPENSE,
                        currentPeriod.atDay(1), currentPeriod.plusMonths(1).atDay(1))
                .forEach(item -> {
                    BigDecimal previous = previousByCategory.get(item.getCategory());
                    if (previous != null && previous.signum() > 0
                            && item.getAmount().compareTo(previous.multiply(SIGNIFICANT_INCREASE)) >= 0) {
                        insights.add("Your spending on " + item.getCategory() + " increased this month.");
                    }
                });

        budgetService.getBudgetsForPeriod(email, currentPeriod.getMonthValue(), currentPeriod.getYear())
                .forEach(budget -> {
                    if (budget.percentageUsed().compareTo(BigDecimal.valueOf(100)) >= 0) {
                        insights.add("You have exceeded your " + budget.category() + " budget.");
                    } else if (budget.percentageUsed().compareTo(BigDecimal.valueOf(80)) >= 0) {
                        insights.add("Your " + budget.category() + " budget is almost exhausted.");
                    }
                });

        if (currentExpenses.compareTo(currentIncome) > 0) {
            insights.add("Your expenses are higher than your income this month.");
        }

        if (!recurringExpenseService.getUpcoming(email).isEmpty()) {
            insights.add("You have an upcoming recurring payment.");
        }

        BigDecimal currentRate = savingsRate(currentIncome, currentExpenses);
        BigDecimal previousRate = savingsRate(previousIncome, previousExpenses);
        if (currentRate.compareTo(previousRate) > 0) {
            insights.add("Your savings rate improved this month.");
        }

        return List.copyOf(insights);
    }

    private BigDecimal sum(Long userId, Transaction.TransactionType type, YearMonth period) {
        BigDecimal value = transactionRepository.sumByUserAndTypeAndDateRange(userId, type,
                period.atDay(1), period.plusMonths(1).atDay(1));
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal savingsRate(BigDecimal income, BigDecimal expenses) {
        if (income.signum() == 0) return BigDecimal.ZERO;
        return income.subtract(expenses).multiply(BigDecimal.valueOf(100))
                .divide(income, 2, RoundingMode.HALF_UP);
    }
}
