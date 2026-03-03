package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogEntity;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/audit")
public class AuditLogController {

  private final AuditLogJpaRepository auditRepo;
  private final AbacEvaluator abacEvaluator;

  public AuditLogController(AuditLogJpaRepository auditRepo, AbacEvaluator abacEvaluator) {
    this.auditRepo = auditRepo;
    this.abacEvaluator = abacEvaluator;
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String actorSub,
      @RequestParam(required = false) String correlationId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = "50") int limit) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("audit:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/audit", null));

    String safeAction = (action == null || action.isBlank()) ? "" : action;
    String safeActorSub = (actorSub == null || actorSub.isBlank()) ? "" : actorSub;
    String safeCorrelationId = (correlationId == null || correlationId.isBlank()) ? "" : correlationId;
    Instant safeFrom = from != null ? from : Instant.EPOCH;
    Instant safeTo = to != null ? to : Instant.now().plusSeconds(86400 * 365 * 30L);

    if (cursor != null && !cursor.isBlank()) {
      Instant cursorInstant = decodeCursor(cursor);
      List<AuditDto> items =
          auditRepo
              .findNextPage(
                  tenantId,
                  safeAction,
                  safeActorSub,
                  safeCorrelationId,
                  safeFrom,
                  safeTo,
                  cursorInstant,
                  PageRequest.of(0, limit))
              .stream()
              .map(AuditDto::from)
              .toList();
      boolean hasMore = items.size() == limit;
      String nextCursor = buildNextCursor(items, hasMore);
      return ResponseEntity.ok(new CursorPage<>(items, nextCursor, hasMore));
    }

    int safeLimit = Math.max(1, Math.min(100, limit));
    Page<AuditLogEntity> page =
        auditRepo.search(
            tenantId,
            safeAction,
            safeActorSub,
            safeCorrelationId,
            safeFrom,
            safeTo,
            PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")));
    List<AuditDto> content = page.getContent().stream().map(AuditDto::from).toList();
    return ResponseEntity.ok(new AuditPageResponse(content, page.getTotalElements()));
  }

  /** DTO para evitar serialização direta de Spring Page (Sort etc.). */
  public record AuditPageResponse(List<AuditDto> content, long totalElements) {}

  /**
   * Exportação de audit log para compliance (retenção/exportação). Requer filtros from/to para
   * limitar o volume. Máximo 10_000 registros por requisição.
   */
  @GetMapping("/export")
  public ResponseEntity<?> export(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String action,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false, defaultValue = "json") String format,
      @RequestParam(required = false, defaultValue = "10000") int limit) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("audit:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/audit/export", null));
    int maxLimit = Math.min(10_000, Math.max(1, limit));
    String safeAction = (action == null || action.isBlank()) ? "" : action;
    Instant safeFrom = from;
    Instant safeTo = to;
    Page<AuditLogEntity> page =
        auditRepo.search(
            tenantId, safeAction, "", "", safeFrom, safeTo, PageRequest.of(0, maxLimit));
    List<AuditDto> items = page.getContent().stream().map(AuditDto::from).toList();

    if ("csv".equalsIgnoreCase(format)) {
      String csv = toCsv(items);
      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=audit-export.csv")
          .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
          .body(csv);
    }
    return ResponseEntity.ok(
        Map.of(
            "from", from.toString(),
            "to", to.toString(),
            "count", items.size(),
            "items", items));
  }

  private static String toCsv(List<AuditDto> items) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        "id,tenantId,actorSub,actorRoles,actorPerms,action,resourceType,resourceId,method,path,statusCode,correlationId,details,createdAt\n");
    for (AuditDto a : items) {
      sb.append(a.id()).append(',')
          .append(a.tenantId()).append(',')
          .append(escapeCsv(a.actorSub())).append(',')
          .append(escapeCsv(a.actorRoles())).append(',')
          .append(escapeCsv(a.actorPerms())).append(',')
          .append(escapeCsv(a.action())).append(',')
          .append(escapeCsv(a.resourceType())).append(',')
          .append(escapeCsv(a.resourceId())).append(',')
          .append(escapeCsv(a.method())).append(',')
          .append(escapeCsv(a.path())).append(',')
          .append(a.statusCode() != null ? a.statusCode() : "").append(',')
          .append(escapeCsv(a.correlationId())).append(',')
          .append(escapeCsv(a.details())).append(',')
          .append(a.createdAt() != null ? a.createdAt().toString() : "")
          .append('\n');
    }
    return sb.toString();
  }

  private static String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private static String encodeCursor(Instant instant) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(instant.toString().getBytes());
  }

  private static String buildNextCursor(List<AuditDto> items, boolean hasMore) {
    if (!hasMore || items.isEmpty()) return null;
    Instant last = items.get(items.size() - 1).createdAt();
    return last != null ? encodeCursor(last) : null;
  }

  private static Instant decodeCursor(String cursor) {
    return Instant.parse(new String(Base64.getUrlDecoder().decode(cursor)));
  }

  public record AuditDto(
      UUID id,
      UUID tenantId,
      String actorSub,
      String actorRoles,
      String actorPerms,
      String action,
      String resourceType,
      String resourceId,
      String method,
      String path,
      Integer statusCode,
      String correlationId,
      String details,
      Instant createdAt) {
    public static AuditDto from(AuditLogEntity e) {
      return new AuditDto(
          e.getId(),
          e.getTenantId(),
          e.getActorSub(),
          e.getActorRoles(),
          e.getActorPerms(),
          e.getAction(),
          e.getResourceType(),
          e.getResourceId(),
          e.getMethod(),
          e.getPath(),
          e.getStatusCode(),
          e.getCorrelationId(),
          e.getDetails(),
          e.getCreatedAt());
    }
  }
}
