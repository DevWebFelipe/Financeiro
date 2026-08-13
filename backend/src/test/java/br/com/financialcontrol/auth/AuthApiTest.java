package br.com.financialcontrol.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class AuthApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void shouldRegisterUserWithNormalizedEmailAndArgon2Hash() throws Exception {
    String email = "User-" + UUID.randomUUID() + "@Example.COM";
    String expectedEmail = email.toLowerCase();
    String password = "senha-segura";

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Felipe", email, password)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Felipe"))
        .andExpect(jsonPath("$.email").value(expectedEmail))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    User saved = userRepository.findByEmail(expectedEmail).orElseThrow();
    assertThat(saved.getPasswordHash()).isNotEqualTo(password);
    assertThat(saved.getPasswordHash()).startsWith("$argon2id$");
    assertThat(passwordEncoder.matches(password, saved.getPasswordHash())).isTrue();
  }

  @Test
  void shouldRejectDuplicateEmail() throws Exception {
    String email = "dup-" + UUID.randomUUID() + "@example.com";
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("User", email, "senha-segura")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Other", email.toUpperCase(), "outra-senha")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldRejectInvalidRegisterRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"","email":"not-an-email","password":"short"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fields").exists());
  }

  @Test
  void shouldLoginAndReturnBearerAccessToken() throws Exception {
    String email = "login-" + UUID.randomUUID() + "@example.com";
    register(email, "senha-segura");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, "senha-segura")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(1800))
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();

    String token =
        com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.accessToken");
    SignedJWT jwt = SignedJWT.parse(token);
    User saved = userRepository.findByEmail(email).orElseThrow();
    assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(saved.getId().toString());
  }

  @Test
  void shouldReturnGenericUnauthorizedForWrongPassword() throws Exception {
    String email = "wrong-pass-" + UUID.randomUUID() + "@example.com";
    register(email, "senha-segura");
    expectInvalidCredentials(email, "senha-errada");
  }

  @Test
  void shouldReturnGenericUnauthorizedForUnknownEmail() throws Exception {
    expectInvalidCredentials("missing-" + UUID.randomUUID() + "@example.com", "senha-segura");
  }

  @Test
  void shouldReturnGenericUnauthorizedForDeactivatedUser() throws Exception {
    String email = "inactive-" + UUID.randomUUID() + "@example.com";
    register(email, "senha-segura");
    User user = userRepository.findByEmail(email).orElseThrow();
    user.setActive(false);
    userRepository.saveAndFlush(user);
    expectInvalidCredentials(email, "senha-segura");
  }

  @Test
  void shouldRejectInvalidLoginRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void healthRegisterAndLoginShouldBePublic() throws Exception {
    mockMvc
        .perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("UP"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    registerJson(
                        "Public", "public-" + UUID.randomUUID() + "@example.com", "senha-segura")))
        .andExpect(status().isCreated());
  }

  private void expectInvalidCredentials(String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Credenciais inválidas."));
  }

  private void register(String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("User", email, password)))
        .andExpect(status().isCreated());
  }

  private static String registerJson(String name, String email, String password) {
    return """
        {"name":"%s","email":"%s","password":"%s"}
        """
        .formatted(name, email, password);
  }

  private static String loginJson(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }
}
