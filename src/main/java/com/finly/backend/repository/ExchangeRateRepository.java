package com.finly.backend.repository;

import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.ExchangeRate;
import com.finly.backend.domain.model.ExchangeRateSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    Optional<ExchangeRate> findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(
            Currency baseCurrency, Currency targetCurrency, ExchangeRateSource source);

    List<ExchangeRate> findAllByOrderByCreatedAtDesc();
}
