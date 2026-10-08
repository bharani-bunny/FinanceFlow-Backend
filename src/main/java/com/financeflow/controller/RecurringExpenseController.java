package com.financeflow.controller;

import com.financeflow.dto.RecurringExpenseRequest;
import com.financeflow.dto.RecurringExpenseResponse;
import com.financeflow.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {
    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    @GetMapping
    public List<RecurringExpenseResponse> getAll(Principal principal) {
        return recurringExpenseService.getAll(principal.getName());
    }

    @GetMapping("/upcoming")
    public List<RecurringExpenseResponse> getUpcoming(Principal principal) {
        return recurringExpenseService.getUpcoming(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringExpenseResponse create(Principal principal,
                                           @Valid @RequestBody RecurringExpenseRequest request) {
        return recurringExpenseService.create(principal.getName(), request);
    }

    @GetMapping("/{id}")
    public RecurringExpenseResponse getById(Principal principal, @PathVariable Long id) {
        return recurringExpenseService.getById(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public RecurringExpenseResponse update(Principal principal, @PathVariable Long id,
                                          @Valid @RequestBody RecurringExpenseRequest request) {
        return recurringExpenseService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable Long id) {
        recurringExpenseService.delete(principal.getName(), id);
    }
}
