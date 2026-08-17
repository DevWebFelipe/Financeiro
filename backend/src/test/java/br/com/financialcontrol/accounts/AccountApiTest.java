package br.com.financialcontrol.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountBalanceResponse;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.security.JwtProperties;
import br.com.financialcontrol.users.User;
import br.com.financialcontrol.users.UserRepository;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class AccountApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtProperties jwtProperties;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateBankAccountAndReturn201() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("alice"), "senha-segura");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson("Nubank", "BANK_ACCOUNT", "1500.00")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Nubank"))
            .andExpect(jsonPath("$.type").value("BANK_ACCOUNT"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andReturn();

    AccountResponse body = read(result, AccountResponse.class);
    assertThat(body.initialBalance()).isEqualByComparingTo("1500.00");

    Account saved = accountRepository.findById(body.id()).orElseThrow();
    User owner = userRepository.findByEmail(savedEmailFromToken(token)).orElseThrow();
    assertThat(saved.getUserId()).isEqualTo(owner.getId());
    assertThat(saved.getId().version()).isEqualTo(7);
    assertThat(saved.getInitialBalance()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldCreateCashAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("cash"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Carteira", "CASH", "0.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("CASH"));
  }

  @Test
  void shouldRejectInvalidCreateRequest() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("invalid"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"","type":"BANK_ACCOUNT","initialBalance":1500.00}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fields.name").exists());
  }

  @Test
  void shouldRejectInvalidAccountType() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("type"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Poupança", "SAVINGS", "100.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldRejectCreateWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Nubank", "BANK_ACCOUNT", "1500.00")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldIgnoreClientSuppliedUserIdOnCreate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("userid"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Nubank","type":"BANK_ACCOUNT","initialBalance":10.00,"userId":"%s"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldListAndGetOwnAccounts() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("list"), "senha-segura");
    AccountResponse created = createAccount(token, "Nubank", "BANK_ACCOUNT", "1500.00");

    mockMvc
        .perform(get("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(created.id().toString()))
        .andExpect(jsonPath("$[0].name").value("Nubank"));

    mockMvc
        .perform(
            get("/api/v1/accounts/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(created.id().toString()));
  }

  @Test
  void shouldReturn404ForUnknownAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("missing"), "senha-segura");

    mockMvc
        .perform(
            get("/api/v1/accounts/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldRejectGetWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/accounts"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldUpdateOwnAccount() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("upd"), "senha-segura");
    AccountResponse created = createAccount(token, "Nubank", "BANK_ACCOUNT", "1500.00");

    MvcResult result =
        mockMvc
            .perform(
                put("/api/v1/accounts/" + created.id())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank PJ","type":"CASH"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    AccountResponse updated = read(result, AccountResponse.class);
    assertThat(updated.name()).isEqualTo("Nubank PJ");
    assertThat(updated.type()).isEqualTo(AccountType.CASH);
    assertThat(updated.initialBalance()).isEqualByComparingTo("1500.00");

    Account saved = accountRepository.findById(created.id()).orElseThrow();
    assertThat(saved.getUserId())
        .isEqualTo(userRepository.findByEmail(savedEmailFromToken(token)).orElseThrow().getId());
  }

  @Test
  void shouldRejectUnknownFieldsOnUpdateIncludingOwnerAndBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("fields"), "senha-segura");
    AccountResponse created = createAccount(token, "Nubank", "BANK_ACCOUNT", "1500.00");
    UUID originalOwner = accountRepository.findById(created.id()).orElseThrow().getUserId();

    mockMvc
        .perform(
            put("/api/v1/accounts/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Hacked","type":"CASH","userId":"%s","initialBalance":0,"active":false}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    Account after = accountRepository.findById(created.id()).orElseThrow();
    assertThat(after.getName()).isEqualTo("Nubank");
    assertThat(after.getUserId()).isEqualTo(originalOwner);
    assertThat(after.getInitialBalance()).isEqualByComparingTo("1500.00");
    assertThat(after.isActive()).isTrue();
  }

  @Test
  void shouldRejectUpdateWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/accounts/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Nubank","type":"BANK_ACCOUNT"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldDeactivateAndReactivateWithoutPhysicalDelete() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("state"), "senha-segura");
    AccountResponse created = createAccount(token, "Nubank", "BANK_ACCOUNT", "0.00");

    mockMvc
        .perform(
            post("/api/v1/accounts/" + created.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    Account deactivated = accountRepository.findById(created.id()).orElseThrow();
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();
    assertThat(deactivated.getName()).isEqualTo("Nubank");

    mockMvc
        .perform(
            get("/api/v1/accounts/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(
            delete("/api/v1/accounts/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isMethodNotAllowed());

    assertThat(accountRepository.findById(created.id())).isPresent();

    mockMvc
        .perform(
            post("/api/v1/accounts/" + created.id() + "/activate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));

    assertThat(accountRepository.findById(created.id()).orElseThrow().isActive()).isTrue();
  }

  @Test
  void shouldReturnInitialBalanceAsCurrentBalance() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("bal"), "senha-segura");
    AccountResponse created = createAccount(token, "Nubank", "BANK_ACCOUNT", "1500.00");

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/accounts/" + created.id() + "/balance")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(created.id().toString()))
            .andReturn();

    AccountBalanceResponse body = read(result, AccountBalanceResponse.class);
    assertThat(body.balance()).isEqualByComparingTo("1500.00");
  }

  @Test
  void shouldIsolateAccountsBetweenUsers() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("iso-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("iso-b"), "senha-segura");
    AccountResponse accountA = createAccount(tokenA, "Conta A", "BANK_ACCOUNT", "1500.00");

    mockMvc
        .perform(
            get("/api/v1/accounts/" + accountA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            put("/api/v1/accounts/" + accountA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Invadida","type":"CASH"}
                    """))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/accounts/" + accountA.id() + "/activate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            post("/api/v1/accounts/" + accountA.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/v1/accounts/" + accountA.id() + "/balance")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    Account unchanged = accountRepository.findById(accountA.id()).orElseThrow();
    assertThat(unchanged.getName()).isEqualTo("Conta A");
    assertThat(unchanged.isActive()).isTrue();
  }

  @Test
  void shouldRejectInvalidTokenAndDeactivatedUser() throws Exception {
    mockMvc
        .perform(get("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());

    String email = uniqueEmail("expired");
    registerAndLogin("Expired", email, "senha-segura");
    User user = userRepository.findByEmail(email).orElseThrow();
    String expired =
        signedToken(user.getId(), Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60));
    mockMvc
        .perform(get("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
        .andExpect(status().isUnauthorized());

    String inactiveEmail = uniqueEmail("inactive");
    String token = registerAndLogin("Inactive", inactiveEmail, "senha-segura");
    User inactive = userRepository.findByEmail(inactiveEmail).orElseThrow();
    inactive.setActive(false);
    userRepository.saveAndFlush(inactive);
    mockMvc
        .perform(get("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnauthorized());
  }

  private AccountResponse createAccount(String token, String name, String type, String balance)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson(name, type, balance)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AccountResponse.class);
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
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private String savedEmailFromToken(String token) throws Exception {
    return userRepository
        .findById(UUID.fromString(SignedJWT.parse(token).getJWTClaimsSet().getSubject()))
        .orElseThrow()
        .getEmail();
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

  private <T> T read(MvcResult result, Class<T> type) throws Exception {
    return jsonMapper.readValue(result.getResponse().getContentAsString(), type);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private static String createJson(String name, String type, String initialBalance) {
    return """
        {"name":"%s","type":"%s","initialBalance":%s}
        """
        .formatted(name, type, initialBalance);
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
