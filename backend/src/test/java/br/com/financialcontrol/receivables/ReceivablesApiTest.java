package br.com.financialcontrol.receivables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financialcontrol.PostgresTestcontainersConfig;
import br.com.financialcontrol.accounts.dto.AccountResponse;
import br.com.financialcontrol.categories.dto.CategoryResponse;
import br.com.financialcontrol.expenses.ResponsibleType;
import br.com.financialcontrol.incomes.Income;
import br.com.financialcontrol.incomes.IncomeRepository;
import br.com.financialcontrol.incomes.dto.IncomeResponse;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class ReceivablesApiTest {

  static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
  private static final String TODAY_TEXT = "2026-08-17";
  private static final String YESTERDAY = "2026-08-16";
  private static final String TOMORROW = "2026-08-18";

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper jsonMapper;
  @Autowired private IncomeRepository incomeRepository;

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-17T15:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Test
  void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(get("/api/v1/receivables")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldIsolateUsersAndExcludeForeignTotals() throws Exception {
    UserFx userA = bootstrap("iso-a");
    UserFx userB = bootstrap("iso-b");
    IncomeResponse expectedA = createIncome(userA, "A-aberta", "100.00", TOMORROW);
    IncomeResponse receivedA = createReceived(userA, "A-recebida", "40.00", YESTERDAY, TODAY_TEXT);
    IncomeResponse cancelledA = createCancelled(userA, "A-cancelada", "15.00", TOMORROW);
    IncomeResponse expectedB = createIncome(userB, "B-aberta", "999.00", TOMORROW);
    createReceived(userB, "B-recebida", "888.00", YESTERDAY, TODAY_TEXT);
    createCancelled(userB, "B-cancelada", "777.00", TOMORROW);

    ReceivablePageResponse pageA = listReceivables(userA.token());
    assertThat(ids(pageA)).containsExactly(expectedA.id());
    assertThat(ids(pageA)).doesNotContain(expectedB.id(), receivedA.id(), cancelledA.id());
    assertThat(pageA.summary().futureAmount()).isEqualByComparingTo("100.00");
    assertThat(pageA.summary().receivedAmount()).isEqualByComparingTo("0.00");

    ReceivablePageResponse receivedOnlyA = listReceivables(userA.token(), "status", "RECEIVED");
    assertThat(ids(receivedOnlyA)).containsExactly(receivedA.id());
    assertThat(ids(receivedOnlyA)).doesNotContain(expectedB.id());
    assertThat(receivedOnlyA.summary().receivedAmount()).isEqualByComparingTo("40.00");
  }

  @Test
  void shouldListOnlyExpectedByDefaultAndKeepReceivedAmountZero() throws Exception {
    UserFx user = bootstrap("default");
    IncomeResponse future = createIncome(user, "Futura", "100.00", TOMORROW);
    IncomeResponse overdue = createIncome(user, "Vencida", "30.00", YESTERDAY);
    IncomeResponse received = createReceived(user, "Recebida", "50.00", YESTERDAY, TODAY_TEXT);
    IncomeResponse cancelled = createCancelled(user, "Cancelada", "20.00", TOMORROW);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/receivables").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].remainingAmount").doesNotExist())
            .andExpect(jsonPath("$.items[0].receivedAmount").doesNotExist())
            .andExpect(jsonPath("$.summary.receivedAmount").exists())
            .andReturn();
    ReceivablePageResponse page = read(result, ReceivablePageResponse.class);

    assertThat(ids(page)).containsExactlyInAnyOrder(future.id(), overdue.id());
    assertThat(ids(page)).doesNotContain(received.id(), cancelled.id());
    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(20);
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("100.00");
    assertThat(page.summary().overdueAmount()).isEqualByComparingTo("30.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("130.00");
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().totalReceivableAmount())
        .isEqualByComparingTo(page.summary().futureAmount().add(page.summary().overdueAmount()));
  }

  @Test
  void shouldMarkFutureExpectedAsNotOverdue() throws Exception {
    UserFx user = bootstrap("future");
    IncomeResponse income = createIncome(user, "Salário", "5400.00", "2026-09-05");

    ReceivableItemResponse item = item(listReceivables(user.token()), income.id());
    assertThat(item.status()).isEqualTo("EXPECTED");
    assertThat(item.overdue()).isFalse();
    assertThat(item.amount()).isEqualByComparingTo("5400.00");
    assertThat(item.expectedDate()).isEqualTo(LocalDate.of(2026, 9, 5));
    assertThat(item.receivedDate()).isNull();
    assertThat(item.accountId()).isNull();
  }

  @Test
  void shouldMarkPastExpectedAsOverdueUsingFinancialTimezone() throws Exception {
    UserFx user = bootstrap("overdue");
    IncomeResponse income = createIncome(user, "Atrasada", "80.00", YESTERDAY);

    ReceivableItemResponse item = item(listReceivables(user.token()), income.id());
    assertThat(item.status()).isEqualTo("EXPECTED");
    assertThat(item.overdue()).isTrue();
    assertThat(item.amount()).isEqualByComparingTo("80.00");
    assertThat(item.expectedDate()).isEqualTo(LocalDate.parse(YESTERDAY));
  }

  @Test
  void shouldNotMarkExpectedOnTodayAsOverdue() throws Exception {
    UserFx user = bootstrap("today");
    IncomeResponse income = createIncome(user, "Hoje", "25.00", TODAY_TEXT);

    ReceivableItemResponse item = item(listReceivables(user.token()), income.id());
    assertThat(item.status()).isEqualTo("EXPECTED");
    assertThat(item.overdue()).isFalse();
    assertThat(item.expectedDate()).isEqualTo(TODAY);
  }

  @Test
  void shouldHideReceivedUnlessStatusFilter() throws Exception {
    UserFx user = bootstrap("received");
    IncomeResponse received =
        createReceived(user, "Freelance", "200.00", "2026-07-01", "2026-07-10");

    assertThat(ids(listReceivables(user.token()))).doesNotContain(received.id());

    ReceivablePageResponse page = listReceivables(user.token(), "status", "RECEIVED");
    ReceivableItemResponse item = item(page, received.id());
    assertThat(item.status()).isEqualTo("RECEIVED");
    assertThat(item.overdue()).isFalse();
    assertThat(item.receivedDate()).isEqualTo(LocalDate.of(2026, 7, 10));
    assertThat(item.amount()).isEqualByComparingTo("200.00");
    assertThat(item.accountId()).isEqualTo(user.accountId());
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("200.00");
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().overdueAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldNeverIncludeCancelledEvenWhenOtherFiltersMatch() throws Exception {
    UserFx user = bootstrap("cancelled");
    UUID otherCategory = createIncomeCategory(user.token(), "Outra").id();
    IncomeResponse cancelled = createCancelled(user, "Inutilizada", "90.00", "2026-09-10");

    assertThat(ids(listReceivables(user.token()))).doesNotContain(cancelled.id());
    assertThat(ids(listReceivables(user.token(), "status", "EXPECTED")))
        .doesNotContain(cancelled.id());
    ReceivablePageResponse byPeriod =
        listReceivables(
            user.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-09-01",
            "endDate",
            "2026-09-30");
    assertThat(ids(byPeriod)).doesNotContain(cancelled.id());
    assertThat(byPeriod.summary().totalReceivableAmount()).isEqualByComparingTo("0.00");
    assertThat(ids(listReceivables(user.token(), "categoryId", user.categoryId().toString())))
        .doesNotContain(cancelled.id());
    assertThat(ids(listReceivables(user.token(), "categoryId", otherCategory.toString())))
        .doesNotContain(cancelled.id());
    assertThat(ids(listReceivables(user.token(), "accountId", user.accountId().toString())))
        .doesNotContain(cancelled.id());
  }

  @Test
  void shouldFilterBySingleStatusAndRejectInvalidOrMultipleValues() throws Exception {
    UserFx user = bootstrap("status");
    IncomeResponse expected = createIncome(user, "Aberta", "10.00", TOMORROW);
    IncomeResponse received = createReceived(user, "Baixada", "20.00", YESTERDAY, TODAY_TEXT);

    assertThat(ids(listReceivables(user.token(), "status", "EXPECTED")))
        .containsExactly(expected.id());
    assertThat(ids(listReceivables(user.token(), "status", "RECEIVED")))
        .containsExactly(received.id());

    rejectInvalid(user.token(), "status", "INVALID");
    rejectInvalid(user.token(), "status", "CANCELLED");
    rejectInvalid(user.token(), "status", "EXPECTED,RECEIVED");
  }

  @Test
  void shouldFilterExpectedByOverdueFlag() throws Exception {
    UserFx user = bootstrap("overdue-filter");
    IncomeResponse overdue = createIncome(user, "Vencida", "11.00", YESTERDAY);
    IncomeResponse future = createIncome(user, "Futura", "22.00", TOMORROW);
    IncomeResponse today = createIncome(user, "Hoje", "33.00", TODAY_TEXT);

    assertThat(ids(listReceivables(user.token(), "status", "EXPECTED", "overdue", "true")))
        .containsExactly(overdue.id());
    assertThat(ids(listReceivables(user.token(), "status", "EXPECTED", "overdue", "false")))
        .containsExactlyInAnyOrder(future.id(), today.id());
    assertThat(ids(listReceivables(user.token(), "status", "EXPECTED")))
        .containsExactlyInAnyOrder(overdue.id(), future.id(), today.id());
    assertThat(ids(listReceivables(user.token(), "overdue", "true"))).containsExactly(overdue.id());
  }

  @Test
  void shouldRejectOverdueCombinedWithReceived() throws Exception {
    UserFx user = bootstrap("overdue-received");
    rejectInvalid(user.token(), "status", "RECEIVED", "overdue", "true");
    rejectInvalid(user.token(), "status", "RECEIVED", "overdue", "false");
  }

  @Test
  void shouldFilterPeriodByExpectedDateNotReceivedDate() throws Exception {
    UserFx user = bootstrap("date-expected");
    IncomeResponse inRange = createIncome(user, "No periodo", "10.00", "2026-08-10");
    createIncome(user, "Fora", "20.00", "2026-07-01");
    IncomeResponse receivedInAugust =
        createReceived(user, "Recebida em agosto", "30.00", "2026-07-15", "2026-08-12");

    ReceivablePageResponse page =
        listReceivables(
            user.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(ids(page)).containsExactly(inRange.id());
    assertThat(ids(page)).doesNotContain(receivedInAugust.id());
  }

  @Test
  void shouldFilterPeriodByReceivedDateNotExpectedDate() throws Exception {
    UserFx user = bootstrap("date-received");
    IncomeResponse inRange =
        createReceived(user, "Recebida em agosto", "40.00", "2026-07-01", "2026-08-12");
    IncomeResponse outOfRange =
        createReceived(user, "Recebida em julho", "50.00", "2026-08-10", "2026-07-20");

    ReceivablePageResponse page =
        listReceivables(
            user.token(),
            "status",
            "RECEIVED",
            "dateType",
            "RECEIVED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(ids(page)).containsExactly(inRange.id());
    assertThat(ids(page)).doesNotContain(outOfRange.id());
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("40.00");
  }

  @Test
  void shouldRejectPeriodWithoutDateTypeAndAllowDateTypeWithoutPeriod() throws Exception {
    UserFx user = bootstrap("date-type-required");
    IncomeResponse expected = createIncome(user, "Aberta", "10.00", TOMORROW);

    rejectInvalid(user.token(), "startDate", "2026-08-01", "endDate", "2026-08-31");
    rejectInvalid(user.token(), "startDate", "2026-08-01");
    rejectInvalid(user.token(), "endDate", "2026-08-31");

    ReceivablePageResponse page = listReceivables(user.token(), "dateType", "EXPECTED");
    assertThat(ids(page)).containsExactly(expected.id());
  }

  @Test
  void shouldRejectIncompatibleDateTypeAndStatus() throws Exception {
    UserFx user = bootstrap("date-incompatible");
    rejectInvalid(user.token(), "status", "EXPECTED", "dateType", "RECEIVED");
    rejectInvalid(user.token(), "status", "RECEIVED", "dateType", "EXPECTED");
    rejectInvalid(user.token(), "dateType", "RECEIVED");
  }

  @Test
  void shouldUseInclusiveStartAndEndDates() throws Exception {
    UserFx user = bootstrap("inclusive");
    IncomeResponse onStart = createIncome(user, "Inicio", "10.00", "2026-09-10");
    IncomeResponse onEnd = createIncome(user, "Fim", "20.00", "2026-09-20");
    IncomeResponse before = createIncome(user, "Antes", "30.00", "2026-09-09");
    IncomeResponse after = createIncome(user, "Depois", "40.00", "2026-09-21");

    ReceivablePageResponse page =
        listReceivables(
            user.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-09-10",
            "endDate",
            "2026-09-20");
    assertThat(ids(page)).containsExactlyInAnyOrder(onStart.id(), onEnd.id());
    assertThat(ids(page)).doesNotContain(before.id(), after.id());
  }

  @Test
  void shouldRejectStartDateAfterEndDate() throws Exception {
    UserFx user = bootstrap("range");
    rejectInvalid(
        user.token(), "dateType", "EXPECTED", "startDate", "2026-08-31", "endDate", "2026-08-01");
  }

  @Test
  void shouldFilterByCategoryAndApplyItToSummary() throws Exception {
    UserFx user = bootstrap("category");
    UUID salary = user.categoryId();
    UUID extra = createIncomeCategory(user.token(), "Extra").id();
    IncomeResponse salaryIncome = createIncome(user, salary, "Salário", "100.00", TOMORROW);
    createIncome(user, extra, "Bonus", "40.00", TOMORROW);

    ReceivablePageResponse page =
        listReceivables(user.token(), "status", "EXPECTED", "categoryId", salary.toString());
    assertThat(ids(page)).containsExactly(salaryIncome.id());
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("100.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldFilterReceivedByAccountAndReturnEmptyForExpectedWithAccount() throws Exception {
    UserFx user = bootstrap("account");
    UUID otherAccount = createAccount(user.token(), "Carteira", "CASH", "100.00").id();
    IncomeResponse receivedHere =
        createReceived(user, user.accountId(), "Na conta", "70.00", YESTERDAY, TODAY_TEXT);
    createReceived(user, otherAccount, "Outra conta", "15.00", YESTERDAY, TODAY_TEXT);
    createIncome(user, "Aberta", "90.00", TOMORROW);

    ReceivablePageResponse received =
        listReceivables(
            user.token(), "status", "RECEIVED", "accountId", user.accountId().toString());
    assertThat(ids(received)).containsExactly(receivedHere.id());
    assertThat(received.summary().receivedAmount()).isEqualByComparingTo("70.00");

    ReceivablePageResponse expectedWithAccount =
        listReceivables(
            user.token(), "status", "EXPECTED", "accountId", user.accountId().toString());
    assertThat(expectedWithAccount.items()).isEmpty();
    assertThat(expectedWithAccount.totalItems()).isZero();
    assertThat(expectedWithAccount.summary().totalReceivableAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldReturnAndFilterResponsibleWhenPersistedOnIncome() throws Exception {
    UserFx user = bootstrap("responsible");
    IncomeResponse mine = createIncome(user, "Salário Felipe", "100.00", TOMORROW);
    IncomeResponse spouse = createIncome(user, "Salário esposa", "80.00", TOMORROW);
    persistResponsible(mine.id(), ResponsibleType.MINE, null);
    persistResponsible(spouse.id(), ResponsibleType.GIULIA, null);

    ReceivableItemResponse listed = item(listReceivables(user.token()), mine.id());
    assertThat(listed.responsibleType()).isEqualTo(ResponsibleType.MINE);
    assertThat(listed.responsibleName()).isNull();

    ReceivablePageResponse byMine = listReceivables(user.token(), "responsibleType", "MINE");
    assertThat(ids(byMine)).containsExactly(mine.id());
    ReceivablePageResponse byGiulia = listReceivables(user.token(), "responsibleType", "GIULIA");
    assertThat(ids(byGiulia)).containsExactly(spouse.id());

    IncomeResponse other = createIncome(user, "Aluguel terceiro", "50.00", TOMORROW);
    persistResponsible(other.id(), ResponsibleType.OTHER, "Joao");
    ReceivablePageResponse byName = listReceivables(user.token(), "responsibleName", "Joao");
    assertThat(ids(byName)).containsExactly(other.id());
  }

  @Test
  void shouldSortByDocumentedFieldsAndDirections() throws Exception {
    UserFx user = bootstrap("sort");
    IncomeResponse low = createIncome(user, "Beta", "10.00", "2026-09-03");
    IncomeResponse high = createIncome(user, "Alpha", "30.00", "2026-09-01");
    IncomeResponse mid = createIncome(user, "Gamma", "20.00", "2026-09-02");

    assertThat(ids(listReceivables(user.token(), "sort", "amount", "direction", "asc")))
        .containsExactly(low.id(), mid.id(), high.id());
    assertThat(ids(listReceivables(user.token(), "sort", "amount", "direction", "desc")))
        .containsExactly(high.id(), mid.id(), low.id());
    assertThat(ids(listReceivables(user.token(), "sort", "amount", "direction", "ASC")))
        .containsExactly(low.id(), mid.id(), high.id());
    assertThat(ids(listReceivables(user.token(), "sort", "amount", "direction", "DESC")))
        .containsExactly(high.id(), mid.id(), low.id());

    assertThat(ids(listReceivables(user.token(), "sort", "expectedDate", "direction", "asc")))
        .containsExactly(high.id(), mid.id(), low.id());
    assertThat(ids(listReceivables(user.token(), "sort", "description", "direction", "asc")))
        .containsExactly(high.id(), low.id(), mid.id());
    assertThat(ids(listReceivables(user.token(), "sort", "status", "direction", "asc")))
        .containsExactlyInAnyOrder(low.id(), high.id(), mid.id());

    ReceivablePageResponse byCreated =
        listReceivables(user.token(), "sort", "createdAt", "direction", "asc");
    assertThat(ids(byCreated)).containsExactly(low.id(), high.id(), mid.id());

    IncomeResponse receivedEarly =
        createReceived(user, "Recebida cedo", "5.00", YESTERDAY, "2026-07-01");
    IncomeResponse receivedLate =
        createReceived(user, "Recebida tarde", "6.00", YESTERDAY, "2026-07-20");
    assertThat(
            ids(
                listReceivables(
                    user.token(),
                    "status",
                    "RECEIVED",
                    "sort",
                    "receivedDate",
                    "direction",
                    "asc")))
        .containsExactly(receivedEarly.id(), receivedLate.id());
    assertThat(
            ids(
                listReceivables(
                    user.token(),
                    "status",
                    "RECEIVED",
                    "sort",
                    "receivedDate",
                    "direction",
                    "desc")))
        .containsExactly(receivedLate.id(), receivedEarly.id());
  }

  @Test
  void shouldSortByExpectedDateAscByDefault() throws Exception {
    UserFx user = bootstrap("sort-default");
    IncomeResponse later = createIncome(user, "Depois", "10.00", "2026-09-20");
    IncomeResponse earlier = createIncome(user, "Antes", "10.00", "2026-09-10");

    assertThat(ids(listReceivables(user.token()))).containsExactly(earlier.id(), later.id());
  }

  @Test
  void shouldBreakTiesByIdAscendingWhenExpectedDatesAreEqual() throws Exception {
    UserFx user = bootstrap("sort-tie");
    IncomeResponse first = createIncome(user, "Primeira", "10.00", "2026-09-10");
    IncomeResponse second = createIncome(user, "Segunda", "10.00", "2026-09-10");

    assertThat(ids(listReceivables(user.token()))).containsExactly(first.id(), second.id());
    assertThat(ids(listReceivables(user.token(), "sort", "expectedDate", "direction", "desc")))
        .containsExactly(first.id(), second.id());
  }

  @Test
  void shouldRejectInvalidSortAndDirection() throws Exception {
    UserFx user = bootstrap("sort-invalid");
    rejectInvalid(user.token(), "sort", "campoInexistente");
    rejectInvalid(user.token(), "sort", "dueDate");
    rejectInvalid(user.token(), "direction", "INVALID");
  }

  @Test
  void shouldRejectInvalidDateTypeOverdueAndUuid() throws Exception {
    UserFx user = bootstrap("invalid-values");
    rejectInvalid(user.token(), "dateType", "INVALID");
    rejectInvalid(user.token(), "overdue", "INVALID");
    rejectInvalid(user.token(), "categoryId", "not-a-uuid");
    rejectInvalid(user.token(), "accountId", "not-a-uuid");
    rejectInvalid(user.token(), "responsibleType", "INVALID");
  }

  @Test
  void shouldReturnEmptyEnvelopeWhenUserHasNoEligibleIncomes() throws Exception {
    UserFx user = bootstrap("empty");
    createCancelled(user, "Cancelada", "90.00", TOMORROW);
    createReceived(user, "Recebida", "40.00", YESTERDAY, TODAY_TEXT);

    ReceivablePageResponse page = listReceivables(user.token());
    assertThat(page.items()).isEmpty();
    assertThat(page.totalItems()).isZero();
    assertThat(page.totalPages()).isZero();
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().overdueAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldPaginateWithoutLosingOrDuplicatingItems() throws Exception {
    UserFx user = bootstrap("page");
    IncomeResponse first = createIncome(user, "Um", "10.00", "2026-09-01");
    IncomeResponse second = createIncome(user, "Dois", "20.00", "2026-09-02");
    IncomeResponse third = createIncome(user, "Tres", "30.00", "2026-09-03");

    ReceivablePageResponse page0 =
        listReceivables(user.token(), "page", "0", "size", "2", "sort", "expectedDate");
    assertThat(page0.page()).isZero();
    assertThat(page0.size()).isEqualTo(2);
    assertThat(page0.totalItems()).isEqualTo(3);
    assertThat(page0.totalPages()).isEqualTo(2);
    assertThat(ids(page0)).containsExactly(first.id(), second.id());

    ReceivablePageResponse page1 =
        listReceivables(user.token(), "page", "1", "size", "2", "sort", "expectedDate");
    assertThat(ids(page1)).containsExactly(third.id());
    assertThat(page1.totalItems()).isEqualTo(3);
    assertThat(ids(page0)).doesNotContainAnyElementsOf(ids(page1));
  }

  @Test
  void shouldRejectInvalidPageAndSizeAndAcceptSize100() throws Exception {
    UserFx user = bootstrap("size");
    createIncome(user, "Aberta", "10.00", TOMORROW);

    ReceivablePageResponse page = listReceivables(user.token(), "size", "100");
    assertThat(page.size()).isEqualTo(100);
    assertThat(page.items()).hasSize(1);

    rejectBusiness(user.token(), "size", "101");
    rejectBusiness(user.token(), "size", "0");
    rejectBusiness(user.token(), "page", "-1");
  }

  @Test
  void shouldRejectUnknownQueryParameters() throws Exception {
    UserFx user = bootstrap("unknown");
    rejectInvalid(user.token(), "foo", "bar");
    rejectInvalid(user.token(), "status", "EXPECTED", "foo", "bar");
    rejectInvalid(user.token(), "year", "2026");
    rejectInvalid(user.token(), "month", "8");
    rejectInvalid(user.token(), "search", "salario");
  }

  @Test
  void shouldApplyCombinedCompatibleFiltersAsIntersection() throws Exception {
    UserFx user = bootstrap("combo");
    UUID extra = createIncomeCategory(user.token(), "Extra").id();
    IncomeResponse match = createIncome(user, "Alvo", "25.00", "2026-08-10");
    createIncome(user, extra, "Outra categoria", "25.00", "2026-08-10");
    createIncome(user, "Futura mesma categoria", "25.00", "2026-09-10");
    createIncome(user, "Hoje mesma categoria", "25.00", TODAY_TEXT);

    ReceivablePageResponse page =
        listReceivables(
            user.token(),
            "status",
            "EXPECTED",
            "overdue",
            "true",
            "categoryId",
            user.categoryId().toString(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-08-01",
            "endDate",
            "2026-08-31");
    assertThat(ids(page)).containsExactly(match.id());
    assertThat(page.summary().overdueAmount()).isEqualByComparingTo("25.00");
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldComputeSummaryFromFilteredUniverseNotPage() throws Exception {
    UserFx user = bootstrap("summary-page");
    for (int day = 1; day <= 10; day++) {
      String date = "2026-09-" + (day < 10 ? "0" + day : day);
      createIncome(user, "Item-" + day, "10.00", date);
    }

    ReceivablePageResponse page =
        listReceivables(user.token(), "page", "0", "size", "2", "sort", "expectedDate");
    assertThat(page.items()).hasSize(2);
    assertThat(page.totalItems()).isEqualTo(10);
    assertThat(page.totalPages()).isEqualTo(5);
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("100.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("100.00");
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldSummarizeExpectedFuturesAndOverdues() throws Exception {
    UserFx user = bootstrap("summary-expected");
    createIncome(user, "F1", "10.00", TOMORROW);
    createIncome(user, "F2", "20.00", "2026-09-01");
    createIncome(user, "F3", "30.00", TODAY_TEXT);
    createIncome(user, "V1", "40.00", YESTERDAY);
    createIncome(user, "V2", "5.00", "2026-08-01");

    ReceivablePageResponse page = listReceivables(user.token());
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("60.00");
    assertThat(page.summary().overdueAmount()).isEqualByComparingTo("45.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("105.00");
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("0.00");
  }

  @Test
  void shouldZeroReceivableTotalsWhenFilteringReceived() throws Exception {
    UserFx user = bootstrap("summary-received");
    createIncome(user, "Aberta", "100.00", TOMORROW);
    createReceived(user, "R1", "15.00", YESTERDAY, TODAY_TEXT);
    createReceived(user, "R2", "25.00", YESTERDAY, "2026-08-01");

    ReceivablePageResponse page = listReceivables(user.token(), "status", "RECEIVED");
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().overdueAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("0.00");
    assertThat(page.summary().receivedAmount()).isEqualByComparingTo("40.00");
  }

  @Test
  void shouldRespectPeriodInSummary() throws Exception {
    UserFx user = bootstrap("summary-period");
    createIncome(user, "Dentro", "12.00", "2026-09-15");
    createIncome(user, "Fora", "99.00", "2026-10-01");

    ReceivablePageResponse page =
        listReceivables(
            user.token(),
            "dateType",
            "EXPECTED",
            "startDate",
            "2026-09-01",
            "endDate",
            "2026-09-30");
    assertThat(page.summary().futureAmount()).isEqualByComparingTo("12.00");
    assertThat(page.summary().totalReceivableAmount()).isEqualByComparingTo("12.00");
  }

  @Test
  void shouldReclassifyReversedIncomeAsExpected() throws Exception {
    UserFx user = bootstrap("reverse");
    IncomeResponse received = createReceived(user, "Estorno", "60.00", YESTERDAY, TODAY_TEXT);
    assertThat(ids(listReceivables(user.token()))).doesNotContain(received.id());

    mockMvc
        .perform(
            post("/api/v1/incomes/" + received.id() + "/reverse")
                .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
        .andExpect(status().isOk());

    ReceivableItemResponse item = item(listReceivables(user.token()), received.id());
    assertThat(item.status()).isEqualTo("EXPECTED");
    assertThat(item.overdue()).isTrue();
    assertThat(item.accountId()).isNull();
    assertThat(item.receivedDate()).isNull();
  }

  private ReceivablePageResponse listReceivables(String token, String... params) throws Exception {
    var request = get("/api/v1/receivables").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    return read(result, ReceivablePageResponse.class);
  }

  private void rejectInvalid(String token, String... params) throws Exception {
    var request = get("/api/v1/receivables").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  private void rejectBusiness(String token, String... params) throws Exception {
    var request = get("/api/v1/receivables").header(HttpHeaders.AUTHORIZATION, bearer(token));
    for (int i = 0; i < params.length; i += 2) {
      request = request.param(params[i], params[i + 1]);
    }
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
  }

  private UserFx bootstrap(String prefix) throws Exception {
    String token = registerAndLogin(uniqueEmail(prefix));
    UUID categoryId = createIncomeCategory(token, "Salário").id();
    UUID accountId = createAccount(token, "Conta", "BANK_ACCOUNT", "5000.00").id();
    return new UserFx(token, categoryId, accountId);
  }

  private IncomeResponse createIncome(
      UserFx user, String description, String amount, String expectedDate) throws Exception {
    return createIncome(user, user.categoryId(), description, amount, expectedDate);
  }

  private IncomeResponse createIncome(
      UserFx user, UUID categoryId, String description, String amount, String expectedDate)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createIncomeJson(categoryId, description, amount, expectedDate)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private IncomeResponse createReceived(
      UserFx user, String description, String amount, String expectedDate, String receivedDate)
      throws Exception {
    return createReceived(user, user.accountId(), description, amount, expectedDate, receivedDate);
  }

  private IncomeResponse createReceived(
      UserFx user,
      UUID accountId,
      String description,
      String amount,
      String expectedDate,
      String receivedDate)
      throws Exception {
    IncomeResponse created = createIncome(user, description, amount, expectedDate);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes/" + created.id() + "/receive")
                    .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(receiveJson(accountId, receivedDate)))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private IncomeResponse createCancelled(
      UserFx user, String description, String amount, String expectedDate) throws Exception {
    IncomeResponse created = createIncome(user, description, amount, expectedDate);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/incomes/" + created.id() + "/cancel")
                    .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
            .andExpect(status().isOk())
            .andReturn();
    return read(result, IncomeResponse.class);
  }

  private void persistResponsible(UUID incomeId, ResponsibleType type, String name) {
    Income income = incomeRepository.findById(incomeId).orElseThrow();
    income.setResponsibleType(type);
    income.setResponsibleName(name);
    incomeRepository.saveAndFlush(income);
  }

  private CategoryResponse createIncomeCategory(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/categories")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"INCOME"}
                        """
                            .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, CategoryResponse.class);
  }

  private AccountResponse createAccount(String token, String name, String type, String initial)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"%s","initialBalance":%s}
                        """
                            .formatted(name, type, initial)))
            .andExpect(status().isCreated())
            .andReturn();
    return read(result, AccountResponse.class);
  }

  private String registerAndLogin(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Alice","email":"%s","password":"senha-segura"}
                    """
                        .formatted(email)))
        .andExpect(status().isCreated());
    MvcResult result =
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
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private List<UUID> ids(ReceivablePageResponse page) {
    return page.items().stream().map(ReceivableItemResponse::id).toList();
  }

  private ReceivableItemResponse item(ReceivablePageResponse page, UUID id) {
    return page.items().stream().filter(line -> line.id().equals(id)).findFirst().orElseThrow();
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

  private static String createIncomeJson(
      UUID categoryId, String description, String amount, String expectedDate) {
    return """
        {"categoryId":"%s","description":"%s","amount":%s,"expectedDate":"%s"}
        """
        .formatted(categoryId, description, amount, expectedDate);
  }

  private static String receiveJson(UUID accountId, String receivedDate) {
    return """
        {"accountId":"%s","receivedDate":"%s"}
        """
        .formatted(accountId, receivedDate);
  }

  record UserFx(String token, UUID categoryId, UUID accountId) {}

  record ReceivablePageResponse(
      List<ReceivableItemResponse> items,
      Summary summary,
      int page,
      int size,
      long totalItems,
      int totalPages) {}

  record Summary(
      BigDecimal futureAmount,
      BigDecimal overdueAmount,
      BigDecimal totalReceivableAmount,
      BigDecimal receivedAmount) {}

  record ReceivableItemResponse(
      UUID id,
      UUID categoryId,
      UUID accountId,
      ResponsibleType responsibleType,
      String responsibleName,
      String description,
      BigDecimal amount,
      LocalDate expectedDate,
      LocalDate receivedDate,
      String status,
      boolean overdue) {}
}
