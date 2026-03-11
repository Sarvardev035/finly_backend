package com.finly.backend.repository;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.Expense;
import com.finly.backend.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
        List<Expense> findByUserId(UUID userId);

        boolean existsByAccount(Account account);
        boolean existsByAccountId(UUID accountId);

        List<Expense> findTop10ByOrderByCreatedAtDesc();

        Page<Expense> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

        List<Expense> findAllByUser(User user);

        List<Expense> findAllByUserOrderByExpenseDateDesc(User user);

        List<Expense> findAllByUserAndAccountOrderByExpenseDateDesc(User user, Account account);

        List<Expense> findAllByUserAndCategoryOrderByExpenseDateDesc(User user, Category category);

        List<Expense> findAllByUserAndExpenseDateBetweenOrderByExpenseDateDesc(User user, LocalDate startDate,
                        LocalDate endDate);

        Optional<Expense> findByIdAndUser(UUID id, User user);

        @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.expenseDate BETWEEN :startDate AND :endDate")
        BigDecimal getTotalExpenseSum(@Param("user") User user, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT e.category.id, e.category.name, SUM(e.amount) FROM Expense e " +
                        "WHERE e.user = :user AND e.expenseDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY e.category.id, e.category.name")
        List<Object[]> getExpenseStatsByCategory(@Param("user") User user, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT CAST(expense_date AS DATE) as period, SUM(amount) as total FROM expenses " +
                        "WHERE user_id = :userId AND expense_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY CAST(expense_date AS DATE) ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getExpenseTimeSeriesDaily(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT TO_CHAR(expense_date, 'YYYY-MM') as period, SUM(amount) as total FROM expenses " +
                        "WHERE user_id = :userId AND expense_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY TO_CHAR(expense_date, 'YYYY-MM') ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getExpenseTimeSeriesMonthly(@Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT TO_CHAR(expense_date, 'IYYY-IW') as period, SUM(amount) as total FROM expenses " +
                        "WHERE user_id = :userId AND expense_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY TO_CHAR(expense_date, 'IYYY-IW') ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getExpenseTimeSeriesWeekly(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT TO_CHAR(expense_date, 'YYYY') as period, SUM(amount) as total FROM expenses " +
                        "WHERE user_id = :userId AND expense_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY TO_CHAR(expense_date, 'YYYY') ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getExpenseTimeSeriesYearly(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.category.id = :categoryId AND e.expenseDate BETWEEN :startDate AND :endDate")
        BigDecimal getCategoryTotalAmount(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(e.amount) FROM Expense e")
        BigDecimal getTotalExpenseSum();
}
