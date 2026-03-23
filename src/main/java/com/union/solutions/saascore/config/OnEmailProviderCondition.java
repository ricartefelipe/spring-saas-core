package com.union.solutions.saascore.config;

import java.util.Map;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnEmailProviderCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attrs =
        metadata.getAnnotationAttributes(ConditionalOnEmailProvider.class.getName());
    if (attrs == null) {
      return false;
    }
    String expected = (String) attrs.get("value");
    if (expected == null) {
      return false;
    }
    String raw = context.getEnvironment().getProperty("app.email.provider");
    String normalized = EmailProviderConstants.normalize(raw);
    return expected.equalsIgnoreCase(normalized);
  }
}
