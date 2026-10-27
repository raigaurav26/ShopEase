package com.shopease.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real sign-in flow end to end against an in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void registerLoginRefreshAndLogout() throws Exception {
        register("flow@test.com").andExpect(status().isCreated());

        JsonNode login = json(login("flow@test.com", "password123").andExpect(status().isOk()));
        String refresh1 = login.get("refreshToken").asText();

        JsonNode refreshed = json(refresh(refresh1).andExpect(status().isOk()));
        String refresh2 = refreshed.get("refreshToken").asText();

        logout(refresh2).andExpect(status().isNoContent());
        refresh(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    void reusingARotatedRefreshTokenEndsEverySession() throws Exception {
        register("reuse@test.com").andExpect(status().isCreated());
        String laptop = json(login("reuse@test.com", "password123")).get("refreshToken").asText();
        String phone = json(login("reuse@test.com", "password123")).get("refreshToken").asText();

        String rotated = json(refresh(laptop).andExpect(status().isOk())).get("refreshToken").asText();

        // An attacker replays the old laptop token
        refresh(laptop).andExpect(status().isUnauthorized());

        refresh(rotated).andExpect(status().isUnauthorized());
        refresh(phone).andExpect(status().isUnauthorized());
    }

    @Test
    void accountLocksAfterFiveWrongPasswords() throws Exception {
        register("lock@test.com").andExpect(status().isCreated());

        for (int i = 0; i < 5; i++) {
            login("lock@test.com", "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid email or password"));
        }

        login("lock@test.com", "password123").andExpect(status().isLocked());
    }

    @Test
    void unknownEmailGetsTheSameErrorAsWrongPassword() throws Exception {
        login("nobody@test.com", "password123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void duplicateEmailIsAConflict() throws Exception {
        register("dupe@test.com").andExpect(status().isCreated());
        register("DUPE@test.com").andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    private ResultActions register(String email) throws Exception {
        return mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"password123\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private ResultActions logout(String token) throws Exception {
        return mvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
