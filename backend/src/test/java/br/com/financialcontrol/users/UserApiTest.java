package br.com.financialcontrol.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.security.JwtProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class UserApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtProperties jwtProperties;

  @Test
  void shouldReturnOwnProfileAndNeverExposePasswordHash() throws Exception {
    String email = "me-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Alice", email, "senha-segura");
    User saved = userRepository.findByEmail(email).orElseThrow();

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId().toString()))
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void shouldNotReturnAnotherUsersProfile() throws Exception {
    String tokenA =
        registerAndLogin("User A", "a-" + UUID.randomUUID() + "@example.com", "senha-segura");
    String emailB = "b-" + UUID.randomUUID() + "@example.com";
    registerAndLogin("User B", emailB, "senha-segura");
    User userB = userRepository.findByEmail(emailB).orElseThrow();

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(userB.getId().toString())))
        .andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.not(emailB)));
  }

  @Test
  void shouldRejectProtectedEndpointWithoutToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldRejectInvalidToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldRejectExpiredToken() throws Exception {
    String email = "expired-" + UUID.randomUUID() + "@example.com";
    registerAndLogin("Expired", email, "senha-segura");
    User user = userRepository.findByEmail(email).orElseThrow();
    String expired =
        signedToken(user.getId(), Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60));

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectDeactivatedUserToken() throws Exception {
    String email = "deactivated-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Inactive", email, "senha-segura");
    User user = userRepository.findByEmail(email).orElseThrow();
    user.setActive(false);
    userRepository.saveAndFlush(user);

    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldUpdateOwnNameAndEmail() throws Exception {
    String email = "upd-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Old Name", email, "senha-segura");
    String newEmail = "new-" + UUID.randomUUID() + "@example.com";

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"New Name","email":"%s"}
                    """
                        .formatted(newEmail)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(jsonPath("$.email").value(newEmail));
  }

  @Test
  void shouldRejectDuplicateEmailOnUpdate() throws Exception {
    String emailA = "ua-" + UUID.randomUUID() + "@example.com";
    String emailB = "ub-" + UUID.randomUUID() + "@example.com";
    String tokenA = registerAndLogin("A", emailA, "senha-segura");
    registerAndLogin("B", emailB, "senha-segura");

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"A","email":"%s"}
                    """
                        .formatted(emailB)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldRejectUnknownProfileFields() throws Exception {
    String email = "fields-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Keep", email, "senha-segura");
    User before = userRepository.findByEmail(email).orElseThrow();

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Hacked","email":"%s","id":"%s","active":false,"passwordHash":"x"}
                    """
                        .formatted(email, UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    User after = userRepository.findById(before.getId()).orElseThrow();
    assertThat(after.getId()).isEqualTo(before.getId());
    assertThat(after.isActive()).isTrue();
    assertThat(after.getPasswordHash()).isEqualTo(before.getPasswordHash());
    assertThat(after.getName()).isEqualTo("Keep");
  }

  @Test
  void shouldChangePasswordAndInvalidateOldCredentials() throws Exception {
    String email = "pwd-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Pwd", email, "senha-antiga");

    mockMvc
        .perform(
            put("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"currentPassword":"senha-antiga","newPassword":"senha-nova1"}
                    """))
        .andExpect(status().isNoContent());

    User saved = userRepository.findByEmail(email).orElseThrow();
    assertThat(saved.getPasswordHash()).isNotEqualTo("senha-nova1");
    assertThat(saved.getPasswordHash()).startsWith("$argon2id$");
    assertThat(passwordEncoder.matches("senha-nova1", saved.getPasswordHash())).isTrue();

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, "senha-antiga")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Credenciais inválidas."));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, "senha-nova1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }

  @Test
  void shouldRejectWrongCurrentPassword() throws Exception {
    String email = "wrong-current-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Pwd", email, "senha-segura");

    mockMvc
        .perform(
            put("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"currentPassword":"nao-e-essa","newPassword":"senha-nova1"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Credenciais inválidas."));
  }

  @Test
  void shouldRejectInvalidNewPassword() throws Exception {
    String email = "invalid-new-" + UUID.randomUUID() + "@example.com";
    String token = registerAndLogin("Pwd", email, "senha-segura");

    mockMvc
        .perform(
            put("/api/v1/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"currentPassword":"senha-segura","newPassword":"short"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fields.newPassword").exists());
  }

  private String registerAndLogin(String name, String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(name, email, password)))
        .andExpect(status().isCreated());
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return com.jayway.jsonpath.JsonPath.read(
        result.getResponse().getContentAsString(), "$.accessToken");
  }

  private String signedToken(UUID userId, Instant issuedAt, Instant expiresAt) throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)));
    return jwt.serialize();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
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
