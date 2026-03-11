package com.finly.backend.service;

import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.CategoryType;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.response.AccountBalanceResponse;
import com.finly.backend.dto.response.AnalyticsSummaryResponse;
import com.finly.backend.dto.response.CategoryComparisonResponse;
import com.finly.backend.dto.response.CategoryStatsResponse;
import com.finly.backend.dto.response.DashboardSummaryResponse;
import com.finly.backend.dto.response.ExpenseCategoryAmountResponse;
import com.finly.backend.dto.response.IncomeVsExpenseResponse;
import com.finly.backend.dto.response.MonthlyAmountResponse;
import com.finly.backend.dto.response.TimeSeriesResponse;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.repository.AnalyticsRepository;
import com.finly.backend.repository.CategoryRepository;
import com.finly.backend.repository.ExpenseRepository;
import com.finly.backend.repository.IncomeRepository;
import com.finly.backend.repository.UserRepository;
import com.finly.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final AnalyticsRepository analyticsRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(UserDetailsImpl principal) {
        UUID userId = getUser(principal).getId();
        BigDecimal totalBalance = defaultZero(analyticsRepository.totalBalance(userId));
        BigDecimal totalIncome = defaultZero(analyticsRepository.totalIncome(userId));
        BigDecimal totalExpense = defaultZero(analyticsRepository.totalExpense(userId));
        return AnalyticsSummaryResponse.builder()
                .totalBalance(totalBalance)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .savings(totalIncome.subtract(totalExpense))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryAmountResponse> getExpensesByCategory(LocalDate from, LocalDate to,
            UserDetailsImpl principal) {
        UUID userId = getUser(principal).getId();
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        return analyticsRepository.expensesByCategory(userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream()
                .map(row -> ExpenseCategoryAmountResponse.builder()
                        .category(String.valueOf(row[0]))
                        .amount(defaultZero((BigDecimal) row[1]))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyAmountResponse> getMonthlyExpenses(LocalDate from, LocalDate to, UserDetailsImpl principal) {
        UUID userId = getUser(principal).getId();
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusMonths(12);
        LocalDateTime fromDate = resolvedFrom.atStartOfDay();
        LocalDateTime toDate = resolvedTo.plusDays(1).atStartOfDay();
        return analyticsRepository.monthlyExpenses(userId, fromDate, toDate).stream()
                .map(row -> MonthlyAmountResponse.builder()
                        .month(String.valueOf(row[0]))
                        .amount(defaultZero((BigDecimal) row[1]))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncomeVsExpenseResponse> getIncomeVsExpense(LocalDate from, LocalDate to, UserDetailsImpl principal) {
        UUID userId = getUser(principal).getId();
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusMonths(12);
        LocalDateTime fromDate = resolvedFrom.atStartOfDay();
        LocalDateTime toDate = resolvedTo.plusDays(1).atStartOfDay();
        Map<String, IncomeVsExpenseResponse> map = new TreeMap<>();

        for (Object[] row : analyticsRepository.monthlyIncomes(userId, fromDate, toDate)) {
            String month = String.valueOf(row[0]);
            BigDecimal income = defaultZero((BigDecimal) row[1]);
            map.put(month, IncomeVsExpenseResponse.builder()
                    .month(month)
                    .income(income)
                    .expense(BigDecimal.ZERO)
                    .build());
        }

        for (Object[] row : analyticsRepository.monthlyExpenses(userId, fromDate, toDate)) {
            String month = String.valueOf(row[0]);
            BigDecimal expense = defaultZero((BigDecimal) row[1]);
            map.compute(month, (m, existing) -> {
                if (existing == null) {
                    return IncomeVsExpenseResponse.builder()
                            .month(m)
                            .income(BigDecimal.ZERO)
                            .expense(expense)
                            .build();
                }
                existing.setExpense(expense);
                return existing;
            });
        }

        return new ArrayList<>(map.values());
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceResponse> getAccountBalances(UserDetailsImpl principal) {
        UUID userId = getUser(principal).getId();
        return analyticsRepository.accountBalances(userId).stream()
                .map(row -> AccountBalanceResponse.builder()
                        .accountName(String.valueOf(row[0]))
                        .balance(defaultZero((BigDecimal) row[1]))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);

        BigDecimal totalIncome = incomeRepository.getTotalIncomeSum(user, startDate, endDate);
        if (totalIncome == null)
            totalIncome = BigDecimal.ZERO;

        BigDecimal totalExpense = expenseRepository.getTotalExpenseSum(user, startDate, endDate);
        if (totalExpense == null)
            totalExpense = BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpense);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = balance.multiply(new BigDecimal("100"))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);
        }

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .savingsRate(savingsRate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryStatsResponse> getCategoryStatistics(CategoryType type, LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);
        List<Object[]> results;
        BigDecimal totalAmount;

        if (type == CategoryType.EXPENSE) {
            results = expenseRepository.getExpenseStatsByCategory(user, startDate, endDate);
            totalAmount = expenseRepository.getTotalExpenseSum(user, startDate, endDate);
        } else {
            results = incomeRepository.getIncomeStatsByCategory(user, startDate, endDate);
            totalAmount = incomeRepository.getTotalIncomeSum(user, startDate, endDate);
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        final BigDecimal finalTotal = totalAmount;

        return results.stream()
                .map(row -> {
                    BigDecimal amount = (BigDecimal) row[2];
                    BigDecimal percentage = amount.multiply(new BigDecimal("100"))
                            .divide(finalTotal, 2, RoundingMode.HALF_UP);

                    return CategoryStatsResponse.builder()
                            .categoryId((UUID) row[0])
                            .categoryName((String) row[1])
                            .totalAmount(amount)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesResponse> getTimeSeries(String periodType, LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);
        Map<String, TimeSeriesResponse> timeSeriesMap = new TreeMap<>();

        List<Object[]> incomeResults;
        List<Object[]> expenseResults;

        switch (periodType.toUpperCase()) {
            case "DAILY":
                incomeResults = incomeRepository.getIncomeTimeSeriesDaily(user.getId(), startDate, endDate);
                expenseResults = expenseRepository.getExpenseTimeSeriesDaily(user.getId(), startDate, endDate);
                break;
            case "WEEKLY":
                incomeResults = incomeRepository.getIncomeTimeSeriesWeekly(user.getId(), startDate, endDate);
                expenseResults = expenseRepository.getExpenseTimeSeriesWeekly(user.getId(), startDate, endDate);
                break;
            case "MONTHLY":
                incomeResults = incomeRepository.getIncomeTimeSeriesMonthly(user.getId(), startDate, endDate);
                expenseResults = expenseRepository.getExpenseTimeSeriesMonthly(user.getId(), startDate, endDate);
                break;
            case "YEARLY":
                incomeResults = incomeRepository.getIncomeTimeSeriesYearly(user.getId(), startDate, endDate);
                expenseResults = expenseRepository.getExpenseTimeSeriesYearly(user.getId(), startDate, endDate);
                break;
            default:
                throw new IllegalArgumentException("Invalid period type: " + periodType);
        }

        for (Object[] row : incomeResults) {
            String period = String.valueOf(row[0]);
            BigDecimal amount = (BigDecimal) row[1];
            timeSeriesMap.put(period, TimeSeriesResponse.builder()
                    .period(period)
                    .incomeAmount(amount)
                    .expenseAmount(BigDecimal.ZERO)
                    .build());
        }

        for (Object[] row : expenseResults) {
            String period = String.valueOf(row[0]);
            BigDecimal amount = (BigDecimal) row[1];
            if (timeSeriesMap.containsKey(period)) {
                timeSeriesMap.get(period).setExpenseAmount(amount);
            } else {
                timeSeriesMap.put(period, TimeSeriesResponse.builder()
                        .period(period)
                        .incomeAmount(BigDecimal.ZERO)
                        .expenseAmount(amount)
                        .build());
            }
        }

        return new ArrayList<>(timeSeriesMap.values());
    }

    @Transactional(readOnly = true)
    public List<CategoryComparisonResponse> getCategoryComparison(LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);
        List<Category> categories = categoryRepository.findAllByUser(user);
        List<CategoryComparisonResponse> comparisonList = new ArrayList<>();

        for (Category category : categories) {
            BigDecimal expense = expenseRepository.getCategoryTotalAmount(user.getId(), category.getId(), startDate,
                    endDate);
            BigDecimal income = incomeRepository.getCategoryTotalAmount(user.getId(), category.getId(), startDate,
                    endDate);

            if (expense == null)
                expense = BigDecimal.ZERO;
            if (income == null)
                income = BigDecimal.ZERO;

            if (expense.compareTo(BigDecimal.ZERO) > 0 || income.compareTo(BigDecimal.ZERO) > 0) {
                comparisonList.add(CategoryComparisonResponse.builder()
                        .categoryId(category.getId())
                        .categoryName(category.getName())
                        .incomeAmount(income)
                        .expenseAmount(expense)
                        .netAmount(income.subtract(expense))
                        .build());
            }
        }

        return comparisonList;
    }

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
