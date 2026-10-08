package com.financeflow.controller;

import com.financeflow.dto.TransactionRequest;
import com.financeflow.dto.TransactionResponse;
import com.financeflow.entity.Transaction;
import com.financeflow.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(Principal principal, @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(principal.getName(), request);
    }

    @GetMapping
    public List<TransactionResponse> getAll(
            Principal principal,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long accountId) {
        return transactionService.getAll(principal.getName(), type, category, date, accountId);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(Principal principal, @PathVariable Long id) {
        return transactionService.getById(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(Principal principal, @PathVariable Long id,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable Long id) {
        transactionService.delete(principal.getName(), id);
    }
}
