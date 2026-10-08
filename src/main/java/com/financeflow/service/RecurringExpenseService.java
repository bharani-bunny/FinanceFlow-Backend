package com.financeflow.service;

import com.financeflow.dto.RecurringExpenseRequest;
import com.financeflow.dto.RecurringExpenseResponse;
import com.financeflow.entity.RecurringExpense;
import com.financeflow.entity.User;
import com.financeflow.repository.RecurringExpenseRepository;
import com.financeflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringExpenseService {
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final UserRepository userRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringExpenseRepository,
                                   UserRepository userRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getAll(String email) {
        User user = getUser(email);
        return recurringExpenseRepository.findAllByUser_IdOrderByNextPaymentDateAscIdAsc(user.getId()).stream()
                .map(RecurringExpenseService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getUpcoming(String email) {
        User user = getUser(email);
        return recurringExpenseRepository
                .findAllByUser_IdAndNextPaymentDateGreaterThanEqualOrderByNextPaymentDateAscIdAsc(
                        user.getId(), LocalDate.now())
                .stream().map(RecurringExpenseService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecurringExpenseResponse getById(String email, Long id) {
        User user = getUser(email);
        return toResponse(findExpense(id, user.getId()));
    }

    @Transactional
    public RecurringExpenseResponse create(String email, RecurringExpenseRequest request) {
        User user = getUser(email);
        RecurringExpense expense = new RecurringExpense();
        apply(expense, request, user);
        return toResponse(recurringExpenseRepository.save(expense));
    }

    @Transactional
    public RecurringExpenseResponse update(String email, Long id, RecurringExpenseRequest request) {
        User user = getUser(email);
        RecurringExpense expense = findExpense(id, user.getId());
        apply(expense, request, user);
        return toResponse(recurringExpenseRepository.save(expense));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = getUser(email);
        recurringExpenseRepository.delete(findExpense(id, user.getId()));
    }

    private RecurringExpense findExpense(Long id, Long userId) {
        return recurringExpenseRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Recurring expense not found"));
    }

    private static void apply(RecurringExpense expense, RecurringExpenseRequest request, User user) {
        expense.setName(request.name().trim());
        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setFrequency(request.frequency());
        expense.setNextPaymentDate(request.nextPaymentDate());
        expense.setUser(user);
    }

    private static RecurringExpenseResponse toResponse(RecurringExpense expense) {
        return new RecurringExpenseResponse(expense.getId(), expense.getName(), expense.getAmount(),
                expense.getCategory(), expense.getFrequency(), expense.getNextPaymentDate());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"));
    }
}
