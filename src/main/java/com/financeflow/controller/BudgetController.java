package com.financeflow.controller;

import com.financeflow.dto.BudgetRequest;
import com.financeflow.dto.BudgetResponse;
import com.financeflow.service.BudgetService;
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
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> getBudgets(Principal principal) {
        return budgetService.getBudgets(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(Principal principal, @Valid @RequestBody BudgetRequest request) {
        return budgetService.createBudget(principal.getName(), request);
    }

    @GetMapping("/{id}")
    public BudgetResponse getBudget(Principal principal, @PathVariable Long id) {
        return budgetService.getBudget(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public BudgetResponse updateBudget(Principal principal, @PathVariable Long id,
                                       @Valid @RequestBody BudgetRequest request) {
        return budgetService.updateBudget(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(Principal principal, @PathVariable Long id) {
        budgetService.deleteBudget(principal.getName(), id);
    }

    @GetMapping("/{id}/usage")
    public BudgetResponse getUsage(Principal principal, @PathVariable Long id) {
        return budgetService.getUsage(principal.getName(), id);
    }
}
