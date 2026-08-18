package br.com.financialcontrol.projections.dto;

import java.time.LocalDate;
import java.util.List;

public record ProjectionResponse(
    LocalDate startDate,
    LocalDate endDate,
    ProjectionSummaryResponse summary,
    List<ProjectionMonthResponse> months,
    List<ProjectionQuarterResponse> quarters,
    ProjectionEventPageResponse events,
    List<ProjectionEventResponse> undatedEvents) {}
