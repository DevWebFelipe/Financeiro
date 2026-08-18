package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.reports.CashFlowFlowType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

public record CashFlowResponse(
    ReportPeriodResponse period,
    CashFlowFlowType flowType,
    UUID accountId,
    @JsonInclude(JsonInclude.Include.NON_NULL) CashFlowHistoricalResponse historical,
    @JsonInclude(JsonInclude.Include.NON_NULL) CashFlowProjectedResponse projected) {}
