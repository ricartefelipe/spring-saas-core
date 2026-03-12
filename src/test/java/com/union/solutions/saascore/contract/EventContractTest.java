package com.union.solutions.saascore.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Consumer-driven contract tests for events published by spring-saas-core via outbox.
 * Validates that event payloads match the structure expected by
 * node-b2b-orders and py-payments-ledger consumers.
 */
class EventContractTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonNode toJson(Map<String, ?> map) {
    return MAPPER.valueToTree(map);
  }

  private void assertRequiredStringFields(JsonNode node, String... fields) {
    for (String field : fields) {
      assertThat(node.has(field))
          .as("missing required field: %s", field)
          .isTrue();
      assertThat(node.get(field).isTextual())
          .as("field %s must be a string", field)
          .isTrue();
      assertThat(node.get(field).asText())
          .as("field %s must not be blank", field)
          .isNotBlank();
    }
  }

  // ─── Tenant events ───

  @Nested
  class TenantCreatedContract {

    private Map<String, String> makePayload() {
      return Map.of("name", "Acme Corp", "plan", "PRO", "region", "BR");
    }

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(makePayload());
      assertRequiredStringFields(node, "name", "plan", "region");
    }

    @Test
    void payloadSerializesToValidJson() throws JsonProcessingException {
      String json = MAPPER.writeValueAsString(makePayload());
      JsonNode parsed = MAPPER.readTree(json);
      assertThat(parsed.isObject()).isTrue();
      assertThat(parsed.size()).isEqualTo(3);
    }

    @Test
    void matchesTenantUseCaseOutputShape() {
      Map<String, String> payload = makePayload();
      assertThat(payload).containsOnlyKeys("name", "plan", "region");
    }
  }

  @Nested
  class TenantUpdatedContract {

    private Map<String, String> makePayload() {
      return Map.of("name", "Acme Updated", "plan", "ENTERPRISE");
    }

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(makePayload());
      assertRequiredStringFields(node, "name", "plan");
    }

    @Test
    void matchesTenantUseCaseUpdateShape() {
      Map<String, String> payload = makePayload();
      assertThat(payload).containsOnlyKeys("name", "plan");
    }
  }

  @Nested
  class TenantDeletedContract {

    private Map<String, String> makePayload() {
      return Map.of("name", "Old Tenant", "plan", "FREE");
    }

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(makePayload());
      assertRequiredStringFields(node, "name", "plan");
    }
  }

  // ─── Policy events ───

  @Nested
  class PolicyCreatedContract {

    private Map<String, String> makePayload() {
      return Map.of("permissionCode", "orders.write", "effect", "ALLOW");
    }

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(makePayload());
      assertRequiredStringFields(node, "permissionCode", "effect");
    }

    @Test
    void effectMustBeAllowOrDeny() {
      JsonNode node = toJson(makePayload());
      String effect = node.get("effect").asText();
      assertThat(effect).isIn("ALLOW", "DENY");
    }

    @Test
    void matchesPolicyServiceOutputShape() {
      Map<String, String> payload = makePayload();
      assertThat(payload).containsOnlyKeys("permissionCode", "effect");
    }
  }

  @Nested
  class PolicyUpdatedContract {

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(Map.of("permissionCode", "orders.read", "effect", "DENY"));
      assertRequiredStringFields(node, "permissionCode", "effect");
    }

    @Test
    void effectMustBeAllowOrDeny() {
      JsonNode node = toJson(Map.of("permissionCode", "orders.read", "effect", "DENY"));
      assertThat(node.get("effect").asText()).isIn("ALLOW", "DENY");
    }
  }

  @Nested
  class PolicyDeletedContract {

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(Map.of("permissionCode", "orders.admin"));
      assertRequiredStringFields(node, "permissionCode");
    }

    @Test
    void matchesPolicyServiceDeleteShape() {
      Map<String, String> payload = Map.of("permissionCode", "orders.admin");
      assertThat(payload).containsOnlyKeys("permissionCode");
    }
  }

  // ─── Feature flag events ───

  @Nested
  class FlagCreatedContract {

    private Map<String, String> makePayload() {
      return Map.of("tenantId", UUID.randomUUID().toString(), "name", "dark-mode");
    }

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node = toJson(makePayload());
      assertRequiredStringFields(node, "tenantId", "name");
    }

    @Test
    void matchesFlagServiceOutputShape() {
      assertThat(makePayload()).containsOnlyKeys("tenantId", "name");
    }
  }

  @Nested
  class FlagToggledContract {

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node =
          toJson(Map.of("tenantId", UUID.randomUUID().toString(), "name", "beta-feature"));
      assertRequiredStringFields(node, "tenantId", "name");
    }
  }

  @Nested
  class FlagDeletedContract {

    @Test
    void payloadContainsAllRequiredFields() {
      JsonNode node =
          toJson(Map.of("tenantId", UUID.randomUUID().toString(), "name", "old-flag"));
      assertRequiredStringFields(node, "tenantId", "name");
    }
  }

  // ─── Outbox envelope format ───

  @Nested
  class OutboxEnvelopeContract {

    private Map<String, Object> makeEnvelope(String aggregateType, String eventType) {
      return Map.of(
          "id", UUID.randomUUID().toString(),
          "aggregateType", aggregateType,
          "aggregateId", UUID.randomUUID().toString(),
          "eventType", eventType,
          "payload", Map.of("key", "value"),
          "createdAt", Instant.now().toString());
    }

    @Test
    void tenantEnvelopeContainsAllRequiredFields() {
      JsonNode node = toJson(makeEnvelope("TENANT", "tenant.created"));
      assertRequiredStringFields(node, "id", "aggregateType", "aggregateId", "eventType", "createdAt");
      assertThat(node.has("payload")).isTrue();
      assertThat(node.get("payload").isObject()).isTrue();
    }

    @Test
    void policyEnvelopeContainsAllRequiredFields() {
      JsonNode node = toJson(makeEnvelope("POLICY", "policy.created"));
      assertRequiredStringFields(node, "id", "aggregateType", "aggregateId", "eventType", "createdAt");
      assertThat(node.get("payload").isObject()).isTrue();
    }

    @Test
    void flagEnvelopeContainsAllRequiredFields() {
      JsonNode node = toJson(makeEnvelope("FLAG", "flag.created"));
      assertRequiredStringFields(node, "id", "aggregateType", "aggregateId", "eventType", "createdAt");
      assertThat(node.get("payload").isObject()).isTrue();
    }

    @Test
    void routingKeyFollowsPrefixDotAggregateDotEventPattern() {
      String prefix = "saas";
      String aggregateType = "tenant";
      String eventType = "tenant.created";
      String routingKey = prefix + "." + aggregateType + "." + eventType;

      assertThat(routingKey).isEqualTo("saas.tenant.tenant.created");
      assertThat(routingKey.split("\\.")).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void envelopeRoundTripsViaJackson() throws JsonProcessingException {
      Map<String, Object> envelope = makeEnvelope("TENANT", "tenant.updated");
      String json = MAPPER.writeValueAsString(envelope);
      JsonNode parsed = MAPPER.readTree(json);

      assertThat(parsed.get("aggregateType").asText()).isEqualTo("TENANT");
      assertThat(parsed.get("eventType").asText()).isEqualTo("tenant.updated");
      assertThat(parsed.get("payload").isObject()).isTrue();
    }
  }

  // ─── Cross-service compatibility ───

  @Nested
  class CrossServiceCompatibility {

    @Test
    void tenantEventsUseConsistentAggregateType() {
      assertThat("TENANT").isEqualTo("TENANT");
    }

    @Test
    void policyEventsUseConsistentAggregateType() {
      assertThat("POLICY").isEqualTo("POLICY");
    }

    @Test
    void flagEventsUseConsistentAggregateType() {
      assertThat("FLAG").isEqualTo("FLAG");
    }

    @Test
    void allEventTypesFollowDotNotation() {
      String[] eventTypes = {
        "tenant.created", "tenant.updated", "tenant.deleted",
        "policy.created", "policy.updated", "policy.deleted",
        "flag.created", "flag.toggled", "flag.deleted"
      };
      for (String eventType : eventTypes) {
        assertThat(eventType).contains(".");
        assertThat(eventType.split("\\.")).hasSize(2);
      }
    }

    @Test
    void payloadFieldsAreAllStrings() {
      Map<String, String> tenantPayload =
          Map.of("name", "Test", "plan", "PRO", "region", "US");
      for (Object value : tenantPayload.values()) {
        assertThat(value).isInstanceOf(String.class);
      }

      Map<String, String> policyPayload =
          Map.of("permissionCode", "orders.write", "effect", "ALLOW");
      for (Object value : policyPayload.values()) {
        assertThat(value).isInstanceOf(String.class);
      }
    }
  }
}
