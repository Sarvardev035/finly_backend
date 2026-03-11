package com.finly.backend.service;

import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.Transfer;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateTransferRequest;
import com.finly.backend.dto.response.TransferResponse;
import com.finly.backend.exception.AccountNotFoundException;
import com.finly.backend.exception.InsufficientBalanceException;
import com.finly.backend.exception.InvalidCredentialsException;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.AccountRepository;
import com.finly.backend.repository.TransferRepository;
import com.finly.backend.repository.UserRepository;
import com.finly.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request, UserDetailsImpl principal) {
        User user = getUser(principal);
        return createTransfer(request, user);
    }

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request, User user) {

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        Account fromAccount = accountRepository.findByIdAndUser(request.getFromAccountId(), user)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Source account not found with id: " + request.getFromAccountId()));

        Account toAccount = accountRepository.findByIdAndUser(request.getToAccountId(), user)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Destination account not found with id: " + request.getToAccountId()));

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account: " + fromAccount.getName());
        }

        Currency fromCurrency = fromAccount.getCurrency() == null ? Currency.USD : fromAccount.getCurrency();
        Currency toCurrency = toAccount.getCurrency() == null ? Currency.USD : toAccount.getCurrency();

        BigDecimal exchangeRate;
        if (fromCurrency != toCurrency) {
            // Always use effective rate from /api/exchange-rates (ADMIN override > API)
            exchangeRate = exchangeRateService.getRate(fromCurrency, toCurrency).setScale(8, RoundingMode.HALF_UP);
        } else {
            exchangeRate = BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP);
        }
        BigDecimal convertedAmount = request.getAmount().multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);

        // Deduct from source and save
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);

        // Add to destination and save
        toAccount.setBalance(toAccount.getBalance().add(convertedAmount));
        accountRepository.save(toAccount);

        Transfer transfer = Transfer.builder()
                .user(user)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .description(request.getDescription())
                .transferDate(request.getTransferDate())
                .build();

        @SuppressWarnings("null")
        Transfer savedTransfer = transferRepository.save(transfer);
        return mapToResponse(savedTransfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfers(UUID accountId, LocalDate startDate, LocalDate endDate,
            UserDetailsImpl principal) {
        User user = getUser(principal);
        return getTransfers(accountId, startDate, endDate, user);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfers(UUID accountId, LocalDate startDate, LocalDate endDate,
            User user) {

        List<Transfer> transfers;

        if (startDate != null && endDate != null) {
            transfers = transferRepository.findAllByUserAndTransferDateBetweenOrderByTransferDateDesc(user, startDate,
                    endDate);
        } else if (accountId != null) {
            Account account = accountRepository.findByIdAndUser(accountId, user)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found for filtering: " + accountId));
            transfers = transferRepository.findAllByUserAndAccountOrderByTransferDateDesc(user, account);
        } else {
            transfers = transferRepository.findAllByUserOrderByTransferDateDesc(user);
        }

        return transfers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferById(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        return getTransferById(id, user);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferById(UUID id, User user) {
        Transfer transfer = transferRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with id: " + id));
        return mapToResponse(transfer);
    }

    @Transactional
    public void deleteTransfer(UUID id, UserDetailsImpl principal) {
        User user = getUser(principal);
        deleteTransfer(id, user);
    }

    @Transactional
    public void deleteTransfer(UUID id, User user) {
        Transfer transfer = transferRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with id: " + id));

        Account fromAccount = transfer.getFromAccount();
        Account toAccount = transfer.getToAccount();

        // Verify that reverting doesn't result in negative balance for the destination
        // account
        if (toAccount.getBalance().compareTo(transfer.getConvertedAmount()) < 0) {
            throw new InsufficientBalanceException("Cannot delete transfer because destination account ("
                    + toAccount.getName() + ") has insufficient funds to refund.");
        }

        // Refund original amount back to source account
        fromAccount.setBalance(fromAccount.getBalance().add(transfer.getAmount()));

        // Subtract converted amount from destination account
        toAccount.setBalance(toAccount.getBalance().subtract(transfer.getConvertedAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transferRepository.delete(transfer);
    }

    // ---- Helper methods ----

    private User getUser(UserDetailsImpl principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found."));
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromAccountId(transfer.getFromAccount().getId())
                .fromAccountName(transfer.getFromAccount().getName())
                .toAccountId(transfer.getToAccount().getId())
                .toAccountName(transfer.getToAccount().getName())
                .amount(transfer.getAmount())
                .convertedAmount(transfer.getConvertedAmount())
                .exchangeRate(transfer.getExchangeRate())
                .description(transfer.getDescription())
                .transferDate(transfer.getTransferDate())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
