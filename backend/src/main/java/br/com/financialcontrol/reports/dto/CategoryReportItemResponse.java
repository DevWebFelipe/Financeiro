package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.categories.CategoryType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryReportItemResponse(
    UUID categoryId,
    String name,
    CategoryType type,
    boolean active,
    BigDecimal periodOriginal,
    BigDecimal periodDiscount,
    BigDecimal periodSurcharge,
    BigDecimal periodObligation,
    BigDecimal periodPaid,
    BigDecimal periodRemaining,
    BigDecimal amount,
    BigDecimal accruedAmount,
    BigDecimal receivedAmount,
    BigDecimal remainingAmount,
    BigDecimal periodReceivedAmount) {}
