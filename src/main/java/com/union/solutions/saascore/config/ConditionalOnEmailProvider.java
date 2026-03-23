package com.union.solutions.saascore.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/**
 * Condição insensível a maiúsculas para {@code app.email.provider} (ex.: {@code SMTP} no Railway =
 * {@code smtp}).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnEmailProviderCondition.class)
public @interface ConditionalOnEmailProvider {

  /** Um de: {@code log}, {@code resend}, {@code smtp} (comparação case-insensitive). */
  String value();
}
