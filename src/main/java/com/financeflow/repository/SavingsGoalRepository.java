package com.financeflow.repository;

import com.financeflow.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findAllByUser_IdOrderByTargetDateAscIdAsc(Long userId);

    Optional<SavingsGoal> findByIdAndUser_Id(Long id, Long userId);
}
