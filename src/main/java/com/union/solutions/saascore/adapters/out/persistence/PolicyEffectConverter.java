package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Policy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter
public class PolicyEffectConverter implements AttributeConverter<Policy.Effect, String> {

  @Override
  public String convertToDatabaseColumn(Policy.Effect effect) {
    return effect == null ? null : effect.name();
  }

  @Override
  public Policy.Effect convertToEntityAttribute(String dbValue) {
    if (dbValue == null || dbValue.isBlank()) {
      return null;
    }
    return Policy.Effect.valueOf(dbValue.toUpperCase(Locale.ROOT));
  }
}
