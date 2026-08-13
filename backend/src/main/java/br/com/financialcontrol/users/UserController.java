package br.com.financialcontrol.users;

import br.com.financialcontrol.config.OpenApiConfig;
import br.com.financialcontrol.security.AuthenticatedUser;
import br.com.financialcontrol.users.dto.ChangePasswordRequest;
import br.com.financialcontrol.users.dto.UpdateProfileRequest;
import br.com.financialcontrol.users.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  @Operation(summary = "Consultar o perfil do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Perfil do usuário autenticado"),
    @ApiResponse(responseCode = "401", description = "Não autenticado")
  })
  public UserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    return userService.getMe(authenticatedUser);
  }

  @PutMapping("/me")
  @Operation(summary = "Atualizar nome e e-mail do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado"),
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
  })
  public UserResponse updateMe(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateMe(authenticatedUser, request);
  }

  @PutMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Alterar a senha do usuário autenticado")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Senha alterada"),
    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
    @ApiResponse(responseCode = "401", description = "Não autenticado ou senha atual incorreta")
  })
  public void changePassword(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(authenticatedUser, request);
  }
}
