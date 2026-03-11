package com.finly.backend.service;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateAccountRequest;
import com.finly.backend.dto.request.UpdateAccountRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.exception.AccountDeletionException;
import com.finly.backend.exception.AccountNotFoundException;
import com.finly.backend.exception.AccountInUseException;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.repository.AccountRepository;
import com.finly.backend.repository.ExpenseRepository;
import com.finly.backend.repository.IncomeRepository;
import com.finly.backend.repository.TransferRepository;
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
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createAccount(request, user);
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, User user) {
        // Validate card-specific fields when type is BANK_CARD
        if (request.getType() == AccountType.BANK_CARD) {
            if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
                throw new IllegalArgumentException("Card number is required for BANK_CARD accounts.");
            }
            if (request.getCardType() == null) {
                throw new IllegalArgumentException("Card type is required for BANK_CARD accounts.");
            }
        }

        Account account = Account.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .currency(request.getCurrency())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .cardNumber(request.getCardNumber())
                .cardType(request.getCardType())
                .expiryDate(request.getExpiryDate())
                .build();

        return mapToResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(UserDetailsImpl principal) {
        User user = getUser(principal);
        return getUserAccounts(user);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(User user) {
        return accountRepository.findAllByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getAccountById(id, user);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id, User user) {
        Account account = accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return updateAccount(id, request, user);
    }

    @Transactional
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request, User user) {
        Account account = accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            account.setName(request.getName());
        }
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }

        return mapToResponse(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteAccount(id, user);
    }

    @Transactional
    public void deleteAccount(UUID id, User user) {
        Account account = accountRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));

        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountDeletionException(
                    "Account cannot be deleted because balance is not zero: " + account.getBalance()
                            + ". Please transfer or spend the balance first.");
        }

        if (expenseRepository.existsByAccountId(account.getId())
                || incomeRepository.existsByAccountId(account.getId())
                || transferRepository.existsByFromAccountId(account.getId())
                || transferRepository.existsByToAccountId(account.getId())) {
            throw new AccountInUseException("Cannot delete account because it is used in transactions.");
        }

        accountRepository.delete(account);
    }

    // ---- Helper methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .cardNumber(account.getCardNumber())
                .cardType(account.getCardType())
                .expiryDate(account.getExpiryDate())
                .createdAt(account.getCreatedAt())
                .userFullName(account.getUser() != null ? account.getUser().getFullName() : null)
                .build();
    }
}
