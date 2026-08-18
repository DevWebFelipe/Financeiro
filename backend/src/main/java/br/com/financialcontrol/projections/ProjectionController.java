package br.com.financialcontrol.projections;

import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.projections.dto.ProjectionResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projections")
@Tag(name = "Projections")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ProjectionController {

  private static final Set<String> ALLOWED_PARAMS =
      Set.of("startDate", "endDate", "year", "month", "months", "accountId", "page", "size");

  private final ProjectionService projectionService;

  public ProjectionController(ProjectionService projectionService) {
    this.projectionService = projectionService;
  }

  @GetMapping
  @Operation(summary = "Consultar projeção de fluxo de caixa do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Projeção derivada do saldo atual"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public ProjectionResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer months,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request);
    return projectionService.project(
        authenticatedUser, startDate, endDate, year, month, months, accountId, page, size);
  }

  private static void rejectUnknownParams(HttpServletRequest request) {
    for (String name : request.getParameterMap().keySet()) {
      if (!ALLOWED_PARAMS.contains(name)) {
        throw new InvalidRequestException(ProjectionService.INVALID_DATA);
      }
    }
  }
}
