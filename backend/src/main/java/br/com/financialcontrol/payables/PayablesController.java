package br.com.financialcontrol.payables;

import br.com.financialcontrol.config.InvalidRequestException;
import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.payables.dto.PayablePageResponse;
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
@RequestMapping("/api/v1/payables")
@Tag(name = "Payables")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class PayablesController {

  private static final Set<String> ALLOWED_PARAMS =
      Set.of(
          "startDate",
          "endDate",
          "year",
          "month",
          "includeWithoutDueDate",
          "status",
          "overdue",
          "creditCardId",
          "withoutCreditCard",
          "categoryId",
          "responsibleType",
          "search",
          "sort",
          "direction",
          "page",
          "size");

  private final PayablesService payablesService;

  public PayablesController(PayablesService payablesService) {
    this.payablesService = payablesService;
  }

  @GetMapping
  @Operation(summary = "Listar contas a pagar do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Visão consolidada de obrigações"),
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public PayablePageResponse list(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      HttpServletRequest request,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(defaultValue = "false") boolean includeWithoutDueDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(required = false) UUID creditCardId,
      @RequestParam(defaultValue = "false") boolean withoutCreditCard,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) ResponsibleType responsibleType,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    rejectUnknownParams(request);
    return payablesService.list(
        authenticatedUser,
        startDate,
        endDate,
        year,
        month,
        includeWithoutDueDate,
        status,
        overdue,
        creditCardId,
        withoutCreditCard,
        categoryId,
        responsibleType,
        search,
        sort,
        direction,
        page,
        size);
  }

  private static void rejectUnknownParams(HttpServletRequest request) {
    for (String name : request.getParameterMap().keySet()) {
      if (!ALLOWED_PARAMS.contains(name)) {
        throw new InvalidRequestException(PayablesService.INVALID_DATA);
      }
    }
  }
}
