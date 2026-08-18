package br.com.financialcontrol.dashboard.dto;

import br.com.financialcontrol.receivables.dto.ReceivableSummaryResponse;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
    LocalDate asOfDate,
    LocalDate startDate,
    LocalDate endDate,
    DashboardBalanceResponse balance,
    DashboardProjectionResponse projection,
    DashboardPayablesResponse payables,
    ReceivableSummaryResponse receivables,
    List<DashboardAccountBalanceResponse> accounts,
    List<DashboardCreditCardResponse> creditCards) {}
