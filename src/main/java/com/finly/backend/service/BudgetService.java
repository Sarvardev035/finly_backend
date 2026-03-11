package com.finly.backend.service;

import com.finly.backend.domain.model.Budget;
import com.finly.backend.domain.model.BudgetType;
import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.Expense;
import com.finly.backend.domain.model.Income;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateBudgetRequest;
import com.finly.backend.dto.response.BudgetResponse;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.BudgetRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    @Transactional
    public BudgetResponse createOrUpdateBudget(CreateBudgetRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createOrUpdateBudget(request, user);
    }

    @Transactional
    public BudgetResponse createOrUpdateBudget(CreateBudgetRequest request, User user) {
        Category category = null;

        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
        }

        Optional<Budget> existingBudget;
        if (category != null) {
            existingBudget = budgetRepository.findByUserAndTypeAndCategoryAndYearAndMonth(user, request.getType(),
                    category, request.getYear(),
                    request.getMonth());
        } else {
            existingBudget = budgetRepository.findByUserAndTypeAndCategoryIsNullAndYearAndMonth(user, request.getType(),
                    request.getYear(),
                    request.getMonth());
        }

        Budget budget;
        if (existingBudget.isPresent()) {
            @SuppressWarnings("null")
            Budget b = existingBudget.get();
            budget = b;
            budget.setMonthlyLimit(request.getMonthlyLimit());
        } else {
            budget = Budget.builder()
                    .user(user)
                    .type(request.getType())
                    .category(category)
                    .monthlyLimit(request.getMonthlyLimit())
                    .year(request.getYear())
                    .month(request.getMonth())
                    .build();
        }

        @SuppressWarnings("null")
        Budget savedBudget = budgetRepository.save(budget);
        return calculateBudgetStats(savedBudget, user);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(int year, int month, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getBudgets(year, month, user);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(int year, int month, User user) {
        List<Budget> budgets = budgetRepository.findByUserAndYearAndMonth(user, year, month);

        return budgets.stream()
                .map(budget -> calculateBudgetStats(budget, user))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBudget(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteBudget(id, user);
    }

    @Transactional
    public void deleteBudget(UUID id, User user) {
        Budget budget = budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));

        budgetRepository.delete(budget);
    }

    // ---- Helper Methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    @Transactional(readOnly = true)
    public BudgetResponse calculateBudgetStats(Budget budget, User user) {
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        BigDecimal realizedAmount;
        if (budget.getType() == BudgetType.EXPENSE) {
            List<Expense> expenses = expenseRepository.findAllByUserAndExpenseDateBetweenOrderByExpenseDateDesc(user,
                    startDate, endDate);
            if (budget.getCategory() != null) {
                realizedAmount = expenses.stream()
                        .filter(e -> e.getCategory().getId().equals(budget.getCategory().getId()))
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                realizedAmount = expenses.stream()
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        } else {
            // INCOME logic
            List<Income> incomes = incomeRepository.findAllByUserAndIncomeDateBetweenOrderByIncomeDateDesc(user,
                    startDate, endDate);
            if (budget.getCategory() != null) {
                realizedAmount = incomes.stream()
                        .filter(i -> i.getCategory().getId().equals(budget.getCategory().getId()))
                        .map(Income::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                realizedAmount = incomes.stream()
                        .map(Income::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        BigDecimal remainingAmount = budget.getMonthlyLimit().subtract(realizedAmount);

        BigDecimal percentageUsed = BigDecimal.ZERO;
        if (budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = realizedAmount.multiply(new BigDecimal("100"))
                    .divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP);
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .type(budget.getType())
                .categoryId(budget.getCategory() != null ? budget.getCategory().getId() : null)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getName()
                        : (budget.getType() == BudgetType.EXPENSE ? "Overall Expense Budget" : "Overall Income Target"))
                .monthlyLimit(budget.getMonthlyLimit())
                .spentAmount(realizedAmount)
                .remainingAmount(remainingAmount)
                .percentageUsed(percentageUsed)
                .year(budget.getYear())
                .month(budget.getMonth())
                .build();
    }
}
