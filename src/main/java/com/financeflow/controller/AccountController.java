package com.financeflow.controller;

import com.financeflow.dto.AccountRequest;
import com.financeflow.dto.AccountResponse;
import com.financeflow.service.AccountService;
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
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> getAccounts(Principal principal) {
        return accountService.getAccounts(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(Principal principal, @Valid @RequestBody AccountRequest request) {
        return accountService.createAccount(principal.getName(), request);
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(Principal principal, @PathVariable Long id) {
        return accountService.getAccount(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(Principal principal, @PathVariable Long id,
                                         @Valid @RequestBody AccountRequest request) {
        return accountService.updateAccount(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Principal principal, @PathVariable Long id) {
        accountService.deleteAccount(principal.getName(), id);
    }
}
