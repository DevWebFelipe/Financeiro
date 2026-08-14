package br.com.financialcontrol.categories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.categories.dto.CategoryResponse;
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
class CategoryApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtProperties jwtProperties;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldCreateExpenseCategoryAndReturn201() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("alice"), "senha-segura");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson("Mercado", "EXPENSE")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Mercado"))
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andReturn();

    CategoryResponse body = read(result, CategoryResponse.class);
    Category saved = categoryRepository.findById(body.id()).orElseThrow();
    User owner = userRepository.findByEmail(savedEmailFromToken(token)).orElseThrow();
    assertThat(saved.getUserId()).isEqualTo(owner.getId());
    assertThat(saved.getId().version()).isEqualTo(7);
    assertThat(saved.isActive()).isTrue();
  }

  @Test
  void shouldCreateIncomeCategory() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("income"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Salário", "INCOME")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("INCOME"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void shouldTrimNameOnCreate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("trim"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("  Mercado  ", "EXPENSE")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Mercado"));
  }

  @Test
  void shouldRejectBlankAndWhitespaceName() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("blank"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("", "EXPENSE")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fields.name").exists());

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("   ", "EXPENSE")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fields.name").exists());
  }

  @Test
  void shouldRejectMissingAndInvalidType() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("type"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Mercado"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado", "TRANSFER")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldRejectCreateWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado", "EXPENSE")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldRejectUnknownFieldsIncludingUserIdOnCreate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("userid"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Mercado","type":"EXPENSE","userId":"%s"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Mercado","type":"EXPENSE","id":"%s","active":false,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldRejectDuplicateNameAndType() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("dup"), "senha-segura");
    createCategory(token, "Mercado", "EXPENSE");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado", "EXPENSE")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldAllowSameNameForIncomeAndExpense() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("both"), "senha-segura");
    createCategory(token, "Mercado", "EXPENSE");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado", "INCOME")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("INCOME"));
  }

  @Test
  void shouldTreatNamesAsEqualIgnoringCaseAndSurroundingWhitespace() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("case"), "senha-segura");
    createCategory(token, "Mercado", "EXPENSE");

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson(" mercado", "EXPENSE")))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("MERCADO", "EXPENSE")))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado ", "EXPENSE")))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldRejectDuplicateAfterDeactivation() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("deact-dup"), "senha-segura");
    CategoryResponse created = createCategory(token, "Mercado", "EXPENSE");

    mockMvc
        .perform(
            post("/api/v1/categories/" + created.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado", "EXPENSE")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldListOnlyOwnCategoriesAndSupportFilters() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("list-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("list-b"), "senha-segura");
    CategoryResponse expense = createCategory(tokenA, "Mercado", "EXPENSE");
    CategoryResponse income = createCategory(tokenA, "Salário", "INCOME");
    CategoryResponse inactive = createCategory(tokenA, "Lazer", "EXPENSE");
    createCategory(tokenB, "Outro", "EXPENSE");

    mockMvc
        .perform(
            post("/api/v1/categories/" + inactive.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").value(expense.id().toString()));

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .param("type", "INCOME"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(income.id().toString()));

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .param("type", "EXPENSE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .param("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .param("active", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(inactive.id().toString()));

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .param("type", "EXPENSE")
                .param("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(expense.id().toString()));

    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Outro"));
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoCategories() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("empty"), "senha-segura");

    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void shouldRejectListWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/categories"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void shouldUpdateOwnCategory() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("upd"), "senha-segura");
    CategoryResponse created = createCategory(token, "Mercado", "EXPENSE");

    MvcResult result =
        mockMvc
            .perform(
                put("/api/v1/categories/" + created.id())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson("Moradia", "INCOME")))
            .andExpect(status().isOk())
            .andReturn();

    CategoryResponse updated = read(result, CategoryResponse.class);
    assertThat(updated.name()).isEqualTo("Moradia");
    assertThat(updated.type()).isEqualTo(CategoryType.INCOME);
    assertThat(updated.active()).isTrue();

    Category saved = categoryRepository.findById(created.id()).orElseThrow();
    assertThat(saved.getUserId())
        .isEqualTo(userRepository.findByEmail(savedEmailFromToken(token)).orElseThrow().getId());
  }

  @Test
  void shouldTrimNameOnUpdateAndRejectDuplicate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("upd-dup"), "senha-segura");
    CategoryResponse mercado = createCategory(token, "Mercado", "EXPENSE");
    createCategory(token, "Moradia", "EXPENSE");

    mockMvc
        .perform(
            put("/api/v1/categories/" + mercado.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("  moradia  ", "EXPENSE")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void shouldRejectUnknownFieldsOnUpdateIncludingOwnerAndActive() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("fields"), "senha-segura");
    CategoryResponse created = createCategory(token, "Mercado", "EXPENSE");
    UUID originalOwner = categoryRepository.findById(created.id()).orElseThrow().getUserId();

    mockMvc
        .perform(
            put("/api/v1/categories/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Hacked","type":"INCOME","userId":"%s","active":false}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    Category after = categoryRepository.findById(created.id()).orElseThrow();
    assertThat(after.getName()).isEqualTo("Mercado");
    assertThat(after.getUserId()).isEqualTo(originalOwner);
    assertThat(after.isActive()).isTrue();
  }

  @Test
  void shouldReturn404ForUnknownCategoryOnUpdate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("missing"), "senha-segura");

    mockMvc
        .perform(
            put("/api/v1/categories/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Mercado", "EXPENSE")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldDeactivateWithoutPhysicalDeleteAndKeepIdempotent() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("state"), "senha-segura");
    CategoryResponse created = createCategory(token, "Mercado", "EXPENSE");

    mockMvc
        .perform(
            post("/api/v1/categories/" + created.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    Category deactivated = categoryRepository.findById(created.id()).orElseThrow();
    assertThat(deactivated).isNotNull();
    assertThat(deactivated.isActive()).isFalse();
    assertThat(deactivated.getName()).isEqualTo("Mercado");

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("active", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(created.id().toString()));

    mockMvc
        .perform(
            get("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .param("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc
        .perform(
            delete("/api/v1/categories/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isMethodNotAllowed());

    assertThat(categoryRepository.findById(created.id())).isPresent();

    mockMvc
        .perform(
            post("/api/v1/categories/" + created.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void shouldReturn404ForUnknownCategoryOnDeactivate() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("deact-missing"), "senha-segura");

    mockMvc
        .perform(
            post("/api/v1/categories/" + UUID.randomUUID() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void shouldIsolateCategoriesBetweenUsers() throws Exception {
    String tokenA = registerAndLogin("User A", uniqueEmail("iso-a"), "senha-segura");
    String tokenB = registerAndLogin("User B", uniqueEmail("iso-b"), "senha-segura");
    CategoryResponse categoryA = createCategory(tokenA, "Mercado", "EXPENSE");

    mockMvc
        .perform(
            put("/api/v1/categories/" + categoryA.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson("Invadida", "INCOME")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mockMvc
        .perform(
            post("/api/v1/categories/" + categoryA.id() + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    Category unchanged = categoryRepository.findById(categoryA.id()).orElseThrow();
    assertThat(unchanged.getName()).isEqualTo("Mercado");
    assertThat(unchanged.isActive()).isTrue();
  }

  @Test
  void shouldRejectInvalidTokenAndDeactivatedUser() throws Exception {
    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());

    String email = uniqueEmail("expired");
    registerAndLogin("Expired", email, "senha-segura");
    User user = userRepository.findByEmail(email).orElseThrow();
    String expired =
        signedToken(user.getId(), Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60));
    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
        .andExpect(status().isUnauthorized());

    String inactiveEmail = uniqueEmail("inactive");
    String token = registerAndLogin("Inactive", inactiveEmail, "senha-segura");
    User inactive = userRepository.findByEmail(inactiveEmail).orElseThrow();
    inactive.setActive(false);
    userRepository.saveAndFlush(inactive);
    mockMvc
        .perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isUnauthorized());
  }

  private CategoryResponse createCategory(String token, String name, String type) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson(name, type)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CategoryResponse.class);
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

  private static String createJson(String name, String type) {
    return """
        {"name":"%s","type":"%s"}
        """
        .formatted(name, type);
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
