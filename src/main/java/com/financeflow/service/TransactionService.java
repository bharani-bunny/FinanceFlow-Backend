package com.financeflow.service;

import com.financeflow.dto.TransactionRequest;
import com.financeflow.dto.TransactionResponse;
import com.financeflow.entity.Account;
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
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransactionResponse create(String email, TransactionRequest request) {
        User user = getUser(email);
        Account account = getAccount(request.accountId(), user.getId());
        Transaction transaction = new Transaction();
        setFields(transaction, request, account, user);
        applyBalanceChange(account, request.type(), request.amount(), true);
        accountRepository.save(account);
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll(String email, Transaction.TransactionType type,
                                            String category, LocalDate date, Long accountId) {
        User user = getUser(email);
        return transactionRepository.findHistory(user.getId(), null, type == null ? null : type.name(),
                        category, date, accountId).stream()
                .map(TransactionService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecent(String email) {
        User user = getUser(email);
        return transactionRepository.findRecentHistory(user.getId()).stream()
                .map(TransactionService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(String email, Long transactionId) {
        User user = getUser(email);
        return transactionRepository.findHistory(user.getId(), transactionId, null, null, null, null).stream()
                .findFirst()
                .map(TransactionService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @Transactional
    public TransactionResponse update(String email, Long transactionId, TransactionRequest request) {
        User user = getUser(email);
        Transaction transaction = findTransaction(transactionId, user.getId());
        Account oldAccount = getAccount(transaction.getAccount().getId(), user.getId());
        Account newAccount = getAccount(request.accountId(), user.getId());

        applyBalanceChange(oldAccount, transaction.getType(), transaction.getAmount(), false);
        applyBalanceChange(newAccount, request.type(), request.amount(), true);
        accountRepository.save(oldAccount);
        if (!oldAccount.getId().equals(newAccount.getId())) accountRepository.save(newAccount);

        setFields(transaction, request, newAccount, user);
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(String email, Long transactionId) {
        User user = getUser(email);
        Transaction transaction = findTransaction(transactionId, user.getId());
        Account account = getAccount(transaction.getAccount().getId(), user.getId());
        applyBalanceChange(account, transaction.getType(), transaction.getAmount(), false);
        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));
    }

    private Account getAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Transaction findTransaction(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUser_Id(transactionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private static void setFields(Transaction transaction, TransactionRequest request, Account account, User user) {
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.date());
        transaction.setAccount(account);
        transaction.setUser(user);
    }

    private static void applyBalanceChange(Account account, Transaction.TransactionType type,
                                           BigDecimal amount, boolean apply) {
        boolean increase = (type == Transaction.TransactionType.INCOME) == apply;
        BigDecimal newBalance = increase ? account.getBalance().add(amount) : account.getBalance().subtract(amount);
        account.setBalance(newBalance);
    }

    private static TransactionResponse toResponse(TransactionRepository.TransactionHistoryProjection transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAmount(),
                Transaction.TransactionType.valueOf(transaction.getType()), transaction.getCategory(),
                transaction.getDescription(), transaction.getTransactionDate(),
                transaction.getAccountId(), transaction.getAccountName());
    }

    private static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getType(),
                transaction.getCategory(), transaction.getDescription(), transaction.getTransactionDate(),
                transaction.getAccount().getId(), transaction.getAccount().getName());
    }
}
