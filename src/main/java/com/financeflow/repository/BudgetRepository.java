package com.financeflow.repository;

import com.financeflow.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByUser_IdOrderByYearDescMonthDescCategoryAsc(Long userId);

    List<Budget> findAllByUser_IdAndMonthAndYearOrderByCategoryAsc(Long userId, Integer month, Integer year);

    Optional<Budget> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndCategoryAndMonthAndYear(Long userId, String category, Integer month, Integer year);

    boolean existsByUser_IdAndCategoryAndMonthAndYearAndIdNot(
            Long userId, String category, Integer month, Integer year, Long id);
}
