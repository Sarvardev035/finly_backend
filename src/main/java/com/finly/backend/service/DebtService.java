package com.finly.backend.service;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.Debt;
import com.finly.backend.domain.model.DebtStatus;
import com.finly.backend.domain.model.DebtType;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateDebtRequest;
import com.finly.backend.dto.request.RepaymentRequest;
import com.finly.backend.dto.response.DebtResponse;
import com.finly.backend.exception.AccountNotFoundException;
import com.finly.backend.exception.InsufficientBalanceException;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.AccountRepository;
import com.finly.backend.repository.DebtRepository;
import com.finly.backend.repository.UserRepository;
import com.finly.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrencyConversionService currencyConversionService;

    @Transactional
    public DebtResponse createDebt(CreateDebtRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createDebt(request, user);
    }

    @Transactional
    public DebtResponse createDebt(CreateDebtRequest request, User user) {
        Account account = null;
        Currency debtCurrency;
        if (request.getAccountId() != null) {
            account = accountRepository.findByIdAndUser(request.getAccountId(), user)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + request.getAccountId()));
            debtCurrency = request.getCurrency() != null ? request.getCurrency()
                    : (account.getCurrency() != null ? account.getCurrency() : Currency.USD);
        } else {
            debtCurrency = request.getCurrency() != null ? request.getCurrency() : Currency.USD;
        }

        // Apply initial account impact when account is specified.
        if (account != null) {
            BigDecimal accountAmount = currencyConversionService.convert(
                    request.getAmount(), debtCurrency, account.getCurrency() == null ? Currency.USD : account.getCurrency());
            if (request.getType() == DebtType.DEBT) {
                // Borrowed money comes into selected account.
                account.setBalance(account.getBalance().add(accountAmount));
            } else {
                // Receivable means user lent money to others -> leaves selected account.
                if (account.getBalance().compareTo(accountAmount) < 0) {
                    throw new InsufficientBalanceException("Insufficient balance in account " + account.getName()
                            + " for receivable creation.");
                }
                account.setBalance(account.getBalance().subtract(accountAmount));
            }
            accountRepository.save(account);
        }

        Debt debt = Debt.builder()
                .user(user)
                .personName(request.getPersonName())
                .type(request.getType())
                .amount(request.getAmount())
                .remainingAmount(request.getAmount()) // Initially remaining is total amount
                .currency(debtCurrency)
                .account(account)
                .description(request.getDescription())
                .status(DebtStatus.OPEN)
                .dueDate(request.getDueDate())
                .build();

        @SuppressWarnings("null")
        Debt savedDebt = debtRepository.save(debt);
        return mapToResponse(savedDebt);
    }

    @Transactional(readOnly = true)
    public List<DebtResponse> getDebts(DebtType type, DebtStatus status, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getDebts(type, status, user);
    }

    @Transactional(readOnly = true)
    public List<DebtResponse> getDebts(DebtType type, DebtStatus status, User user) {
        List<Debt> debts;

        if (type != null && status != null) {
            debts = debtRepository.findAllByUserAndTypeAndStatusOrderByDueDateAsc(user, type, status);
        } else if (type != null) {
            debts = debtRepository.findAllByUserAndTypeOrderByDueDateAsc(user, type);
        } else if (status != null) {
            debts = debtRepository.findAllByUserAndStatusOrderByDueDateAsc(user, status);
        } else {
            debts = debtRepository.findAllByUserOrderByDueDateAsc(user);
        }

        return debts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DebtResponse getDebtById(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getDebtById(id, user);
    }

    @Transactional(readOnly = true)
    public DebtResponse getDebtById(UUID id, User user) {
        Debt debt = debtRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Debt not found with id: " + id));
        return mapToResponse(debt);
    }

    @Transactional
    public DebtResponse repayDebt(UUID id, RepaymentRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return repayDebt(id, request, user);
    }

    @Transactional
    public DebtResponse repayDebt(UUID id, RepaymentRequest request, User user) {
        Debt debt = debtRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Debt not found with id: " + id));

        if (debt.getStatus() == DebtStatus.CLOSED) {
            throw new IllegalArgumentException("This debt is already fully repaid and closed.");
        }

        if (request.getPaymentAmount().compareTo(debt.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed the remaining debt amount.");
        }

        // If an account is provided, adjust its balance
        if (request.getAccountId() != null) {
            Account account = accountRepository.findByIdAndUser(request.getAccountId(), user)
                    .orElseThrow(
                            () -> new AccountNotFoundException("Account not found with id: " + request.getAccountId()));

            Currency debtCurrency = debt.getCurrency() == null ? Currency.USD : debt.getCurrency();
            Currency accountCurrency = account.getCurrency() == null ? Currency.USD : account.getCurrency();
            BigDecimal accountPayment = currencyConversionService.convert(request.getPaymentAmount(), debtCurrency,
                    accountCurrency);

            if (debt.getType() == DebtType.DEBT) {
                // You owe money: Repaying decreases your account balance
                if (account.getBalance().compareTo(accountPayment) < 0) {
                    throw new InsufficientBalanceException(
                            "Insufficient balance in account " + account.getName() + " to make this repayment.");
                }
                account.setBalance(account.getBalance().subtract(accountPayment));
            } else if (debt.getType() == DebtType.RECEIVABLE) {
                // Someone owes you money: Repayment increases your account balance
                account.setBalance(account.getBalance().add(accountPayment));
            }
            @SuppressWarnings({ "null", "unused" })
            Account savedAccount = accountRepository.save(account);
        }

        // Adjust remaining debt amount
        debt.setRemainingAmount(debt.getRemainingAmount().subtract(request.getPaymentAmount()));

        // Close debt if remaining amount is zero
        if (debt.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            debt.setStatus(DebtStatus.CLOSED);
        }

        @SuppressWarnings("null")
        Debt savedDebt = debtRepository.save(debt);
        return mapToResponse(savedDebt);
    }

    @Transactional
    public void deleteDebt(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteDebt(id, user);
    }

    @Transactional
    public void deleteDebt(UUID id, User user) {
        Debt debt = debtRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Debt not found with id: " + id));

        // Prevent deletion if repayment has already started
        if (debt.getRemainingAmount().compareTo(debt.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Cannot delete a debt that has partial or full repayments. Try closing or fully repaying it instead.");
        }

        // Revert initial account impact if debt was linked to an account and has no repayments.
        if (debt.getAccount() != null) {
            Account account = debt.getAccount();
            Currency debtCurrency = debt.getCurrency() == null ? Currency.USD : debt.getCurrency();
            Currency accountCurrency = account.getCurrency() == null ? Currency.USD : account.getCurrency();
            BigDecimal accountAmount = currencyConversionService.convert(debt.getAmount(), debtCurrency, accountCurrency);

            if (debt.getType() == DebtType.DEBT) {
                // Reverse initial credit
                if (account.getBalance().compareTo(accountAmount) < 0) {
                    throw new InsufficientBalanceException(
                            "Cannot delete debt because account " + account.getName()
                                    + " does not have enough balance to rollback.");
                }
                account.setBalance(account.getBalance().subtract(accountAmount));
            } else {
                // Reverse initial debit
                account.setBalance(account.getBalance().add(accountAmount));
            }
            accountRepository.save(account);
        }

        debtRepository.delete(debt);
    }

    // ---- Helper methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private DebtResponse mapToResponse(Debt debt) {
        return DebtResponse.builder()
                .id(debt.getId())
                .personName(debt.getPersonName())
                .type(debt.getType())
                .amount(debt.getAmount())
                .remainingAmount(debt.getRemainingAmount())
                .currency(debt.getCurrency() == null ? Currency.USD : debt.getCurrency())
                .accountId(debt.getAccount() != null ? debt.getAccount().getId() : null)
                .accountName(debt.getAccount() != null ? debt.getAccount().getName() : null)
                .description(debt.getDescription())
                .status(debt.getStatus())
                .dueDate(debt.getDueDate())
                .createdAt(debt.getCreatedAt())
                .build();
    }
}
