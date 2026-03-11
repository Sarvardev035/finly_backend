package com.finly.backend.repository;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.Income;
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
public interface IncomeRepository extends JpaRepository<Income, UUID> {
        List<Income> findByUserId(UUID userId);

        boolean existsByAccount(Account account);
        boolean existsByAccountId(UUID accountId);

        Page<Income> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

        List<Income> findTop10ByOrderByCreatedAtDesc();

        List<Income> findAllByUser(User user);

        List<Income> findAllByUserOrderByIncomeDateDesc(User user);

        List<Income> findAllByUserAndAccountOrderByIncomeDateDesc(User user, Account account);

        List<Income> findAllByUserAndCategoryOrderByIncomeDateDesc(User user, Category category);

        List<Income> findAllByUserAndIncomeDateBetweenOrderByIncomeDateDesc(User user, LocalDate startDate,
                        LocalDate endDate);

        Optional<Income> findByIdAndUser(UUID id, User user);

        @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user = :user AND i.incomeDate BETWEEN :startDate AND :endDate")
        BigDecimal getTotalIncomeSum(@Param("user") User user, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT i.category.id, i.category.name, SUM(i.amount) FROM Income i " +
                        "WHERE i.user = :user AND i.incomeDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY i.category.id, i.category.name")
        List<Object[]> getIncomeStatsByCategory(@Param("user") User user, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT CAST(income_date AS DATE) as period, SUM(amount) as total FROM incomes " +
                        "WHERE user_id = :userId AND income_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY CAST(income_date AS DATE) ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getIncomeTimeSeriesDaily(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT TO_CHAR(income_date, 'YYYY-MM') as period, SUM(amount) as total FROM incomes " +
                        "WHERE user_id = :userId AND income_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY TO_CHAR(income_date, 'YYYY-MM') ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getIncomeTimeSeriesMonthly(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT TO_CHAR(income_date, 'IYYY-IW') as period, SUM(amount) as total FROM incomes " +
                        "WHERE user_id = :userId AND income_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY TO_CHAR(income_date, 'IYYY-IW') ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getIncomeTimeSeriesWeekly(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = "SELECT TO_CHAR(income_date, 'YYYY') as period, SUM(amount) as total FROM incomes " +
                        "WHERE user_id = :userId AND income_date BETWEEN :startDate AND :endDate " +
                        "GROUP BY TO_CHAR(income_date, 'YYYY') ORDER BY period ASC", nativeQuery = true)
        List<Object[]> getIncomeTimeSeriesYearly(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId AND i.category.id = :categoryId AND i.incomeDate BETWEEN :startDate AND :endDate")
        BigDecimal getCategoryTotalAmount(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId,
                        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(i.amount) FROM Income i")
        BigDecimal getTotalIncomeSum();
}
