package com.yourorg.saascore.config;

import com.yourorg.saascore.adapters.in.auth.JwtAuthenticationFilter;
import com.yourorg.saascore.adapters.in.auth.TenantResolutionFilter;
import com.yourorg.saascore.observability.CorrelationIdFilter;
import com.yourorg.saascore.observability.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CorrelationIdFilter correlationIdFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantResolutionFilter tenantResolutionFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            CorrelationIdFilter correlationIdFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            TenantResolutionFilter tenantResolutionFilter,
            RateLimitFilter rateLimitFilter) {
        this.correlationIdFilter = correlationIdFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tenantResolutionFilter = tenantResolutionFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/v1/auth/token",
                                                "/healthz",
                                                "/readyz",
                                                "/docs",
                                                "/docs/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**",
                                                "/openapi.json",
                                                "/metrics",
                                                "/actuator",
                                                "/actuator/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantResolutionFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, TenantResolutionFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
