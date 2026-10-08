package com.financeflow.service;

import com.financeflow.dto.AccountRequest;
import com.financeflow.dto.AccountResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.User;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.UserRepository;
import com.financeflow.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(String email) {
        User user = getUser(email);
        return accountRepository.findAllByUser_IdOrderByIdAsc(user.getId()).stream()
                .map(AccountService::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse createAccount(String email, AccountRequest request) {
        User user = getUser(email);
        Account account = new Account();
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setBalance(request.balance());
        account.setUser(user);
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String email, Long accountId) {
        User user = getUser(email);
        return toResponse(findAccount(accountId, user.getId()));
    }

    @Transactional
    public AccountResponse updateAccount(String email, Long accountId, AccountRequest request) {
        User user = getUser(email);
        Account account = findAccount(accountId, user.getId());
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setBalance(request.balance());
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(String email, Long accountId) {
        User user = getUser(email);
        Account account = findAccount(accountId, user.getId());
        if (transactionRepository.existsByAccount_Id(account.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account cannot be deleted while transactions are linked to it");
        }
        try {
            accountRepository.delete(account);
            accountRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account cannot be deleted while transactions are linked to it");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));
    }

    private Account findAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getType(),
                account.getBalance(), account.getCreatedAt());
    }
}



