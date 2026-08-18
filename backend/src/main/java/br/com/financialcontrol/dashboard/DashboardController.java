package br.com.financialcontrol.dashboard;

import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.dashboard.dto.DashboardResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DashboardController {

  private static final Set<String> ALLOWED_PARAMS =
      Set.of("startDate", "endDate", "year", "month", "months");

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping
  @Operation(summary = "Consultar o dashboard financeiro do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão derivada consolidada"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public DashboardResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer months) {
    rejectUnknownParams(request);
    return dashboardService.load(authenticatedUser, startDate, endDate, year, month, months);
  }

  private static void rejectUnknownParams(HttpServletRequest request) {
    for (String name : request.getParameterMap().keySet()) {
      if (!ALLOWED_PARAMS.contains(name)) {
        throw new InvalidRequestException(DashboardService.INVALID_DATA);
      }
    }
  }
}
