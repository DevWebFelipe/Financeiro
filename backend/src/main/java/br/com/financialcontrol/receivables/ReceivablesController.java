package br.com.financialcontrol.receivables;

import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.receivables.dto.ReceivablePageResponse;
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
@RequestMapping("/api/v1/receivables")
@Tag(name = "Receivables")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ReceivablesController {

  private static final Set<String> ALLOWED_PARAMS =
      Set.of(
          "startDate",
          "endDate",
          "dateType",
          "status",
          "overdue",
          "categoryId",
          "accountId",
          "responsibleType",
          "responsibleName",
          "sort",
          "direction",
          "page",
          "size");

  private final ReceivablesService receivablesService;

  public ReceivablesController(ReceivablesService receivablesService) {
    this.receivablesService = receivablesService;
  }

  @GetMapping
  @Operation(summary = "Listar contas a receber do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão consolidada de duplicatas"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public ReceivablePageResponse list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String dateType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID accountId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String responsibleName,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request);
    return receivablesService.list(
        authenticatedUser,
        startDate,
        endDate,
        dateType,
        status,
        overdue,
        categoryId,
        accountId,
        responsibleType,
        responsibleName,
        sort,
        direction,
        page,
        size);
  }

  private static void rejectUnknownParams(HttpServletRequest request) {
    for (String name : request.getParameterMap().keySet()) {
      if (!ALLOWED_PARAMS.contains(name)) {
        throw new InvalidRequestException(ReceivablesService.INVALID_DATA);
      }
    }
  }
}
