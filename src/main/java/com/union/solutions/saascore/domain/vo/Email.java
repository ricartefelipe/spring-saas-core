package com.union.solutions.saascore.domain.vo;

import java.util.Objects;

public record Email(String value) {

  public Email {
    Objects.requireNonNull(value, "Email must not be null");
    if (!value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
      throw new IllegalArgumentException("Invalid email format: " + value);
    }
    value = value.toLowerCase().trim();
  }

  @Override
  public String toString() {
    return value;
  }
}
