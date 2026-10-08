package com.financeflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavingsGoalDashboardApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void savingsGoalsSupportCrudProgressValidationAndOwnership() throws Exception {
        String email = "session9-goal-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "session9-goal-other-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        String token = registerAndLogin(email, password, "Goal User");
        LocalDate targetDate = LocalDate.now().plusMonths(6);

        MvcResult created = mockMvc.perform(post("/api/savings-goals")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Buy Laptop", "50000.00", "25000.00", targetDate)))
                .andExpect(status().isCreated()).andReturn();
        long goalId = body(created).get("id").asLong();
        assertProgress(body(created), "50000.00", "25000.00", "50.00");

        mockMvc.perform(post("/api/savings-goals").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Invalid goal", "1000.00", "-1.00", targetDate)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/savings-goals/{id}/progress", goalId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentAmount", new BigDecimal("-5.00"))))
                .andExpect(status().isBadRequest());

        JsonNode edited = body(mockMvc.perform(put("/api/savings-goals/{id}", goalId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Emergency Fund", "100000.00", "25000.00", targetDate)))
                .andExpect(status().isOk()).andReturn());
        assertProgress(edited, "100000.00", "25000.00", "25.00");

        JsonNode updatedProgress = body(mockMvc.perform(put("/api/savings-goals/{id}/progress", goalId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json("currentAmount", new BigDecimal("50000.00"))))
                .andExpect(status().isOk()).andReturn());
        assertProgress(updatedProgress, "100000.00", "50000.00", "50.00");
        assertEquals(1, body(mockMvc.perform(get("/api/savings-goals").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn()).size());
        mockMvc.perform(get("/api/savings-goals/{id}", goalId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        String otherToken = registerAndLogin(otherEmail, password, "Other Goal User");
        assertEquals(0, body(mockMvc.perform(get("/api/savings-goals").header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk()).andReturn()).size());
        mockMvc.perform(get("/api/savings-goals/{id}", goalId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/savings-goals/{id}", goalId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/savings-goals/{id}", goalId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardCombinesUserScopedMonthlyTotalsAndFeatureSummaries() throws Exception {
        String email = "session9-dashboard-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "session9-dashboard-other-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        String token = registerAndLogin(email, password, "Dashboard User");
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        long accountId = createAccount(token, "10000.00");

        createTransaction(token, accountId, "6000.00", "INCOME", "Salary", today);
        createTransaction(token, accountId, "1200.00", "EXPENSE", "Food", today);
        createTransaction(token, accountId, "800.00", "EXPENSE", "Bills", today);
        createTransaction(token, accountId, "5000.00", "INCOME", "Salary", today.minusMonths(1));

        mockMvc.perform(post("/api/budgets").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("category", "Food", "amount", new BigDecimal("2000.00"),
                                "month", thisMonth.getMonthValue(), "year", thisMonth.getYear())))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/budgets").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("category", "Bills", "amount", new BigDecimal("1000.00"),
                                "month", thisMonth.minusMonths(1).getMonthValue(),
                                "year", thisMonth.minusMonths(1).getYear())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recurring-expenses").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Internet", "amount", new BigDecimal("799.00"),
                                "category", "Bills", "frequency", "MONTHLY",
                                "nextPaymentDate", today.plusDays(3).toString())))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/recurring-expenses").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Old payment", "amount", new BigDecimal("200.00"),
                                "category", "Bills", "frequency", "WEEKLY",
                                "nextPaymentDate", today.minusDays(1).toString())))
                .andExpect(status().isCreated());

        MvcResult goalResult = mockMvc.perform(post("/api/savings-goals")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Emergency Fund", "50000.00", "25000.00", today.plusYears(1))))
                .andExpect(status().isCreated()).andReturn();
        long goalId = body(goalResult).get("id").asLong();

        JsonNode dashboard = body(mockMvc.perform(get("/api/dashboard")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals(thisMonth.getMonthValue(), dashboard.get("month").asInt());
        assertEquals(thisMonth.getYear(), dashboard.get("year").asInt());
        assertMoney(dashboard, "totalAccountBalance", "19000.00");
        assertMoney(dashboard, "currentMonthIncome", "6000.00");
        assertMoney(dashboard, "currentMonthExpenses", "2000.00");
        assertMoney(dashboard, "currentMonthSavings", "4000.00");
        assertMoney(dashboard, "savingsRate", "66.67");

        JsonNode budgetSummary = dashboard.get("budgetSummary");
        assertEquals(1, budgetSummary.size());
        assertEquals("Food", budgetSummary.get(0).get("category").asText());
        assertMoney(budgetSummary.get(0), "amountSpent", "1200.00");
        assertMoney(budgetSummary.get(0), "remainingAmount", "800.00");

        JsonNode upcoming = dashboard.get("upcomingPayments");
        assertEquals(1, upcoming.size());
        assertEquals("Internet", upcoming.get(0).get("name").asText());
        JsonNode goals = dashboard.get("savingsGoals");
        assertEquals(1, goals.size());
        assertEquals(goalId, goals.get(0).get("id").asLong());
        assertMoney(goals.get(0), "progressPercentage", "50.00");
        JsonNode expenseByCategory = dashboard.get("expenseByCategory");
        assertEquals(2, expenseByCategory.size());
        assertEquals("Bills", expenseByCategory.get(0).get("category").asText());
        assertMoney(expenseByCategory.get(0), "amount", "800.00");
        assertEquals("Food", expenseByCategory.get(1).get("category").asText());
        assertMoney(expenseByCategory.get(1), "amount", "1200.00");
        JsonNode recent = dashboard.get("recentTransactions");
        assertEquals(4, recent.size());
        assertEquals("Bills", recent.get(0).get("category").asText());
        assertEquals("Session 9 account", recent.get(0).get("accountName").asText());

        String otherToken = registerAndLogin(otherEmail, password, "Empty Dashboard User");
        JsonNode emptyDashboard = body(mockMvc.perform(get("/api/dashboard")
                .header("Authorization", bearer(otherToken))).andExpect(status().isOk()).andReturn());
        assertMoney(emptyDashboard, "totalAccountBalance", "0.00");
        assertMoney(emptyDashboard, "currentMonthIncome", "0.00");
        assertMoney(emptyDashboard, "currentMonthExpenses", "0.00");
        assertMoney(emptyDashboard, "currentMonthSavings", "0.00");
        assertMoney(emptyDashboard, "savingsRate", "0.00");
        assertEquals(0, emptyDashboard.get("budgetSummary").size());
        assertEquals(0, emptyDashboard.get("upcomingPayments").size());
        assertEquals(0, emptyDashboard.get("savingsGoals").size());
        assertEquals(0, emptyDashboard.get("recentTransactions").size());
        assertEquals(0, emptyDashboard.get("expenseByCategory").size());
    }

    private String registerAndLogin(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name, "email", email, "password", password)))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", password)))
                .andExpect(status().isOk()).andReturn();
        String token = body(result).get("token").asText();
        assertNotNull(token);
        return token;
    }

    private long createAccount(String token, String balance) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Session 9 account", "type", "CASH",
                                "balance", new BigDecimal(balance))))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    private void createTransaction(String token, long accountId, String amount, String type,
                                   String category, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("amount", new BigDecimal(amount), "type", type, "category", category,
                                "description", "Session 9 dashboard test", "date", date.toString(),
                                "accountId", accountId)))
                .andExpect(status().isCreated());
    }

    private String goalBody(String name, String target, String current, LocalDate date) throws Exception {
        return json("name", name, "targetAmount", new BigDecimal(target),
                "currentAmount", new BigDecimal(current), "targetDate", date.toString());
    }

    private void assertProgress(JsonNode goal, String target, String current, String progress) {
        assertMoney(goal, "targetAmount", target);
        assertMoney(goal, "currentAmount", current);
        assertMoney(goal, "progressPercentage", progress);
    }

    private void assertMoney(JsonNode object, String field, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(object.get(field).decimalValue()), field);
    }

    private String json(Object... properties) throws Exception {
        var values = new LinkedHashMap<String, Object>();
        for (int i = 0; i < properties.length; i += 2) values.put((String) properties[i], properties[i + 1]);
        return objectMapper.writeValueAsString(values);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) { return "Bearer " + token; }
}

