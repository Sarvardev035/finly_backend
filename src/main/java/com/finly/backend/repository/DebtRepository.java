package com.finly.backend.repository;

import com.finly.backend.domain.model.Debt;
import com.finly.backend.domain.model.DebtStatus;
import com.finly.backend.domain.model.DebtType;
import com.finly.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DebtRepository extends JpaRepository<Debt, UUID> {
    List<Debt> findByUserId(UUID userId);

    List<Debt> findAllByUser(User user);

    List<Debt> findAllByUserOrderByDueDateAsc(User user);

    List<Debt> findAllByUserAndTypeOrderByDueDateAsc(User user, DebtType type);

    List<Debt> findAllByUserAndStatusOrderByDueDateAsc(User user, DebtStatus status);

    List<Debt> findAllByUserAndTypeAndStatusOrderByDueDateAsc(User user, DebtType type, DebtStatus status);

    List<Debt> findAllByStatusAndDueDateLessThanEqual(DebtStatus status, java.time.LocalDate dueDate);

    Optional<Debt> findByIdAndUser(UUID id, User user);

    Page<Debt> findByPersonNameContainingIgnoreCase(String personName, Pageable pageable);
}
