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
class TransactionApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void transactionCrudFiltersOwnershipAndBalancesWork() throws Exception {
        String email = "session7-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "session7-other-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Session 7 User", "email", email, "password", password)))
                .andExpect(status().isCreated());
        String token = login(email, password);

        MvcResult accountResult = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Session 7 Test Account", "type", "CASH", "balance", 1000.00)))
                .andExpect(status().isCreated()).andReturn();
        long accountId = body(accountResult).get("id").asLong();

        MvcResult expenseResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("100.00", "EXPENSE", "Food", today, accountId)))
                .andExpect(status().isCreated()).andReturn();
        long expenseId = body(expenseResult).get("id").asLong();
        assertBalance(token, accountId, "900.00");
        mockMvc.perform(delete("/api/accounts/{id}", accountId).header("Authorization", bearer(token)))
                .andExpect(status().isConflict());

        MvcResult incomeResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("50.00", "INCOME", "Salary", today, accountId)))
                .andExpect(status().isCreated()).andReturn();
        long incomeId = body(incomeResult).get("id").asLong();
        assertBalance(token, accountId, "950.00");

        MvcResult secondAccountResult = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Session 7 Second Account", "type", "SAVINGS", "balance", 200.00)))
                .andExpect(status().isCreated()).andReturn();
        long secondAccountId = body(secondAccountResult).get("id").asLong();
        MvcResult secondAccountExpenseResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("25.00", "EXPENSE", "Transportation", today, secondAccountId)))
                .andExpect(status().isCreated()).andReturn();
        long secondAccountExpenseId = body(secondAccountExpenseResult).get("id").asLong();
        assertBalance(token, secondAccountId, "175.00");

        JsonNode allTransactions = body(mockMvc.perform(get("/api/transactions")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals(3, allTransactions.size());
        assertEquals(secondAccountExpenseId, allTransactions.get(0).get("id").asLong());
        assertEquals("Session 7 Second Account", allTransactions.get(0).get("accountName").asText());
        assertEquals(2, body(mockMvc.perform(get("/api/transactions").param("accountId", String.valueOf(accountId))
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn()).size());
        assertEquals(1, body(mockMvc.perform(get("/api/transactions").param("accountId", String.valueOf(secondAccountId))
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn()).size());
        assertEquals(2, body(mockMvc.perform(get("/api/transactions").param("type", "EXPENSE")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn()).size());
        assertEquals(1, body(mockMvc.perform(get("/api/transactions").param("category", "Food")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn()).size());
        assertEquals(3, body(mockMvc.perform(get("/api/transactions").param("date", today.toString())
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn()).size());

        mockMvc.perform(get("/api/transactions/{id}", expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/transactions/{id}", expenseId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("80.00", "INCOME", "Salary", today, secondAccountId)))
                .andExpect(status().isOk());
        assertBalance(token, accountId, "1050.00");
        assertBalance(token, secondAccountId, "255.00");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Other User", "email", otherEmail, "password", password)))
                .andExpect(status().isCreated());
        String otherToken = login(otherEmail, password);
        assertEquals(0, body(mockMvc.perform(get("/api/transactions")
                .header("Authorization", bearer(otherToken))).andExpect(status().isOk()).andReturn()).size());
        mockMvc.perform(get("/api/transactions/{id}", expenseId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/accounts/{id}", accountId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/transactions/{id}", expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        assertBalance(token, accountId, "1050.00");
        assertBalance(token, secondAccountId, "175.00");
        mockMvc.perform(delete("/api/transactions/{id}", incomeId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        assertBalance(token, accountId, "1000.00");
        mockMvc.perform(delete("/api/transactions/{id}", secondAccountExpenseId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        assertBalance(token, secondAccountId, "200.00");
        mockMvc.perform(get("/api/transactions/{id}", incomeId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/accounts/{id}", accountId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/accounts/{id}", secondAccountId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void authAndTransactionValidationRejectInvalidRequests() throws Exception {
        String email = "session14-validation-" + UUID.randomUUID() + "@example.com";
        String password = "FinanceFlow-test-123!";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Validation User", "email", email, "password", password)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Duplicate", "email", email, "password", password)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", "wrong-password")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());

        String token = login(email, password);
        MvcResult account = mockMvc.perform(post("/api/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Validation Account", "type", "BANK", "balance", 1000.00)))
                .andExpect(status().isCreated()).andReturn();
        long accountId = body(account).get("id").asLong();
        mockMvc.perform(post("/api/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("-1.00", "EXPENSE", "Food", LocalDate.now(), accountId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("10.00", "INVALID", "Food", LocalDate.now(), accountId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody("10.00", "EXPENSE", "Food", LocalDate.now(), Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    private String login(
String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", password)))
                .andExpect(status().isOk()).andReturn();
        String token = body(result).get("token").asText();
        assertNotNull(token);
        return token;
    }

    private void assertBalance(String token, long accountId, String expected) throws Exception {
        JsonNode account = body(mockMvc.perform(get("/api/accounts/{id}", accountId)
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertEquals(0, new BigDecimal(expected).compareTo(account.get("balance").decimalValue()));
    }

    private String transactionBody(String amount, String type, String category, LocalDate date, long accountId)
            throws Exception {
        return json("amount", new BigDecimal(amount), "type", type, "category", category,
                "description", "Session 7 integration test", "date", date.toString(), "accountId", accountId);
    }

    private String json(Object... properties) throws Exception {
        var values = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < properties.length; i += 2) values.put((String) properties[i], properties[i + 1]);
        return objectMapper.writeValueAsString(values);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) { return "Bearer " + token; }
}


