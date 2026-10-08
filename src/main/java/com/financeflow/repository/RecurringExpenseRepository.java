package com.financeflow.repository;

import com.financeflow.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findAllByUser_IdOrderByNextPaymentDateAscIdAsc(Long userId);

    List<RecurringExpense> findAllByUser_IdAndNextPaymentDateGreaterThanEqualOrderByNextPaymentDateAscIdAsc(
            Long userId, LocalDate date);

    Optional<RecurringExpense> findByIdAndUser_Id(Long id, Long userId);
}
