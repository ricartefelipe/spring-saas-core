package com.union.solutions.saascore.unit.adapters.in.auth;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.auth.DevTokenController;
import com.union.solutions.saascore.application.port.TokenIssuer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DevTokenControllerTest {

  @Mock TokenIssuer tokenIssuer;
  @Mock Environment environment;

  private static final String VALID_REQUEST =
      "{\"sub\":\"dev@test\",\"tid\":\"00000000-0000-0000-0000-000000000001\","
          + "\"roles\":[\"admin\"],\"perms\":[\"tenants:read\"],"
          + "\"plan\":\"pro\",\"region\":\"us-east-1\"}";

  private DevTokenController buildController(boolean productionProfile) {
    DevTokenController controller = new DevTokenController(tokenIssuer, environment);
    ReflectionTestUtils.setField(controller, "productionProfile", productionProfile);
    return controller;
  }

  @Test
  void issueDevToken_inDevProfile_returnsToken() throws Exception {
    when(tokenIssuer.issue(
            anyString(), anyString(), anyList(), anyList(), anyString(), anyString()))
        .thenReturn("mock-jwt-token");

    MockMvc mvc = MockMvcBuilders.standaloneSetup(buildController(false)).build();

    mvc.perform(
            post("/v1/dev/token").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("mock-jwt-token"))
        .andExpect(jsonPath("$.token_type").value("Bearer"))
        .andExpect(jsonPath("$.expires_in").value(3600));
  }

  @Test
  void issueDevToken_inProdProfile_returns403() throws Exception {
    MockMvc mvc = MockMvcBuilders.standaloneSetup(buildController(true)).build();

    mvc.perform(
            post("/v1/dev/token").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("dev_token_disabled"));
  }

  @Test
  void issueDevToken_missingRequiredFields_returns400() throws Exception {
    MockMvc mvc = MockMvcBuilders.standaloneSetup(buildController(false)).build();

    mvc.perform(
            post("/v1/dev/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"admin\"]}"))
        .andExpect(status().isBadRequest());
  }
}
