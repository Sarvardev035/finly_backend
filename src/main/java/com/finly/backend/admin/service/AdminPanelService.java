package com.finly.backend.admin.service;

import com.finly.backend.admin.dto.AdminAccountForm;
import com.finly.backend.admin.dto.AdminDashboardStats;
import com.finly.backend.admin.dto.AdminUserForm;
import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Income;
import com.finly.backend.domain.model.Expense;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateAccountRequest;
import com.finly.backend.dto.request.UpdateAccountRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.exception.EmailAlreadyExistsException;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.*;
import com.finly.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class AdminPanelService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final DebtRepository debtRepository;
    private final TransferRepository transferRepository;
    private final NotificationRepository notificationRepository;

    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public AdminDashboardStats dashboardStats() {
        BigDecimal expenseTotal = expenseRepository.getTotalExpenseSum();
        BigDecimal incomeTotal = incomeRepository.getTotalIncomeSum();

        return new AdminDashboardStats(
                userRepository.count(),
                accountRepository.count(),
                expenseTotal == null ? BigDecimal.ZERO : expenseTotal,
                incomeTotal == null ? BigDecimal.ZERO : incomeTotal);
    }

    @Transactional(readOnly = true)
    public List<String> recentActivity() {
        List<TimedActivity> activity = new ArrayList<>();

        List<Expense> recentExpenses = expenseRepository.findTop10ByOrderByCreatedAtDesc();
        List<Income> recentIncomes = incomeRepository.findTop10ByOrderByCreatedAtDesc();

        recentExpenses.stream()
                .map(e -> new TimedActivity(e.getCreatedAt(),
                        "EXPENSE %s by %s".formatted(e.getAmount(), e.getUser().getEmail())))
                .forEach(activity::add);

        recentIncomes.stream()
                .map(i -> new TimedActivity(i.getCreatedAt(),
                        "INCOME %s by %s".formatted(i.getAmount(), i.getUser().getEmail())))
                .forEach(activity::add);

        return activity.stream()
                .sorted(Comparator.comparing(TimedActivity::at).reversed())
                .limit(10)
                .map(TimedActivity::message)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<User> findUsers(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable);
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User createUser(@Valid AdminUserForm form) {
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        User user = User.builder()
                .fullName(form.getFullName())
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(form.getRole())
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID userId, @Valid AdminUserForm form) {
        User user = getUser(userId);

        if (!user.getEmail().equalsIgnoreCase(form.getEmail()) && userRepository.existsByEmail(form.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setRole(form.getRole());

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Page<Account> findAccountsByUser(UUID userId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return accountRepository.findByUserId(userId, pageable);
        }
        return accountRepository.findByUserIdAndNameContainingIgnoreCase(userId, search, pageable);
    }

    @Transactional
    public AccountResponse createAccountForUser(UUID userId, @Valid AdminAccountForm form) {
        User user = getUser(userId);

        CreateAccountRequest request = CreateAccountRequest.builder()
                .name(form.getName())
                .type(form.getType())
                .currency(form.getCurrency())
                .initialBalance(form.getInitialBalance() == null ? BigDecimal.ZERO : form.getInitialBalance())
                .cardNumber(form.getCardNumber())
                .cardType(form.getCardType())
                .expiryDate(form.getExpiryDate())
                .build();

        return accountService.createAccount(request, user);
    }

    @Transactional
    public AccountResponse updateAccountForUser(UUID userId, UUID accountId, @Valid AdminAccountForm form) {
        User user = getUser(userId);

        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .name(form.getName())
                .currency(form.getCurrency())
                .build();

        return accountService.updateAccount(accountId, request, user);
    }

    @Transactional
    public void deleteAccountForUser(UUID userId, UUID accountId) {
        User user = getUser(userId);
        accountService.deleteAccount(accountId, user);
    }

    // --- GLOBAL CRUD ---

    @Transactional(readOnly = true)
    public Page<Account> findAllAccounts(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return accountRepository.findAll(pageable);
        return accountRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    @Transactional(readOnly = true)
    public Page<com.finly.backend.domain.model.Category> findAllCategories(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return categoryRepository.findAll(pageable);
        return categoryRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    @Transactional
    public com.finly.backend.domain.model.Category createCategory(
            @Valid com.finly.backend.admin.dto.AdminCategoryForm form) {
        com.finly.backend.domain.model.Category category = com.finly.backend.domain.model.Category.builder()
                .name(form.getName())
                .type(form.getType())
                .build();
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Page<Expense> findAllExpenses(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return expenseRepository.findAll(pageable);
        return expenseRepository.findByDescriptionContainingIgnoreCase(search, pageable);
    }

    @Transactional
    public void deleteExpense(UUID id) {
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Income> findAllIncomes(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return incomeRepository.findAll(pageable);
        return incomeRepository.findByDescriptionContainingIgnoreCase(search, pageable);
    }

    @Transactional
    public void deleteIncome(UUID id) {
        incomeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<com.finly.backend.domain.model.Budget> findAllBudgets(Pageable pageable) {
        return budgetRepository.findAll(pageable);
    }

    @Transactional
    public void deleteBudget(UUID id) {
        budgetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<com.finly.backend.domain.model.Debt> findAllDebts(String search, Pageable pageable) {
        if (search == null || search.isBlank())
            return debtRepository.findAll(pageable);
        return debtRepository.findByPersonNameContainingIgnoreCase(search, pageable);
    }

    @Transactional
    public void deleteDebt(UUID id) {
        debtRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<com.finly.backend.domain.model.Transfer> findAllTransfers(Pageable pageable) {
        return transferRepository.findAll(pageable);
    }

    @Transactional
    public void deleteTransfer(UUID id) {
        transferRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<com.finly.backend.domain.model.Notification> findAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }

    private record TimedActivity(java.time.LocalDateTime at, String message) {
    }
}
