package com.yourorg.saascore.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthz_isOk() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk());
    }

    @Test
    void readyz_isOk() throws Exception {
        mvc.perform(get("/readyz")).andExpect(status().isOk());
    }

    @Test
    void token_withoutBody_returns4xx() throws Exception {
        mvc.perform(post("/v1/auth/token").contentType("application/json").content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
    }
}
