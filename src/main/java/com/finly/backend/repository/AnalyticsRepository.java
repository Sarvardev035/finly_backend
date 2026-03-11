package com.finly.backend.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AnalyticsRepository extends Repository<com.finly.backend.domain.model.User, UUID> {

    @Query(value = "SELECT COALESCE(SUM(a.balance), 0) FROM accounts a WHERE a.user_id = :userId", nativeQuery = true)
    BigDecimal totalBalance(@Param("userId") UUID userId);

    @Query(value = "SELECT COALESCE(SUM(i.amount), 0) FROM incomes i WHERE i.user_id = :userId", nativeQuery = true)
    BigDecimal totalIncome(@Param("userId") UUID userId);

    @Query(value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e WHERE e.user_id = :userId", nativeQuery = true)
    BigDecimal totalExpense(@Param("userId") UUID userId);

    @Query(value = """
            SELECT c.name, COALESCE(SUM(e.amount), 0) AS total_amount
            FROM expenses e
            JOIN categories c ON e.category_id = c.id
            WHERE e.user_id = :userId
              AND e.created_at >= :fromDate
              AND e.created_at < :toDate
            GROUP BY c.name
            ORDER BY total_amount DESC
            """, nativeQuery = true)
    List<Object[]> expensesByCategory(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('month', e.created_at), 'YYYY-MM') AS month_key,
                   COALESCE(SUM(e.amount), 0) AS total_amount
            FROM expenses e
            WHERE e.user_id = :userId
              AND e.created_at >= :fromDate
              AND e.created_at < :toDate
            GROUP BY DATE_TRUNC('month', e.created_at)
            ORDER BY DATE_TRUNC('month', e.created_at)
            """, nativeQuery = true)
    List<Object[]> monthlyExpenses(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('month', i.created_at), 'YYYY-MM') AS month_key,
                   COALESCE(SUM(i.amount), 0) AS total_amount
            FROM incomes i
            WHERE i.user_id = :userId
              AND i.created_at >= :fromDate
              AND i.created_at < :toDate
            GROUP BY DATE_TRUNC('month', i.created_at)
            ORDER BY DATE_TRUNC('month', i.created_at)
            """, nativeQuery = true)
    List<Object[]> monthlyIncomes(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query(value = """
            SELECT a.name, a.balance
            FROM accounts a
            WHERE a.user_id = :userId
            ORDER BY a.created_at
            """, nativeQuery = true)
    List<Object[]> accountBalances(@Param("userId") UUID userId);
}
