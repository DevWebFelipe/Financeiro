package br.com.financialcontrol.reports.dto;

import br.com.financialcontrol.projections.dto.ProjectionMonthResponse;
import br.com.financialcontrol.projections.dto.ProjectionQuarterResponse;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
import br.com.financialcontrol.projections.dto.ProjectionSummaryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CashFlowProjectedResponse(
    Boolean empty,
    ProjectionSummaryResponse summary,
    List<ProjectionMonthResponse> months,
    List<ProjectionQuarterResponse> quarters) {

  public static CashFlowProjectedResponse emptyProjected() {
    return new CashFlowProjectedResponse(true, null, null, null);
  }

  public static CashFlowProjectedResponse from(ProjectionResponse projection) {
    return new CashFlowProjectedResponse(
        null, projection.summary(), projection.months(), projection.quarters());
  }
}
