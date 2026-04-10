package com.union.solutions.saascore.adapters.out.persistence;

import com.union.solutions.saascore.domain.Policy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PolicyEffectConverter implements AttributeConverter<Policy.Effect, String> {

  @Override
  public String convertToDatabaseColumn(Policy.Effect attribute) {
    return attribute == null ? null : attribute.name();
  }

  @Override
  public Policy.Effect convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    return Policy.Effect.valueOf(dbData.toUpperCase());
  }
}
