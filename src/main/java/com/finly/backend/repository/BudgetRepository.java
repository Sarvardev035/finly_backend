package com.finly.backend.repository;

import com.finly.backend.domain.model.Budget;
import com.finly.backend.domain.model.BudgetType;
import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserId(UUID userId);

    List<Budget> findByUserAndYearAndMonth(User user, int year, int month);

    List<Budget> findByUserAndTypeAndYearAndMonth(User user, BudgetType type, int year, int month);

    Optional<Budget> findByUserAndTypeAndCategoryAndYearAndMonth(User user, BudgetType type, Category category,
            int year, int month);

    Optional<Budget> findByUserAndTypeAndCategoryIsNullAndYearAndMonth(User user, BudgetType type, int year, int month);

    List<Budget> findAllByUser(User user);

    Optional<Budget> findByIdAndUser(UUID id, User user);
}
