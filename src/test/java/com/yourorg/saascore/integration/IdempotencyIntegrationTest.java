package com.yourorg.saascore.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yourorg.saascore.config.DataSourceRoutingConfig;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.adapters.out.persistence.TenantEntity;
import com.yourorg.saascore.adapters.out.persistence.TenantJpaRepository;
import com.yourorg.saascore.adapters.out.persistence.UserEntity;
import com.yourorg.saascore.adapters.out.persistence.UserJpaRepository;
import com.yourorg.saascore.domain.Tenant;
import com.yourorg.saascore.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdempotencyIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final String IDEM_KEY = "test-idem-key-1";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserJpaRepository userRepo;
    @Autowired
    TenantJpaRepository tenantRepo;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        TenantContext.setShardKey(DataSourceRoutingConfig.SHARD_A);
        TenantEntity te =
                TenantEntity.from(
                        new Tenant(
                                TENANT_ID,
                                "Test Tenant",
                                Tenant.TenantStatus.ACTIVE,
                                Tenant.Plan.pro,
                                "region-a",
                                "shard-a",
                                Instant.now()));
        tenantRepo.save(te);
        UserEntity ue =
                UserEntity.from(
                        new User(
                                UUID.randomUUID(),
                                TENANT_ID,
                                "idem-test@example.com",
                                passwordEncoder.encode("password"),
                                User.UserStatus.ACTIVE,
                                Instant.now()));
        userRepo.save(ue);
        TenantContext.clear();

        String body =
                mvc.perform(
                                post("/v1/auth/token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "username",
                                                                "idem-test@example.com",
                                                                "password",
                                                                "password"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        token = objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void createUser_twiceWithSameIdempotencyKey_returnsSameStatusAndBody() throws Exception {
        String payload =
                objectMapper.writeValueAsString(
                        Map.of("email", "newuser@example.com", "password", "secret"));
        ResultActions first =
                mvc.perform(
                        post("/v1/users")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-Id", TENANT_ID.toString())
                                .header("Idempotency-Key", IDEM_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload));
        first.andExpect(status().is2xxSuccessful());
        String firstBody = first.andReturn().getResponse().getContentAsString();
        int firstStatus = first.andReturn().getResponse().getStatus();

        ResultActions second =
                mvc.perform(
                        post("/v1/users")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-Id", TENANT_ID.toString())
                                .header("Idempotency-Key", IDEM_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload));
        second.andExpect(status().is(firstStatus));
        String secondBody = second.andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(secondBody).isEqualTo(firstBody);
    }
}
