package com.financeflow.repository;

import com.financeflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdAndUser_Id(Long id, Long userId);
    boolean existsByAccount_Id(Long accountId);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.type = :type and t.category = :category " +
            "and t.transactionDate >= :startDate and t.transactionDate < :endDate")
    BigDecimal sumByUserAndTypeAndCategoryAndDateRange(@Param("userId") Long userId,
                                                        @Param("type") Transaction.TransactionType type,
                                                        @Param("category") String category,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t " +
            "where t.user.id = :userId and t.type = :type " +
            "and t.transactionDate >= :startDate and t.transactionDate < :endDate")
    BigDecimal sumByUserAndTypeAndDateRange(@Param("userId") Long userId,
                                             @Param("type") Transaction.TransactionType type,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("select t.category as category, sum(t.amount) as amount from Transaction t " +
            "where t.user.id = :userId and t.type = :type " +
            "and t.transactionDate >= :startDate and t.transactionDate < :endDate " +
            "group by t.category order by t.category")
    List<CategorySpendingProjection> sumByCategoryAndDateRange(@Param("userId") Long userId,
                                                                @Param("type") Transaction.TransactionType type,
                                                                @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT t.id AS id, t.amount AS amount, t.type AS type, " +
            "t.category AS category, t.description AS description, " +
            "t.transaction_date AS transactionDate, t.account_id AS accountId, " +
            "COALESCE(a.name, 'Account unavailable') AS accountName " +
            "FROM transactions t LEFT JOIN accounts a ON a.id = t.account_id " +
            "WHERE t.user_id = :userId " +
            "AND (:transactionId IS NULL OR t.id = :transactionId) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:date IS NULL OR t.transaction_date = :date) " +
            "AND (:accountId IS NULL OR t.account_id = :accountId) " +
            "ORDER BY t.transaction_date DESC, t.id DESC", nativeQuery = true)
    List<TransactionHistoryProjection> findHistory(@Param("userId") Long userId,
                                                   @Param("transactionId") Long transactionId,
                                                   @Param("type") String type,
                                                   @Param("category") String category,
                                                   @Param("date") LocalDate date,
                                                   @Param("accountId") Long accountId);

    @Query(value = "SELECT t.id AS id, t.amount AS amount, t.type AS type, " +
            "t.category AS category, t.description AS description, " +
            "t.transaction_date AS transactionDate, t.account_id AS accountId, " +
            "COALESCE(a.name, 'Account unavailable') AS accountName " +
            "FROM transactions t LEFT JOIN accounts a ON a.id = t.account_id " +
            "WHERE t.user_id = :userId " +
            "ORDER BY t.transaction_date DESC, t.id DESC LIMIT 5", nativeQuery = true)
    List<TransactionHistoryProjection> findRecentHistory(@Param("userId") Long userId);

    interface TransactionHistoryProjection {
        Long getId();
        java.math.BigDecimal getAmount();
        String getType();
        String getCategory();
        String getDescription();
        LocalDate getTransactionDate();
        Long getAccountId();
        String getAccountName();
    }

    interface CategorySpendingProjection {
        String getCategory();
        BigDecimal getAmount();
    }
}
