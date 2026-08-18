package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.expenses.ResponsibleType;
import java.math.BigDecimal;

public record InvoiceReportResponsibleGroupResponse(
    ResponsibleType responsibleType, String responsibleName, BigDecimal original) {}
