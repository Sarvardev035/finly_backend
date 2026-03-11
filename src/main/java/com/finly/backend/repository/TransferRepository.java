package com.finly.backend.repository;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Transfer;
import com.finly.backend.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {
        List<Transfer> findByUserId(UUID userId);

        boolean existsByFromAccount(Account account);

        boolean existsByToAccount(Account account);

        boolean existsByFromAccountId(UUID accountId);

        boolean existsByToAccountId(UUID accountId);

        List<Transfer> findAllByUser(User user);

        List<Transfer> findAllByUserOrderByTransferDateDesc(User user);

        @Query("SELECT t FROM Transfer t WHERE t.user = :user AND (t.fromAccount = :account OR t.toAccount = :account) ORDER BY t.transferDate DESC")
        List<Transfer> findAllByUserAndAccountOrderByTransferDateDesc(@Param("user") User user,
                        @Param("account") Account account);

        List<Transfer> findAllByUserAndTransferDateBetweenOrderByTransferDateDesc(User user, LocalDate startDate,
                        LocalDate endDate);

        Optional<Transfer> findByIdAndUser(UUID id, User user);
}
