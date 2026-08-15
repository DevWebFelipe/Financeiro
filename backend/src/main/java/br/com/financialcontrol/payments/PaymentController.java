package br.com.financialcontrol.payments;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.expenses.ExpenseService;
import br.com.financialcontrol.payments.dto.PaymentResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class PaymentController {

  private final ExpenseService expenseService;

  public PaymentController(ExpenseService expenseService) {
    this.expenseService = expenseService;
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar um pagamento do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
  })
  public PaymentResponse get(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable UUID id) {
    return expenseService.getPayment(authenticatedUser, id);
  }
}
