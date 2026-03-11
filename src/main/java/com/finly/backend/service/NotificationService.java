package com.finly.backend.service;

import com.finly.backend.domain.model.*;
import com.finly.backend.dto.response.BudgetResponse;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.BudgetRepository;
import com.finly.backend.repository.CategoryRepository;
import com.finly.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    private final com.finly.backend.repository.DebtRepository debtRepository;
    private static final BigDecimal WARNING_THRESHOLD_PERCENT = new BigDecimal("80");

    // Simple AI/Keyword mapping for auto-categorization
    private static final Map<String, String> KEYWORD_TO_CATEGORY = new HashMap<>();

    static {
        KEYWORD_TO_CATEGORY.put("KORZINKA", "Grocery");
        KEYWORD_TO_CATEGORY.put("MAKRO", "Grocery");
        KEYWORD_TO_CATEGORY.put("BENZIN", "Transport");
        KEYWORD_TO_CATEGORY.put("YANDEX", "Taxi/Transport");
        KEYWORD_TO_CATEGORY.put("PAYME", "Utility");
        KEYWORD_TO_CATEGORY.put("CLICK", "Utility");
        KEYWORD_TO_CATEGORY.put("RESTORAN", "Food & Drink");
        KEYWORD_TO_CATEGORY.put("CAFE", "Food & Drink");
        KEYWORD_TO_CATEGORY.put("APTEKA", "Health");
    }

    @Transactional
    public void autoCategorizeExpense(Expense expense) {
        String desc = expense.getDescription().toUpperCase();
        String suggestedCategoryName = null;

        for (Map.Entry<String, String> entry : KEYWORD_TO_CATEGORY.entrySet()) {
            if (desc.contains(entry.getKey())) {
                suggestedCategoryName = entry.getValue();
                break;
            }
        }

        if (suggestedCategoryName != null) {
            final String finalCategoryName = suggestedCategoryName;
            List<Category> userCategories = categoryRepository.findAllByUser(expense.getUser());

            userCategories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(finalCategoryName))
                    .findFirst()
                    .ifPresent(category -> {
                        expense.setCategory(category);
                        createNotification(expense.getUser(), NotificationType.CATEGORY_DETECT,
                                "AI detected category '" + category.getName() + "' for your expense: "
                                        + expense.getDescription(),
                                expense.getId());
                    });
        }
    }

    @Transactional
    public void checkOverspending(Expense expense) {
        User user = expense.getUser();
        LocalDate now = LocalDate.now();

        // Check category budget
        budgetRepository.findByUserAndTypeAndCategoryAndYearAndMonth(user, BudgetType.EXPENSE, expense.getCategory(),
                now.getYear(), now.getMonthValue())
                .ifPresent(budget -> {
                    BudgetResponse stats = budgetService.calculateBudgetStats(budget, user);
                    notifyBudgetStatus(user, stats, "category '" + expense.getCategory().getName() + "'",
                            expense.getId());
                });

        // Check overall budget
        budgetRepository
                .findByUserAndTypeAndCategoryIsNullAndYearAndMonth(user, BudgetType.EXPENSE, now.getYear(),
                        now.getMonthValue())
                .ifPresent(budget -> {
                    BudgetResponse stats = budgetService.calculateBudgetStats(budget, user);
                    notifyBudgetStatus(user, stats, "overall monthly budget", expense.getId());
                });
    }

    private void notifyBudgetStatus(User user, BudgetResponse stats, String budgetLabel, UUID expenseId) {
        if (stats == null) {
            return;
        }

        BigDecimal remaining = stats.getRemainingAmount() == null ? BigDecimal.ZERO : stats.getRemainingAmount();
        BigDecimal percentageUsed = stats.getPercentageUsed() == null ? BigDecimal.ZERO : stats.getPercentageUsed();

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            createNotification(user, NotificationType.OVERSPENDING,
                    "Budget limit exceeded for " + budgetLabel + ". Remaining: " + remaining,
                    expenseId);
            return;
        }

        if (percentageUsed.compareTo(WARNING_THRESHOLD_PERCENT) >= 0) {
            createNotification(user, NotificationType.BUDGET_WARNING,
                    "Budget usage for " + budgetLabel + " reached " + percentageUsed + "%. Remaining: " + remaining,
                    expenseId);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(User user) {
        return notificationRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(UUID id, User user) {
        Notification notification = notificationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    @Transactional
    public void sendPaymentReminders() {
        log.info("Running scheduled payment reminders...");
        LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);

        List<Debt> upcomingDebts = debtRepository.findAllByStatusAndDueDateLessThanEqual(DebtStatus.OPEN,
                threeDaysFromNow);

        for (Debt debt : upcomingDebts) {
            String msg = String.format("Reminder: You have a %s of %s due on %s to %s.",
                    debt.getType() == DebtType.DEBT ? "debt" : "receivable",
                    debt.getRemainingAmount(),
                    debt.getDueDate(),
                    debt.getPersonName());

            createNotification(debt.getUser(), NotificationType.PAYMENT_REMINDER, msg, null);
        }
    }

    private void createNotification(User user, NotificationType type, String message, UUID expenseId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .relatedExpenseId(expenseId)
                .build();
        @SuppressWarnings({ "null", "unused" })
        Notification savedNotification = notificationRepository.save(notification);
    }
}
