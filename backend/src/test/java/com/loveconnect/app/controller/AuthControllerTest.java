package com.loveconnect.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveconnect.app.dto.AuthResponse;
import com.loveconnect.app.dto.LoginRequest;
import com.loveconnect.app.dto.UserResponse;
import com.loveconnect.app.entity.Gender;
import com.loveconnect.app.entity.Role;
import com.loveconnect.app.service.AuthService;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;

    @Test
    void loginReturnsToken() throws Exception {
        UserResponse user = new UserResponse(1L, "Ava", "ava@example.com", "9999999999", Gender.FEMALE,
                28, "Pune", null, Role.USER, false, false);
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-token", user));
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "ava@example.com");
        payload.put("password", "password123");
        payload.put("rememberMe", true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}


