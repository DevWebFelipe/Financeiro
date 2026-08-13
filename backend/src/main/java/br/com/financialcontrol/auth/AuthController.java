package br.com.financialcontrol.auth;

import br.com.financialcontrol.auth.dto.LoginRequest;
import br.com.financialcontrol.auth.dto.LoginResponse;
import br.com.financialcontrol.auth.dto.RegisterRequest;
import br.com.financialcontrol.users.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cadastrar usuário")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Usuário criado",
        content = @Content(schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
  })
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Autenticar e emitir Access Token")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Autenticado",
        content = @Content(schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
  })
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
