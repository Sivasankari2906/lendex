package com.example.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;

/** Runs the main flows and the interest due-date logic against a real PostgreSQL server. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop" })
class PostgresIntegrationTest {

    @Autowired MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();

    private String register(String name) throws Exception {
        String r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"password\":\"Passw0rd@1\",\"email\":\"" + name + "@example.com\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(r).get("token").asText();
    }

    private long loan(String auth, String principal, String rate, LocalDate trackingStart) throws Exception {
        long b = json.readTree(mvc.perform(post("/api/borrowers").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ravi\",\"phone\":\"9111111111\"}")).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String loan = mvc.perform(post("/api/borrowers/" + b + "/loans").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"principal\":\"" + principal + "\",\"monthlyInterestRate\":\"" + rate + "\",\"issuedDate\":\"" + trackingStart
                        + "\",\"trackingStartDate\":\"" + trackingStart + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(loan).get("id").asLong();
    }

    private void pay(String auth, long loanId, String amount, LocalDate month) throws Exception {
        mvc.perform(post("/api/loans/" + loanId + "/payments").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"" + amount + "\",\"date\":\"" + month + "\"}")).andExpect(status().isOk());
    }

    private int remindersFor(String auth) throws Exception {
        JsonNode list = json.readTree(mvc.perform(get("/api/reminders").header("Authorization", auth)).andReturn().getResponse().getContentAsString());
        return list.size();
    }

    @Test
    void unpaidInterestFromEarlierMonthsShowsAsOverdue() throws Exception {
        String auth = register("pg-overdue");
        loan(auth, "10000", "2", YearMonth.now().minusMonths(3).atDay(5));
        org.junit.jupiter.api.Assertions.assertEquals(1, remindersFor(auth));
    }

    @Test
    void payingEveryMonthsFullInterestClearsTheReminder() throws Exception {
        String auth = register("pg-paid");
        LocalDate start = YearMonth.now().minusMonths(3).atDay(5);
        long id = loan(auth, "10000", "2", start);
        for (LocalDate m = start; m.isBefore(LocalDate.now().withDayOfMonth(1)); m = m.plusMonths(1)) {
            pay(auth, id, "200", m); // 2% of 10,000
        }
        org.junit.jupiter.api.Assertions.assertEquals(0, remindersFor(auth));
    }

    @Test
    void partialInterestStillShowsAsOverdue() throws Exception {
        String auth = register("pg-partial");
        LocalDate start = YearMonth.now().minusMonths(2).atDay(5);
        long id = loan(auth, "10000", "2", start);
        for (LocalDate m = start; m.isBefore(LocalDate.now().withDayOfMonth(1)); m = m.plusMonths(1)) {
            pay(auth, id, "150", m); // less than the 200 due
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, remindersFor(auth));
    }

    @Test
    void postponingHidesTheReminder() throws Exception {
        String auth = register("pg-postpone");
        long id = loan(auth, "10000", "2", YearMonth.now().minusMonths(3).atDay(5));
        mvc.perform(post("/api/loans/" + id + "/postpone").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":\"7\"}")).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0, remindersFor(auth));
    }

    @Test
    void deleteBorrowerWithPaymentsWorksOnPostgres() throws Exception {
        String auth = register("pg-delete");
        long id = loan(auth, "5000", "1", LocalDate.now().minusMonths(1));
        pay(auth, id, "50", LocalDate.now());
        long borrowerId = json.readTree(mvc.perform(get("/api/borrowers").header("Authorization", auth)).andReturn()
                .getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(delete("/api/borrowers/" + borrowerId).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/loans").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void otpFallsBackToEmailWhenSmsIsNotConfiguredAndResendIsThrottled() throws Exception {
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"otp@example.com\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.channel").value("email"));
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"otp@example.com\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isBadRequest());
    }
}
