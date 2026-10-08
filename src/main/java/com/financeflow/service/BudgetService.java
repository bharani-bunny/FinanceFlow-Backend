package com.financeflow.service;

import com.financeflow.dto.BudgetRequest;
import com.financeflow.dto.BudgetResponse;
import com.financeflow.entity.Budget;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.repository.BudgetRepository;
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
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository,
                        UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(String email) {
        User user = getUser(email);
        return budgetRepository.findAllByUser_IdOrderByYearDescMonthDescCategoryAsc(user.getId()).stream()
                .map(budget -> toResponse(budget, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForPeriod(String email, int month, int year) {
        User user = getUser(email);
        return budgetRepository.findAllByUser_IdAndMonthAndYearOrderByCategoryAsc(user.getId(), month, year).stream()
                .map(budget -> toResponse(budget, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(String email, Long id) {
        User user = getUser(email);
        return toResponse(findBudget(id, user.getId()), user.getId());
    }

    @Transactional
    public BudgetResponse createBudget(String email, BudgetRequest request) {
        User user = getUser(email);
        ensureUnique(user.getId(), request, null);
        Budget budget = new Budget();
        apply(budget, request, user);
        return toResponse(budgetRepository.save(budget), user.getId());
    }

    @Transactional
    public BudgetResponse updateBudget(String email, Long id, BudgetRequest request) {
        User user = getUser(email);
        Budget budget = findBudget(id, user.getId());
        ensureUnique(user.getId(), request, id);
        apply(budget, request, user);
        return toResponse(budgetRepository.save(budget), user.getId());
    }

    @Transactional
    public void deleteBudget(String email, Long id) {
        User user = getUser(email);
        budgetRepository.delete(findBudget(id, user.getId()));
    }

    @Transactional(readOnly = true)
    public BudgetResponse getUsage(String email, Long id) {
        User user = getUser(email);
        return toResponse(findBudget(id, user.getId()), user.getId());
    }

    private Budget findBudget(Long id, Long userId) {
        return budgetRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private void ensureUnique(Long userId, BudgetRequest request, Long currentId) {
        boolean exists = currentId == null
                ? budgetRepository.existsByUser_IdAndCategoryAndMonthAndYear(
                        userId, request.category(), request.month(), request.year())
                : budgetRepository.existsByUser_IdAndCategoryAndMonthAndYearAndIdNot(
                        userId, request.category(), request.month(), request.year(), currentId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A budget already exists for this category and month");
        }
    }

    private static void apply(Budget budget, BudgetRequest request, User user) {
        budget.setCategory(request.category());
        budget.setAmount(request.amount());
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setUser(user);
    }

    private BudgetResponse toResponse(Budget budget, Long userId) {
        YearMonth period = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.plusMonths(1).atDay(1);
        BigDecimal spent = transactionRepository.sumByUserAndTypeAndCategoryAndDateRange(
                userId, Transaction.TransactionType.EXPENSE, budget.getCategory(), startDate, endDate);
        if (spent == null) spent = BigDecimal.ZERO;

        BigDecimal percentage = spent.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
        String status = percentage.compareTo(BigDecimal.valueOf(100)) >= 0
                ? "Budget exceeded."
                : percentage.compareTo(BigDecimal.valueOf(80)) >= 0
                        ? "Budget is almost exhausted."
                        : "On track.";

        return new BudgetResponse(budget.getId(), budget.getCategory(), budget.getAmount(),
                budget.getMonth(), budget.getYear(), spent,
                budget.getAmount().subtract(spent), percentage, status);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"));
    }
}
