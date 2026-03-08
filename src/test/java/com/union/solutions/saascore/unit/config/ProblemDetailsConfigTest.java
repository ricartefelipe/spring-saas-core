package com.union.solutions.saascore.unit.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.union.solutions.saascore.config.ProblemDetailsConfig;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailsConfigTest {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new FakeController())
            .setControllerAdvice(new ProblemDetailsConfig())
            .build();
  }

  @Test
  void dataIntegrityViolation_returns409() throws Exception {
    mvc.perform(get("/fake/data-integrity"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"));
  }

  @Test
  void constraintViolation_returns400() throws Exception {
    mvc.perform(get("/fake/constraint-violation"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"));
  }

  @Test
  void httpMessageNotReadable_returns400() throws Exception {
    mvc.perform(get("/fake/not-readable"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Malformed JSON request"));
  }

  @Test
  void entityNotFound_returns404() throws Exception {
    mvc.perform(get("/fake/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Not Found"));
  }

  @Test
  void illegalArgument_returns400() throws Exception {
    mvc.perform(get("/fake/illegal-arg"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"));
  }

  @Test
  void genericException_returns500() throws Exception {
    mvc.perform(get("/fake/generic-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500));
  }

  @RestController
  @RequestMapping("/fake")
  static class FakeController {

    @GetMapping("/data-integrity")
    public void dataIntegrity() {
      throw new DataIntegrityViolationException("duplicate key");
    }

    @GetMapping("/constraint-violation")
    public void constraintViolation() {
      @SuppressWarnings("unchecked")
      ConstraintViolation<Object> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
      Path path = org.mockito.Mockito.mock(Path.class);
      org.mockito.Mockito.when(path.toString()).thenReturn("name");
      org.mockito.Mockito.when(violation.getPropertyPath()).thenReturn(path);
      org.mockito.Mockito.when(violation.getMessage()).thenReturn("must not be blank");
      throw new ConstraintViolationException("validation failed", Set.of(violation));
    }

    @GetMapping("/not-readable")
    public void notReadable() {
      throw new HttpMessageNotReadableException(
          "JSON parse error",
          new org.springframework.http.HttpInputMessage() {
            @Override
            public java.io.InputStream getBody() {
              return java.io.InputStream.nullInputStream();
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
              return new org.springframework.http.HttpHeaders();
            }
          });
    }

    @GetMapping("/not-found")
    public void notFound() {
      throw new EntityNotFoundException("Entity not found");
    }

    @GetMapping("/illegal-arg")
    public void illegalArg() {
      throw new IllegalArgumentException("Invalid parameter");
    }

    @GetMapping("/generic-error")
    public void genericError() {
      throw new RuntimeException("Something went wrong");
    }
  }
}
