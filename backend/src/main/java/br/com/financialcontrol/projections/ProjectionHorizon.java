package br.com.financialcontrol.projections;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record ProjectionHorizon(
    LocalDate asOfDate, LocalDate startDate, LocalDate endDate, List<YearMonth> months) {}
