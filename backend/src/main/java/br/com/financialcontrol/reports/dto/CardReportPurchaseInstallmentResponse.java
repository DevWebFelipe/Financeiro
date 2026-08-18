package br.com.financialcontrol.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardReportPurchaseInstallmentResponse(
    int installmentNumber, LocalDate dueDate, BigDecimal amount) {}
