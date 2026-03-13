package com.union.solutions.saascore.adapters.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "actor_sub")
  private String actorSub;

  @Column(name = "actor_roles", columnDefinition = "text")
  private String actorRoles;

  @Column(name = "actor_perms", columnDefinition = "text")
  private String actorPerms;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @Column(name = "resource_type", length = 64)
  private String resourceType;

  @Column(name = "resource_id")
  private String resourceId;

  @Column(name = "method", length = 10)
  private String method;

  @Column(name = "path", length = 512)
  private String path;

  @Column(name = "status_code")
  private Integer statusCode;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "details", columnDefinition = "text")
  private String details;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public String getActorSub() {
    return actorSub;
  }

  public void setActorSub(String actorSub) {
    this.actorSub = actorSub;
  }

  public String getActorRoles() {
    return actorRoles;
  }

  public void setActorRoles(String actorRoles) {
    this.actorRoles = actorRoles;
  }

  public String getActorPerms() {
    return actorPerms;
  }

  public void setActorPerms(String actorPerms) {
    this.actorPerms = actorPerms;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuditLogEntity that)) return false;
    return id != null && java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
