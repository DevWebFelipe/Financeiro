package br.com.financialcontrol.credit_cards.dto;

import java.math.BigDecimal;

public record CreditCardLimitResponse(
    BigDecimal creditLimit, BigDecimal usedLimit, BigDecimal availableLimit) {}
