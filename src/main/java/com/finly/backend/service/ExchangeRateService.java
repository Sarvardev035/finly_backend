package com.finly.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.ExchangeRate;
import com.finly.backend.domain.model.ExchangeRateSource;
import com.finly.backend.dto.request.ExchangeRateUpsertRequest;
import com.finly.backend.dto.response.ExchangeRateResponse;
import com.finly.backend.exception.ResourceNotFoundException;
import com.finly.backend.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String EXTERNAL_BASE_URL = "https://api.exchangerate.host";
    private static final Map<String, BigDecimal> FALLBACK = Map.of(
            "USD->EUR", new BigDecimal("0.9174"),
            "EUR->USD", new BigDecimal("1.0900"),
            "USD->UZS", new BigDecimal("12658.2278"),
            "UZS->USD", new BigDecimal("0.000079"),
            "EUR->UZS", new BigDecimal("13797.4684"),
            "UZS->EUR", new BigDecimal("0.000072"));

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> listEffectiveRates() {
        List<Currency> currencies = Arrays.asList(Currency.USD, Currency.EUR, Currency.UZS);
        List<ExchangeRateResponse> result = new ArrayList<>();
        for (Currency base : currencies) {
            for (Currency target : currencies) {
                if (base == target) {
                    continue;
                }
                result.add(ExchangeRateResponse.builder()
                        .baseCurrency(base)
                        .targetCurrency(target)
                        .rate(getRate(base, target))
                        .source(resolveSource(base, target))
                        .createdAt(resolveTimestamp(base, target))
                        .build());
            }
        }
        return result;
    }

    @Transactional
    public ExchangeRateResponse createAdminRate(ExchangeRateUpsertRequest request) {
        if (request.getBaseCurrency() == request.getTargetCurrency()) {
            throw new IllegalArgumentException("Base and target currencies cannot be the same");
        }
        ExchangeRate saved = exchangeRateRepository.save(ExchangeRate.builder()
                .baseCurrency(request.getBaseCurrency())
                .targetCurrency(request.getTargetCurrency())
                .rate(request.getRate())
                .source(ExchangeRateSource.ADMIN)
                .build());
        return map(saved);
    }

    @Transactional
    public ExchangeRateResponse updateAdminRate(UUID id, ExchangeRateUpsertRequest request) {
        ExchangeRate rate = exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange rate not found: " + id));
        if (request.getBaseCurrency() == request.getTargetCurrency()) {
            throw new IllegalArgumentException("Base and target currencies cannot be the same");
        }
        rate.setBaseCurrency(request.getBaseCurrency());
        rate.setTargetCurrency(request.getTargetCurrency());
        rate.setRate(request.getRate());
        rate.setSource(ExchangeRateSource.ADMIN);
        return map(exchangeRateRepository.save(rate));
    }

    @Transactional
    public List<ExchangeRateResponse> refreshFromInternet() {
        List<Currency> all = Arrays.asList(Currency.USD, Currency.EUR, Currency.UZS);
        List<ExchangeRateResponse> saved = new ArrayList<>();

        for (Currency base : all) {
            String symbols = all.stream()
                    .filter(c -> c != base)
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            JsonNode root = webClientBuilder.baseUrl(EXTERNAL_BASE_URL).build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/latest")
                            .queryParam("base", base.name())
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("rates")) {
                continue;
            }

            JsonNode ratesNode = root.get("rates");
            for (Currency target : all) {
                if (target == base) {
                    continue;
                }
                JsonNode valueNode = ratesNode.get(target.name());
                if (valueNode == null || !valueNode.isNumber()) {
                    continue;
                }
                BigDecimal rate = valueNode.decimalValue().setScale(8, RoundingMode.HALF_UP);
                ExchangeRate entity = exchangeRateRepository.save(ExchangeRate.builder()
                        .baseCurrency(base)
                        .targetCurrency(target)
                        .rate(rate)
                        .source(ExchangeRateSource.API)
                        .build());
                saved.add(map(entity));
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public BigDecimal getRate(Currency base, Currency target) {
        if (base == null || target == null || base == target) {
            return BigDecimal.ONE;
        }
        // 1) Direct ADMIN override
        Optional<BigDecimal> directAdmin = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(
                        base, target, ExchangeRateSource.ADMIN)
                .map(ExchangeRate::getRate);
        if (directAdmin.isPresent()) {
            return directAdmin.get();
        }

        // 2) Reverse ADMIN override (inverse)
        Optional<BigDecimal> reverseAdmin = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(
                        target, base, ExchangeRateSource.ADMIN)
                .map(ExchangeRate::getRate);
        if (reverseAdmin.isPresent() && reverseAdmin.get().compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.ONE.divide(reverseAdmin.get(), 8, RoundingMode.HALF_UP);
        }

        // 3) Direct API rate
        Optional<BigDecimal> directApi = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(
                        base, target, ExchangeRateSource.API)
                .map(ExchangeRate::getRate);
        if (directApi.isPresent()) {
            return directApi.get();
        }

        // 4) Reverse API rate (inverse)
        Optional<BigDecimal> reverseApi = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(
                        target, base, ExchangeRateSource.API)
                .map(ExchangeRate::getRate);
        if (reverseApi.isPresent() && reverseApi.get().compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.ONE.divide(reverseApi.get(), 8, RoundingMode.HALF_UP);
        }

        // 5) Last fallback
        return FALLBACK.getOrDefault(base.name() + "->" + target.name(), BigDecimal.ONE);
    }

    @Transactional(readOnly = true)
    public ExchangeRateSource resolveSource(Currency base, Currency target) {
        if (base == target) {
            return ExchangeRateSource.API;
        }
        boolean hasDirectAdmin = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(base, target, ExchangeRateSource.ADMIN)
                .isPresent();
        boolean hasReverseAdmin = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(target, base, ExchangeRateSource.ADMIN)
                .isPresent();
        if (hasDirectAdmin || hasReverseAdmin) {
            return ExchangeRateSource.ADMIN;
        }
        return ExchangeRateSource.API;
    }

    @Transactional(readOnly = true)
    public LocalDateTime resolveTimestamp(Currency base, Currency target) {
        if (base == target) {
            return LocalDateTime.now();
        }
        return exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(base, target, ExchangeRateSource.ADMIN)
                .map(ExchangeRate::getCreatedAt)
                .or(() -> exchangeRateRepository
                        .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(target, base,
                                ExchangeRateSource.ADMIN)
                        .map(ExchangeRate::getCreatedAt))
                .or(() -> exchangeRateRepository
                        .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(base, target,
                                ExchangeRateSource.API)
                        .map(ExchangeRate::getCreatedAt))
                .or(() -> exchangeRateRepository
                        .findTopByBaseCurrencyAndTargetCurrencyAndSourceOrderByCreatedAtDesc(target, base,
                                ExchangeRateSource.API)
                        .map(ExchangeRate::getCreatedAt))
                .orElse(LocalDateTime.now());
    }

    private ExchangeRateResponse map(ExchangeRate rate) {
        return ExchangeRateResponse.builder()
                .id(rate.getId())
                .baseCurrency(rate.getBaseCurrency())
                .targetCurrency(rate.getTargetCurrency())
                .rate(rate.getRate())
                .source(rate.getSource())
                .createdAt(rate.getCreatedAt())
                .build();
    }
}
