package com.finly.backend.service;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Category;
import com.finly.backend.domain.model.CategoryType;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.Income;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateIncomeRequest;
import com.finly.backend.dto.request.UpdateIncomeRequest;
import com.finly.backend.dto.response.IncomeResponse;
import com.finly.backend.exception.AccountNotFoundException;
import com.finly.backend.exception.CategoryNotFoundException;
import com.finly.backend.exception.InsufficientBalanceException;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.AccountRepository;
import com.finly.backend.repository.CategoryRepository;
import com.finly.backend.repository.IncomeRepository;
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
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CurrencyConversionService currencyConversionService;

    @Transactional
    public IncomeResponse createIncome(CreateIncomeRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createIncome(request, user);
    }

    @Transactional
    public IncomeResponse createIncome(CreateIncomeRequest request, User user) {

        Account account = accountRepository.findByIdAndUser(request.getAccountId(), user)
                .orElseThrow(
                        () -> new AccountNotFoundException("Account not found with id: " + request.getAccountId()));

        Category category = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(
                        () -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException(
                    "Category must be of type INCOME. Selected category is " + category.getType());
        }

        Currency incomeCurrency = resolveIncomeCurrency(request.getCurrency(), account);
        BigDecimal creditAmount = toAccountCurrencyAmount(request.getAmount(), incomeCurrency, account);

        // Increase account balance
        account.setBalance(account.getBalance().add(creditAmount));
        accountRepository.save(account);

        Income income = Income.builder()
                .user(user)
                .account(account)
                .category(category)
                .amount(request.getAmount())
                .currency(incomeCurrency)
                .description(request.getDescription())
                .incomeDate(request.getIncomeDate())
                .build();

        @SuppressWarnings("null")
        Income savedIncome = incomeRepository.save(income);
        return mapToResponse(savedIncome);
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> getIncomes(UUID accountId, UUID categoryId, LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);
        return getIncomes(accountId, categoryId, startDate, endDate, user);
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> getIncomes(UUID accountId, UUID categoryId, LocalDate startDate, LocalDate endDate,
            User user) {

        List<Income> incomes;

        if (startDate != null && endDate != null) {
            incomes = incomeRepository.findAllByUserAndIncomeDateBetweenOrderByIncomeDateDesc(user, startDate, endDate);
        } else if (accountId != null) {
            Account account = accountRepository.findByIdAndUser(accountId, user)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
            incomes = incomeRepository.findAllByUserAndAccountOrderByIncomeDateDesc(user, account);
        } else if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUser(categoryId, user)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            incomes = incomeRepository.findAllByUserAndCategoryOrderByIncomeDateDesc(user, category);
        } else {
            incomes = incomeRepository.findAllByUserOrderByIncomeDateDesc(user);
        }

        return incomes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getIncomeById(id, user);
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(UUID id, User user) {
        Income income = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + id));
        return mapToResponse(income);
    }

    @Transactional
    public IncomeResponse updateIncome(UUID id, UpdateIncomeRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return updateIncome(id, request, user);
    }

    @Transactional
    public IncomeResponse updateIncome(UUID id, UpdateIncomeRequest request, User user) {
        Income income = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + id));

        Account oldAccount = income.getAccount();
        Currency oldIncomeCurrency = resolveIncomeCurrency(income.getCurrency(), oldAccount);
        BigDecimal oldCreditAmount = toAccountCurrencyAmount(income.getAmount(), oldIncomeCurrency, oldAccount);
        BigDecimal newAmount = request.getAmount() != null ? request.getAmount() : income.getAmount();
        Currency newIncomeCurrency = request.getCurrency() != null ? request.getCurrency() : oldIncomeCurrency;
        Account targetAccount = oldAccount;

        if (request.getAccountId() != null && !request.getAccountId().equals(oldAccount.getId())) {
            targetAccount = accountRepository.findByIdAndUser(request.getAccountId(), user)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + request.getAccountId()));

            // Revert old credit from old account first
            if (oldAccount.getBalance().compareTo(oldCreditAmount) < 0) {
                throw new InsufficientBalanceException(
                        "Cannot move income: source account has insufficient balance to reverse old income.");
            }
            oldAccount.setBalance(oldAccount.getBalance().subtract(oldCreditAmount));
            accountRepository.save(oldAccount);

            // Apply new credit to target account using target account currency
            BigDecimal targetCreditAmount = toAccountCurrencyAmount(newAmount, newIncomeCurrency, targetAccount);
            targetAccount.setBalance(targetAccount.getBalance().add(targetCreditAmount));
            accountRepository.save(targetAccount);
            income.setAccount(targetAccount);
        } else {
            BigDecimal newCreditAmount = toAccountCurrencyAmount(newAmount, newIncomeCurrency, oldAccount);
            BigDecimal delta = newCreditAmount.subtract(oldCreditAmount);
            // If delta is negative, we are reducing credited amount and must ensure current balance covers it.
            if (delta.compareTo(BigDecimal.ZERO) < 0 && oldAccount.getBalance().compareTo(delta.abs()) < 0) {
                throw new InsufficientBalanceException(
                        "Cannot decrease income amount: Account would have a negative balance.");
            }
            oldAccount.setBalance(oldAccount.getBalance().add(delta));
            accountRepository.save(oldAccount);
        }

        if (request.getCategoryId() != null && !request.getCategoryId().equals(income.getCategory().getId())) {
            Category newCategory = categoryRepository.findByIdAndUser(request.getCategoryId(), user)
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            if (newCategory.getType() != CategoryType.INCOME) {
                throw new IllegalArgumentException(
                        "Category must be of type INCOME. Selected category is " + newCategory.getType());
            }
            income.setCategory(newCategory);
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            income.setDescription(request.getDescription());
        }

        income.setAmount(newAmount);
        income.setCurrency(newIncomeCurrency);

        if (request.getIncomeDate() != null) {
            income.setIncomeDate(request.getIncomeDate());
        }

        @SuppressWarnings("null")
        Income savedIncome = incomeRepository.save(income);
        return mapToResponse(savedIncome);
    }

    @Transactional
    public void deleteIncome(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteIncome(id, user);
    }

    @Transactional
    public void deleteIncome(UUID id, User user) {
        Income income = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + id));

        Account account = income.getAccount();
        Currency incomeCurrency = resolveIncomeCurrency(income.getCurrency(), account);
        BigDecimal debitAmount = toAccountCurrencyAmount(income.getAmount(), incomeCurrency, account);

        // Ensure that deleting the income doesn't make the account balance negative
        if (account.getBalance().compareTo(debitAmount) < 0) {
            throw new InsufficientBalanceException("Cannot delete income: Account would have a negative balance.");
        }

        // Subtract the amount back from the account balance
        account.setBalance(account.getBalance().subtract(debitAmount));
        accountRepository.save(account);

        incomeRepository.delete(income);
    }

    // ---- Helper methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private IncomeResponse mapToResponse(Income income) {
        return IncomeResponse.builder()
                .id(income.getId())
                .amount(income.getAmount())
                .currency(income.getCurrency() == null ? Currency.USD : income.getCurrency())
                .description(income.getDescription())
                .incomeDate(income.getIncomeDate())
                .categoryId(income.getCategory().getId())
                .categoryName(income.getCategory().getName())
                .accountId(income.getAccount().getId())
                .accountName(income.getAccount().getName())
                .createdAt(income.getCreatedAt())
                .build();
    }

    private Currency resolveIncomeCurrency(Currency requestedCurrency, Account account) {
        if (requestedCurrency != null) {
            return requestedCurrency;
        }
        if (account != null && account.getCurrency() != null) {
            return account.getCurrency();
        }
        return Currency.USD;
    }

    private BigDecimal toAccountCurrencyAmount(BigDecimal incomeAmount, Currency incomeCurrency, Account account) {
        Currency accountCurrency = account != null ? account.getCurrency() : null;
        return currencyConversionService.convert(
                incomeAmount == null ? BigDecimal.ZERO : incomeAmount,
                incomeCurrency,
                accountCurrency == null ? Currency.USD : accountCurrency);
    }
}
