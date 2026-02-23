package com.union.solutions.saascore.adapters.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetails(
    String type, String title, int status, String detail, String instance, String correlation_id) {

  public static ProblemDetails of(
      int status, String title, String detail, String instance, String correlationId) {
    return new ProblemDetails("about:blank", title, status, detail, instance, correlationId);
  }
}
