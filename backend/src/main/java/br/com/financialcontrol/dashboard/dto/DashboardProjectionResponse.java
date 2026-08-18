package br.com.financialcontrol.dashboard.dto;

import br.com.financialcontrol.projections.dto.ProjectionMonthResponse;
import br.com.financialcontrol.projections.dto.ProjectionQuarterResponse;
import br.com.financialcontrol.projections.dto.ProjectionSummaryResponse;
import java.util.List;

public record DashboardProjectionResponse(
    ProjectionSummaryResponse summary,
    List<ProjectionMonthResponse> months,
    List<ProjectionQuarterResponse> quarters) {}
