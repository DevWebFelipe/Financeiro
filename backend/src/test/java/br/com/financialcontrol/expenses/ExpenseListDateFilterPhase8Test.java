package br.com.financialcontrol.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.expenses.dto.CreateExpenseRequest;
import br.com.financialcontrol.expenses.dto.ExpensePageResponse;
import br.com.financialcontrol.expenses.dto.ExpenseResponse;
import br.com.financialcontrol.security.AuthenticatedUser;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class ExpenseListDateFilterPhase8Test {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExpenseService expenseService;

  @Test
  void shouldFilterOneToOneByInstallmentDueDateEquivalentToExpenseDueDate() throws Exception {
    Fixture fx = bootstrap("n1");
    AuthenticatedUser user = new AuthenticatedUser(fx.userId());
    ExpenseResponse inside = create(fx, "10.00", 1, LocalDate.of(2026, 3, 15));
    create(fx, "20.00", 1, LocalDate.of(2026, 5, 15));

    ExpensePageResponse page =
        expenseService.list(
            user,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            null,
            null,
            null,
            null,
            null,
            0,
            20);

    assertThat(page.items()).extracting(ExpenseResponse::id).containsExactly(inside.id());
  }

  @Test
  void shouldIncludeMultiInstallmentWhenAnyParcelDueDateIsInRange() throws Exception {
    Fixture fx = bootstrap("n-multi");
    AuthenticatedUser user = new AuthenticatedUser(fx.userId());
    // first due 2026-01-31 → parcels: 31/01, 28/02, 31/03
    ExpenseResponse multi = create(fx, "300.00", 3, LocalDate.of(2026, 1, 31));
    create(fx, "50.00", 1, LocalDate.of(2025, 12, 1));

    ExpensePageResponse february =
        expenseService.list(
            user,
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28),
            null,
            null,
            null,
            null,
            null,
            0,
            20);
    assertThat(february.items()).extracting(ExpenseResponse::id).containsExactly(multi.id());

    ExpensePageResponse march =
        expenseService.list(
            user,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            null,
            null,
            null,
            null,
            null,
            0,
            20);
    assertThat(march.items()).extracting(ExpenseResponse::id).containsExactly(multi.id());

    ExpensePageResponse april =
        expenseService.list(
            user,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30),
            null,
            null,
            null,
            null,
            null,
            0,
            20);
    assertThat(april.items()).extracting(ExpenseResponse::id).doesNotContain(multi.id());
  }

  @Test
  void shouldNotDuplicateExpenseWhenMultipleInstallmentsMatchAndRespectOwnershipAndPaging()
      throws Exception {
    Fixture owner = bootstrap("own-page");
    Fixture other = bootstrap("oth-page");
    AuthenticatedUser ownerUser = new AuthenticatedUser(owner.userId());
    AuthenticatedUser otherUser = new AuthenticatedUser(other.userId());

    // Two parcels both in March if we used wrong AND semantics with separate EXISTS;
    // with single EXISTS on range, still one expense row.
    ExpenseResponse multi = create(owner, "600.00", 3, LocalDate.of(2026, 3, 10));
    create(other, "600.00", 3, LocalDate.of(2026, 3, 10));

    ExpensePageResponse page =
        expenseService.list(
            ownerUser,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 5, 31),
            null,
            null,
            null,
            null,
            null,
            0,
            20);

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().getFirst().id()).isEqualTo(multi.id());
    assertThat(page.totalItems()).isEqualTo(1);

    ExpensePageResponse otherPage =
        expenseService.list(
            otherUser,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 5, 31),
            null,
            null,
            null,
            null,
            null,
            0,
            20);
    assertThat(otherPage.items()).hasSize(1);
    assertThat(otherPage.items().getFirst().id()).isNotEqualTo(multi.id());

    MvcResult http =
        mockMvc
            .perform(
                get("/api/v1/expenses")
                    .param("startDate", "2026-03-01")
                    .param("endDate", "2026-05-31")
                    .param("page", "0")
                    .param("size", "1")
                    .header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
            .andExpect(status().isOk())
            .andReturn();
    List<?> ids = JsonPath.read(http.getResponse().getContentAsString(), "$.items[*].id");
    assertThat(ids).hasSize(1);
    Number totalItems = JsonPath.read(http.getResponse().getContentAsString(), "$.totalItems");
    assertThat(totalItems.intValue()).isEqualTo(1);
  }

  private ExpenseResponse create(Fixture fx, String total, int installments, LocalDate dueDate) {
    return expenseService.create(
        new AuthenticatedUser(fx.userId()),
        new CreateExpenseRequest(
            fx.categoryId(),
            "Despesa",
            new BigDecimal(total),
            LocalDate.of(2026, 1, 1),
            dueDate,
            PaymentMethod.ACCOUNT,
            fx.accountId(),
            ResponsibleType.MINE,
            null,
            null,
            null,
            installments));
  }

  private Fixture bootstrap(String prefix) throws Exception {
    String email = prefix + "-" + UUID.randomUUID() + "@example.com";
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"User","email":"%s","password":"senha-segura"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
    String token =
        JsonPath.read(
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {"email":"%s","password":"senha-segura"}
                            """
                                .formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.accessToken");
    UUID userId =
        UUID.fromString(
            JsonPath.read(
                mockMvc
                    .perform(
                        get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.id"));
    UUID categoryId =
        UUID.fromString(
            JsonPath.read(
                mockMvc
                    .perform(
                        post("/api/v1/categories")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Cat\",\"type\":\"EXPENSE\"}"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.id"));
    UUID accountId =
        UUID.fromString(
            JsonPath.read(
                mockMvc
                    .perform(
                        post("/api/v1/accounts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"name\":\"Conta\",\"type\":\"BANK_ACCOUNT\",\"initialBalance\":1000.00}"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.id"));
    return new Fixture(token, userId, categoryId, accountId);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private record Fixture(String token, UUID userId, UUID categoryId, UUID accountId) {}
}
