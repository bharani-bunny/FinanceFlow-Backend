package com.financeflow.repository;

import com.financeflow.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAllByUser_IdOrderByIdAsc(Long userId);
    Optional<Account> findByIdAndUser_Id(Long id, Long userId);

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.user.id = :userId")
    BigDecimal sumBalancesByUserId(@Param("userId") Long userId);
}
