package com.finly.backend.repository;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUserId(UUID userId);

    Page<Account> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Account> findByUserIdAndNameContainingIgnoreCase(UUID userId, String name, Pageable pageable);
    Page<Account> findByUserId(UUID userId, Pageable pageable);

    List<Account> findAllByUser(User user);

    Optional<Account> findByIdAndUser(UUID id, User user);

    @Query("SELECT SUM(a.balance) FROM Account a")
    BigDecimal getTotalBalanceSum();
}
