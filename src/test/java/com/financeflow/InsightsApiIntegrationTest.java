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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InsightsApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void insightsApplyRulesToOnlyTheAuthenticatedUsersTransactions() throws Exception {
        String email = "session10-insights-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "session10-insights-other-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        String token = registerAndLogin(email, password, "Insights User");
        YearMonth current = YearMonth.now();
        LocalDate today = LocalDate.now();
        long accountId = createAccount(token);

        addTransaction(token, accountId, "4000.00", "INCOME", "Salary", today);
        addTransaction(token, accountId, "2000.00", "EXPENSE", "Food", today);
        addTransaction(token, accountId, "3000.00", "EXPENSE", "Bills", today);
        addTransaction(token, accountId, "1000.00", "INCOME", "Salary", current.minusMonths(1).atDay(10));
        addTransaction(token, accountId, "1000.00", "EXPENSE", "Food", current.minusMonths(1).atDay(10));
        addTransaction(token, accountId, "2000.00", "EXPENSE", "Bills", current.minusMonths(1).atDay(10));

        addBudget(token, "Food", "1500.00", current);
        addBudget(token, "Bills", "3750.00", current);
        mockMvc.perform(post("/api/recurring-expenses").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Internet", "amount", new BigDecimal("799.00"),
                                "category", "Bills", "frequency", "MONTHLY",
                                "nextPaymentDate", today.plusDays(3).toString())))
                .andExpect(status().isCreated());

        JsonNode insights = body(mockMvc.perform(get("/api/insights").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn());
        assertEquals(8, insights.size());
        assertContains(insights, "Your expenses increased compared with last month.");
        assertContains(insights, "Your spending on Food increased this month.");
        assertContains(insights, "Your spending on Bills increased this month.");
        assertContains(insights, "You have exceeded your Food budget.");
        assertContains(insights, "Your Bills budget is almost exhausted.");
        assertContains(insights, "Your expenses are higher than your income this month.");
        assertContains(insights, "You have an upcoming recurring payment.");
        assertContains(insights, "Your savings rate improved this month.");

        String otherToken = registerAndLogin(otherEmail, password, "Other User");
        JsonNode otherInsights = body(mockMvc.perform(get("/api/insights")
                .header("Authorization", bearer(otherToken))).andExpect(status().isOk()).andReturn());
        assertEquals(0, otherInsights.size());
    }

    private String registerAndLogin(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name, "email", email, "password", password)))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", password)))
                .andExpect(status().isOk()).andReturn();
        return body(result).get("token").asText();
    }

    private long createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Insights test account", "type", "CASH", "balance", 50000.00)))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    private void addTransaction(String token, long accountId, String amount, String type,
                                String category, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("amount", new BigDecimal(amount), "type", type, "category", category,
                                "description", "Session 10 rule test", "date", date.toString(),
                                "accountId", accountId)))
                .andExpect(status().isCreated());
    }

    private void addBudget(String token, String category, String amount, YearMonth period) throws Exception {
        mockMvc.perform(post("/api/budgets").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("category", category, "amount", new BigDecimal(amount),
                                "month", period.getMonthValue(), "year", period.getYear())))
                .andExpect(status().isCreated());
    }

    private void assertContains(JsonNode values, String expected) {
        boolean found = false;
        for (JsonNode value : values) found |= expected.equals(value.asText());
        assertTrue(found, "Expected insight not found: " + expected);
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
