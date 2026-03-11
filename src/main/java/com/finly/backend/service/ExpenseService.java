package com.finly.backend.service;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.Expense;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateExpenseRequest;
import com.finly.backend.dto.request.UpdateExpenseRequest;
import com.finly.backend.dto.response.ExpenseResponse;
import com.finly.backend.exception.AccountNotFoundException;
import com.finly.backend.exception.CategoryNotFoundException;
import com.finly.backend.exception.InsufficientBalanceException;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.AccountRepository;
import com.finly.backend.repository.CategoryRepository;
import com.finly.backend.repository.ExpenseRepository;
import com.finly.backend.repository.UserRepository;
import com.finly.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CurrencyConversionService currencyConversionService;

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createExpense(request, user);
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, User user) {

        Account account = accountRepository.findByIdAndUser(request.getAccountId(), user)
                .orElseThrow(
                        () -> new AccountNotFoundException("Account not found with id: " + request.getAccountId()));

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(
                        () -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));

        Currency expenseCurrency = resolveExpenseCurrency(request.getCurrency(), account);
        BigDecimal debitAmount = toAccountCurrencyAmount(request.getAmount(), expenseCurrency, account);

        // Validate balance and deduct
        if (account.getBalance().compareTo(debitAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account: " + account.getName());
        }

        account.setBalance(account.getBalance().subtract(debitAmount));
        accountRepository.save(account);

        Expense expense = Expense.builder()
                .user(user)
                .account(account)
                .category(category)
                .amount(request.getAmount())
                .currency(expenseCurrency)
                .description(request.getDescription())
                .expenseDate(request.getExpenseDate())
                .build();

        @SuppressWarnings("null")
        Expense savedExpense = expenseRepository.save(expense);

        // AI & Notification hooks
        notificationService.autoCategorizeExpense(savedExpense);
        notificationService.checkOverspending(savedExpense);

        return mapToResponse(savedExpense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(UUID accountId, UUID categoryId, LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);
        return getExpenses(accountId, categoryId, startDate, endDate, user);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(UUID accountId, UUID categoryId, LocalDate startDate, LocalDate endDate,
            User user) {

        List<Expense> expenses;

        // Simple filtering logic (in production, use CriteriaBuilder or QueryDSL for
        // dynamic complex queries)
        if (startDate != null && endDate != null) {
            expenses = expenseRepository.findAllByUserAndExpenseDateBetweenOrderByExpenseDateDesc(user, startDate,
                    endDate);
        } else if (accountId != null) {
            Account account = accountRepository.findByIdAndUser(accountId, user)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
            expenses = expenseRepository.findAllByUserAndAccountOrderByExpenseDateDesc(user, account);
        } else if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUser(categoryId, user)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            expenses = expenseRepository.findAllByUserAndCategoryOrderByExpenseDateDesc(user, category);
        } else {
            expenses = expenseRepository.findAllByUserOrderByExpenseDateDesc(user);
        }

        return expenses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getExpenseById(id, user);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID id, User user) {
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return mapToResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, UpdateExpenseRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return updateExpense(id, request, user);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, UpdateExpenseRequest request, User user) {
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        Account oldAccount = expense.getAccount();
        Currency oldExpenseCurrency = resolveExpenseCurrency(expense.getCurrency(), oldAccount);
        BigDecimal oldDebitAmount = toAccountCurrencyAmount(expense.getAmount(), oldExpenseCurrency, oldAccount);
        BigDecimal newAmount = request.getAmount() != null ? request.getAmount() : expense.getAmount();
        Currency newExpenseCurrency = request.getCurrency() != null ? request.getCurrency() : oldExpenseCurrency;

        // Handle changing the account
        if (request.getAccountId() != null && !request.getAccountId().equals(oldAccount.getId())) {
            Account newAccount = accountRepository.findByIdAndUser(request.getAccountId(), user)
                    .orElseThrow(() -> new AccountNotFoundException("New account not found"));

            // Restore balance to old account
            oldAccount.setBalance(oldAccount.getBalance().add(oldDebitAmount));
            accountRepository.save(oldAccount);

            // Deduct from new account
            BigDecimal newDebitAmount = toAccountCurrencyAmount(newAmount, newExpenseCurrency, newAccount);
            if (newAccount.getBalance().compareTo(newDebitAmount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in new account: " + newAccount.getName());
            }
            newAccount.setBalance(newAccount.getBalance().subtract(newDebitAmount));
            accountRepository.save(newAccount);

            expense.setAccount(newAccount);
        } else {
            // Same account: reconcile old debit with new debit in account currency
            BigDecimal newDebitAmount = toAccountCurrencyAmount(newAmount, newExpenseCurrency, oldAccount);
            BigDecimal delta = newDebitAmount.subtract(oldDebitAmount);
            if (delta.compareTo(BigDecimal.ZERO) > 0 && oldAccount.getBalance().compareTo(delta) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in account to cover increased expense");
            }
            oldAccount.setBalance(oldAccount.getBalance().subtract(delta));
            accountRepository.save(oldAccount);
        }

        // Handle other fields
        if (request.getCategoryId() != null && !request.getCategoryId().equals(expense.getCategory().getId())) {
            Category newCategory = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            expense.setCategory(newCategory);
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            expense.setDescription(request.getDescription());
        }

        expense.setAmount(newAmount);
        expense.setCurrency(newExpenseCurrency);

        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }

        Expense updatedExpense = expenseRepository.save(expense);

        // AI & Notification hooks (re-check after update)
        notificationService.checkOverspending(updatedExpense);

        return mapToResponse(updatedExpense);
    }

    @Transactional
    public void deleteExpense(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteExpense(id, user);
    }

    @Transactional
    public void deleteExpense(UUID id, User user) {
        Expense expense = expenseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        Account account = expense.getAccount();
        // Refund the amount back to the account balance
        Currency expenseCurrency = resolveExpenseCurrency(expense.getCurrency(), account);
        BigDecimal refundAmount = toAccountCurrencyAmount(expense.getAmount(), expenseCurrency, account);
        account.setBalance(account.getBalance().add(refundAmount));
        accountRepository.save(account);

        expenseRepository.delete(expense);
    }

    // ---- Helper methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .currency(expense.getCurrency() == null ? Currency.USD : expense.getCurrency())
                .description(expense.getDescription())
                .expenseDate(expense.getExpenseDate())
                .categoryId(expense.getCategory().getId())
                .categoryName(expense.getCategory().getName())
                .accountId(expense.getAccount().getId())
                .accountName(expense.getAccount().getName())
                .createdAt(expense.getCreatedAt())
                .build();
    }

    private Currency resolveExpenseCurrency(Currency requestedCurrency, Account account) {
        if (requestedCurrency != null) {
            return requestedCurrency;
        }
        if (account != null && account.getCurrency() != null) {
            return account.getCurrency();
        }
        return Currency.USD;
    }

    private BigDecimal toAccountCurrencyAmount(BigDecimal expenseAmount, Currency expenseCurrency, Account account) {
        Currency accountCurrency = account != null ? account.getCurrency() : null;
        return currencyConversionService.convert(
                expenseAmount == null ? BigDecimal.ZERO : expenseAmount,
                expenseCurrency,
                accountCurrency == null ? Currency.USD : accountCurrency);
    }
}
