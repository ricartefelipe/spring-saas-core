package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogEntity;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/audit")
public class AuditLogController {

  private final AuditLogJpaRepository auditRepo;

  public AuditLogController(AuditLogJpaRepository auditRepo) {
    this.auditRepo = auditRepo;
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
      @RequestParam(required = false, defaultValue = "50") int limit,
      @PageableDefault(size = 50) Pageable pageable) {

    if (cursor != null && !cursor.isBlank()) {
      Instant cursorInstant = decodeCursor(cursor);
      List<AuditDto> items =
          auditRepo
              .findNextPage(
                  tenantId,
                  action,
                  actorSub,
                  correlationId,
                  from,
                  to,
                  cursorInstant,
                  PageRequest.of(0, limit))
              .stream()
              .map(AuditDto::from)
              .toList();
      boolean hasMore = items.size() == limit;
      String nextCursor = hasMore ? encodeCursor(items.getLast().createdAt()) : null;
      return ResponseEntity.ok(new CursorPage<>(items, nextCursor, hasMore));
    }

    Page<AuditDto> page =
        auditRepo
            .search(tenantId, action, actorSub, correlationId, from, to, pageable)
            .map(AuditDto::from);
    return ResponseEntity.ok(page);
  }

  private static String encodeCursor(Instant instant) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(instant.toString().getBytes());
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
