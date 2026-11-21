package com.example.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.service.SmsService;

/** OTP by SMS: delivery, attempt limit, resend limits and the server-side check at registration. */
@SpringBootTest
@AutoConfigureMockMvc
class OtpFlowTest {

    @Autowired MockMvc mvc;
    @MockitoSpyBean SmsService sms;

    @BeforeEach
    void smsOn() {
        doReturn(true).when(sms).isEnabled();
        doReturn(true).when(sms).sendSms(anyString(), anyString());
    }

    private String sendOtp(String email, String phone) throws Exception {
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.channel").value("sms"));
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(sms, atLeastOnce()).sendSms(anyString(), msg.capture());
        Matcher m = Pattern.compile("(\\d{6})").matcher(msg.getValue());
        assertTrue(m.find());
        return m.group(1);
    }

    private boolean verifyOtp(String email, String otp) throws Exception {
        String r = mvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}")).andReturn().getResponse().getContentAsString();
        return r.contains("\"valid\":true");
    }

    private int register(String user, String email, String phone) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"Passw0rd@1\",\"email\":\"" + email + "\",\"phone\":\"" + phone + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void codeGoesToTheNormalizedPhoneAndRegistrationNeedsIt() throws Exception {
        String code = sendOtp("sms1@example.com", "98765 43210");
        verify(sms).sendSms(eq("+919876543210"), anyString());
        assertEquals(400, register("sms1", "sms1@example.com", "9876543210"), "phone not verified yet");
        assertTrue(verifyOtp("sms1@example.com", code));
        assertEquals(400, register("sms1", "sms1@example.com", "9123456789"), "a different phone is not verified");
        assertEquals(200, register("sms1", "sms1@example.com", "9876543210"));
    }

    @Test
    void registrationWithoutAPhoneDoesNotNeedAnOtp() throws Exception {
        assertEquals(200, mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nophone\",\"password\":\"Passw0rd@1\",\"email\":\"nophone@example.com\"}"))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void fiveWrongCodesLockTheCode() throws Exception {
        String code = sendOtp("sms2@example.com", "9876500002");
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) assertFalse(verifyOtp("sms2@example.com", wrong));
        assertFalse(verifyOtp("sms2@example.com", code), "locked after 5 wrong attempts");
    }

    @Test
    void aCodeWorksOnlyOnce() throws Exception {
        String code = sendOtp("sms3@example.com", "9876500003");
        assertTrue(verifyOtp("sms3@example.com", code));
        assertFalse(verifyOtp("sms3@example.com", code));
    }

    @Test
    void resendIsLimitedPerEmailAndPerPhone() throws Exception {
        sendOtp("sms4@example.com", "9876500004");
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"sms4@example.com\",\"phone\":\"9876500099\"}")).andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"other4@example.com\",\"phone\":\"9876500004\"}")).andExpect(status().isTooManyRequests());
    }

    @Test
    void invalidNumbersAndEmailsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"sms5@example.com\",\"phone\":\"12345\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"phone\":\"9876543210\"}")).andExpect(status().isBadRequest());
    }
}
