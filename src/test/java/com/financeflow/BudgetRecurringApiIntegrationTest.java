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
class BudgetRecurringApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void budgetsCalculateUsageEnforceOwnershipAndSupportCrud() throws Exception {
        String email = "session8-budget-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "session8-other-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();
        String token = registerAndLogin(email, password, "Budget User");
        long accountId = createAccount(token);

        createTransaction(token, accountId, "2000.00", "EXPENSE", "Food", today);
        createTransaction(token, accountId, "1500.00", "EXPENSE", "Food", today);
        createTransaction(token, accountId, "3000.00", "INCOME", "Food", today);
        createTransaction(token, accountId, "500.00", "EXPENSE", "Food", today.minusMonths(1));

        MvcResult created = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody("Food", "5000.00", month, year)))
                .andExpect(status().isCreated()).andReturn();
        long budgetId = body(created).get("id").asLong();
        assertBudgetUsage(body(created), "5000.00", "3500.00", "1500.00", "70.00", "On track.");

        JsonNode usage = body(mockMvc.perform(get("/api/budgets/{id}/usage", budgetId)
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertBudgetUsage(usage, "5000.00", "3500.00", "1500.00", "70.00", "On track.");
        assertEquals(1, body(mockMvc.perform(get("/api/budgets").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn()).size());

        JsonNode almost = body(mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody("Food", "4375.00", month, year)))
                .andExpect(status().isOk()).andReturn());
        assertBudgetUsage(almost, "4375.00", "3500.00", "875.00", "80.00", "Budget is almost exhausted.");

        JsonNode atLimit = body(mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody("Food", "3500.00", month, year)))
                .andExpect(status().isOk()).andReturn());
        assertBudgetUsage(atLimit, "3500.00", "3500.00", "0.00", "100.00", "Budget exceeded.");

        JsonNode exceeded = body(mockMvc.perform(put("/api/budgets/{id}", budgetId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(budgetBody("Food", "3000.00", month, year)))
                .andExpect(status().isOk()).andReturn());
        assertBudgetUsage(exceeded, "3000.00", "3500.00", "-500.00", "116.67", "Budget exceeded.");
        mockMvc.perform(get("/api/budgets/{id}", budgetId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        String otherToken = registerAndLogin(otherEmail, password, "Other User");
        assertEquals(0, body(mockMvc.perform(get("/api/budgets").header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk()).andReturn()).size());
        mockMvc.perform(get("/api/budgets/{id}", budgetId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/budgets/{id}", budgetId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/budgets/{id}", budgetId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void recurringExpensesSupportCrudUpcomingSortAndOwnership() throws Exception {
        String email = "session8-recurring-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "session8-recurring-other-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        LocalDate today = LocalDate.now();
        String token = registerAndLogin(email, password, "Recurring User");

        long laterId = createRecurring(token, "Netflix", "15.99", "Entertainment", "MONTHLY", today.plusDays(8));
        long soonerId = createRecurring(token, "Rent", "1200.00", "Bills", "MONTHLY", today.plusDays(2));
        long pastId = createRecurring(token, "Old Internet", "40.00", "Bills", "YEARLY", today.minusDays(1));

        JsonNode all = body(mockMvc.perform(get("/api/recurring-expenses")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals(3, all.size());
        assertEquals(pastId, all.get(0).get("id").asLong());

        JsonNode upcoming = body(mockMvc.perform(get("/api/recurring-expenses/upcoming")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals(2, upcoming.size());
        assertEquals(soonerId, upcoming.get(0).get("id").asLong());
        assertEquals(laterId, upcoming.get(1).get("id").asLong());

        JsonNode edited = body(mockMvc.perform(put("/api/recurring-expenses/{id}", laterId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(recurringBody("Internet", "75.00", "Bills", "YEARLY", today.plusDays(5))))
                .andExpect(status().isOk()).andReturn());
        assertEquals("Internet", edited.get("name").asText());
        assertEquals(0, new BigDecimal("75.00").compareTo(edited.get("amount").decimalValue()));
        assertEquals("YEARLY", edited.get("frequency").asText());
        assertEquals(today.plusDays(5).toString(), edited.get("nextPaymentDate").asText());
        assertEquals(laterId, body(mockMvc.perform(get("/api/recurring-expenses/upcoming")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn()).get(1).get("id").asLong());

        String otherToken = registerAndLogin(otherEmail, password, "Other User");
        assertEquals(0, body(mockMvc.perform(get("/api/recurring-expenses")
                .header("Authorization", bearer(otherToken))).andExpect(status().isOk()).andReturn()).size());
        mockMvc.perform(get("/api/recurring-expenses/{id}", laterId)
                .header("Authorization", bearer(otherToken))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recurring-expenses/{id}", laterId)
                .header("Authorization", bearer(token))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/recurring-expenses/{id}", soonerId)
                .header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/recurring-expenses/{id}", laterId)
                .header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/recurring-expenses/{id}", pastId)
                .header("Authorization", bearer(token))).andExpect(status().isNoContent());
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

    private long createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Session 8 Account", "type", "CASH", "balance", 50000.00)))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    private void createTransaction(String token, long accountId, String amount, String type,
                                   String category, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("amount", new BigDecimal(amount), "type", type, "category", category,
                                "description", "Session 8 budget test", "date", date.toString(),
                                "accountId", accountId)))
                .andExpect(status().isCreated());
    }

    private long createRecurring(String token, String name, String amount, String category,
                                 String frequency, LocalDate date) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(recurringBody(name, amount, category, frequency, date)))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asLong();
    }

    private String budgetBody(String category, String amount, int month, int year) throws Exception {
        return json("category", category, "amount", new BigDecimal(amount), "month", month, "year", year);
    }

    private String recurringBody(String name, String amount, String category, String frequency, LocalDate date)
            throws Exception {
        return json("name", name, "amount", new BigDecimal(amount), "category", category,
                "frequency", frequency, "nextPaymentDate", date.toString());
    }

    private void assertBudgetUsage(JsonNode result, String amount, String spent, String remaining,
                                   String percentage, String message) {
        assertEquals(0, new BigDecimal(amount).compareTo(result.get("amount").decimalValue()));
        assertEquals(0, new BigDecimal(spent).compareTo(result.get("amountSpent").decimalValue()));
        assertEquals(0, new BigDecimal(remaining).compareTo(result.get("remainingAmount").decimalValue()));
        assertEquals(0, new BigDecimal(percentage).compareTo(result.get("percentageUsed").decimalValue()));
        assertEquals(message, result.get("status").asText());
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
