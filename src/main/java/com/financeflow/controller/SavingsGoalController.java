package com.financeflow.controller;

import com.financeflow.dto.SavingsGoalProgressRequest;
import com.financeflow.dto.SavingsGoalRequest;
import com.financeflow.dto.SavingsGoalResponse;
import com.financeflow.service.SavingsGoalService;
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
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {
    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @GetMapping
    public List<SavingsGoalResponse> getAll(Principal principal) {
        return savingsGoalService.getAll(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsGoalResponse create(Principal principal, @Valid @RequestBody SavingsGoalRequest request) {
        return savingsGoalService.create(principal.getName(), request);
    }

    @GetMapping("/{id}")
    public SavingsGoalResponse getById(Principal principal, @PathVariable Long id) {
        return savingsGoalService.getById(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse update(Principal principal, @PathVariable Long id,
                                      @Valid @RequestBody SavingsGoalRequest request) {
        return savingsGoalService.update(principal.getName(), id, request);
    }

    @PutMapping("/{id}/progress")
    public SavingsGoalResponse updateProgress(Principal principal, @PathVariable Long id,
                                              @Valid @RequestBody SavingsGoalProgressRequest request) {
        return savingsGoalService.updateProgress(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable Long id) {
        savingsGoalService.delete(principal.getName(), id);
    }
}
