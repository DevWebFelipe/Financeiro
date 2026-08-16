package br.com.financialcontrol.credit_card_invoices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.credit_card_invoices.dto.CreditCardInvoiceResponse;
import br.com.financialcontrol.credit_card_invoices.dto.InvoicePaymentResponse;
import br.com.financialcontrol.credit_cards.dto.CreditCardResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class InvoicePaymentConcurrencyTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;

  @Test
  void shouldPreventConcurrentInvoicePaymentsFromExceedingRemaining() throws Exception {
    String token = registerAndLogin("Alice", uniqueEmail("inv-race"), "senha-segura");
    CategoryResponse category = createCategory(token);
    AccountResponse account = createAccount(token, "500.00");
    CreditCardResponse card = createCard(token);
    createCardExpense(token, category.id(), card.id(), "100.00");
    CreditCardInvoiceResponse invoice = currentInvoice(token, card.id());
    String body = payJson(account.id(), "100.00");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first = pool.submit(() -> payStatus(token, invoice.id(), body, start));
      Future<Integer> second = pool.submit(() -> payStatus(token, invoice.id(), body, start));
      start.countDown();
      int statusA = first.get(30, TimeUnit.SECONDS);
      int statusB = second.get(30, TimeUnit.SECONDS);
      assertThat(List.of(statusA, statusB)).contains(200);
      assertThat(statusA == 200 ? statusB : statusA).isEqualTo(400);
    } finally {
      pool.shutdownNow();
    }

    InvoicePaymentResponse[] payments =
        jsonMapper.readValue(
            mockMvc
                .perform(
                    get("/api/v1/invoices/" + invoice.id() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            InvoicePaymentResponse[].class);
    BigDecimal activePaid =
        Arrays.stream(payments)
            .filter(payment -> payment.status() == InvoicePaymentStatus.ACTIVE)
            .map(InvoicePaymentResponse::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(activePaid).isEqualByComparingTo("100.00");
    CreditCardInvoiceResponse after = currentInvoice(token, card.id());
    assertThat(after.remainingAmount()).isEqualByComparingTo("0.00");
  }

  private int payStatus(String token, UUID invoiceId, String body, CountDownLatch start)
      throws Exception {
    start.await(10, TimeUnit.SECONDS);
    return mockMvc
        .perform(
            post("/api/v1/invoices/" + invoiceId + "/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private CreditCardInvoiceResponse currentInvoice(String token, UUID cardId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/credit-cards/" + cardId + "/invoices/current")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), CreditCardInvoiceResponse.class);
  }

  private void createCardExpense(String token, UUID categoryId, UUID cardId, String amount)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expenses")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"categoryId":"%s","description":"Compra","totalAmount":%s,"expenseDate":"2026-08-11","dueDate":"2099-01-01","paymentMethod":"CREDIT_CARD","creditCardId":"%s","responsibleType":"MINE"}
                    """
                        .formatted(categoryId, amount, cardId)))
        .andExpect(status().isCreated());
  }

  private CreditCardResponse createCard(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/credit-cards")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","holderName":"Ederson","creditLimit":5000.00,"closingDay":10,"dueDay":20}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(
        result.getResponse().getContentAsString(), CreditCardResponse.class);
  }

  private CategoryResponse createCategory(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Cartão","type":"EXPENSE"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
  }

  private AccountResponse createAccount(String token, String initial) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Nubank","type":"BANK_ACCOUNT","initialBalance":%s}
                        """
                            .formatted(initial)))
            .andExpect(status().isCreated())
            .andReturn();
    return jsonMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
  }

  private String registerAndLogin(String name, String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"%s","email":"%s","password":"%s"}
                    """
                        .formatted(name, email, password)))
        .andExpect(status().isCreated());
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"%s"}
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private static String payJson(UUID accountId, String amount) {
    return """
        {"accountId":"%s","amount":%s,"paymentDate":"2026-08-20"}
        """
        .formatted(accountId, amount);
  }
}
