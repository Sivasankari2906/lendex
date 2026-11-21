package com.example.backend;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/** End-to-end API checks for the security and data fixes, against an in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiFixesTest {

    @Autowired MockMvc mvc;
    final ObjectMapper json = new ObjectMapper();

    private String register(String username) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Passw0rd@1\",\"email\":\"" + username + "@example.com\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private long createBorrowerWithLoan(String token) throws Exception {
        long borrowerId = body(mvc.perform(post("/api/borrowers").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ravi\",\"phone\":\"9111111111\"}"))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();
        mvc.perform(post("/api/borrowers/" + borrowerId + "/loans").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"principal\":\"10000\",\"monthlyInterestRate\":\"2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", notNullValue()));
        return borrowerId;
    }

    private long firstLoanId(String token) throws Exception {
        return body(mvc.perform(get("/api/loans").header("Authorization", "Bearer " + token)).andReturn()).get(0).get("id").asLong();
    }

    @Test
    void protectedEndpointsWorkWithATokenAndAreBlockedWithout() throws Exception {
        String token = register("protect1");
        mvc.perform(get("/api/loans").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/loans")).andExpect(status().isForbidden());
    }

    @Test
    void tokenSignedWithTheOldHardcodedSecretIsRejected() throws Exception {
        register("forge1");
        String forged = Jwts.builder().setSubject("forge1").setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor("replace_this_with_a_strong_secret_in_env".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/loans").header("Authorization", "Bearer " + forged)).andExpect(status().isForbidden());
    }

    @Test
    void authErrorsReturnProperStatusesAndMessages() throws Exception {
        register("dupe1");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dupe1\",\"password\":\"x\",\"email\":\"other@example.com\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("That username is already taken."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dupe1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid username or password."));
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If that email is registered, a password reset link has been sent."));
    }

    @Test
    void zeroAndNegativePaymentsAreRejected() throws Exception {
        String token = register("pay1");
        createBorrowerWithLoan(token);
        long loanId = firstLoanId(token);
        for (String amount : new String[] { "-50", "0" }) {
            mvc.perform(post("/api/loans/" + loanId + "/payments").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"" + amount + "\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Payment amount must be greater than zero."));
        }
        String emi = "{\"borrowerName\":\"V\",\"totalAmount\":\"60000\",\"givenInCash\":\"57000\",\"givenDate\":\"" + LocalDate.now()
                + "\",\"tenure\":\"12\",\"emiAmount\":\"5000\",\"startDate\":\"" + LocalDate.now() + "\"}";
        long emiId = body(mvc.perform(post("/api/emis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(emi)).andExpect(status().isOk()).andReturn()).get("id").asLong();
        mvc.perform(post("/api/emis/" + emiId + "/payments").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"-1\",\"date\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void borrowerAndEmiWithPaymentsCanBeDeleted() throws Exception {
        String token = register("del1");
        long borrowerId = createBorrowerWithLoan(token);
        long loanId = firstLoanId(token);
        mvc.perform(post("/api/loans/" + loanId + "/payments").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"100\"}")).andExpect(status().isOk());
        mvc.perform(delete("/api/borrowers/" + borrowerId).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/borrowers").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.length()").value(0));

        String emi = "{\"borrowerName\":\"V\",\"totalAmount\":\"60000\",\"givenInCash\":\"57000\",\"givenDate\":\"" + LocalDate.now()
                + "\",\"tenure\":\"12\",\"emiAmount\":\"5000\",\"startDate\":\"" + LocalDate.now() + "\"}";
        long emiId = body(mvc.perform(post("/api/emis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(emi)).andReturn()).get("id").asLong();
        mvc.perform(post("/api/emis/" + emiId + "/payments").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":\"5000\",\"date\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/emis/" + emiId).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void otherUsersCannotReachALoan() throws Exception {
        String alice = register("iso-alice"), bob = register("iso-bob");
        createBorrowerWithLoan(alice);
        long loanId = firstLoanId(alice);
        mvc.perform(get("/api/loans/" + loanId).header("Authorization", "Bearer " + bob)).andExpect(status().isForbidden());
    }
}
