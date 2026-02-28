package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.adapters.in.rest.HealthController;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

  @Mock private DataSource dataSource;

  @Test
  void healthz_returnsOk() throws Exception {
    HealthController controller = new HealthController(dataSource, null, null);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(get("/healthz"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  void readyz_whenDbUp_returns200WithUp() throws Exception {
    Connection conn = org.mockito.Mockito.mock(Connection.class);
    Statement stmt = org.mockito.Mockito.mock(Statement.class);
    org.mockito.Mockito.when(dataSource.getConnection()).thenReturn(conn);
    org.mockito.Mockito.when(conn.createStatement()).thenReturn(stmt);

    HealthController controller = new HealthController(dataSource, null, null);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(get("/readyz"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.db").value("UP"))
        .andExpect(jsonPath("$.redis").value("SKIPPED"))
        .andExpect(jsonPath("$.rabbitmq").value("SKIPPED"))
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void readyz_whenDbDown_returns503WithDown() throws Exception {
    org.mockito.Mockito.when(dataSource.getConnection()).thenThrow(new RuntimeException("DB unreachable"));

    HealthController controller = new HealthController(dataSource, null, null);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(get("/readyz"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.db").value("DOWN"))
        .andExpect(jsonPath("$.status").value("DOWN"));
  }
}
