package com.union.solutions.saascore.adapters.in.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  private final DataSource dataSource;
  private final RedisConnectionFactory redisConnectionFactory;
  private final org.springframework.amqp.rabbit.connection.ConnectionFactory
      rabbitConnectionFactory;

  public HealthController(
      DataSource dataSource,
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory,
      @Autowired(required = false)
          org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory) {
    this.dataSource = dataSource;
    this.redisConnectionFactory = redisConnectionFactory;
    this.rabbitConnectionFactory = rabbitConnectionFactory;
  }

  @GetMapping("/healthz")
  public ResponseEntity<String> healthz() {
    return ResponseEntity.ok("OK");
  }

  @GetMapping("/readyz")
  public ResponseEntity<Map<String, Object>> readyz() {
    Map<String, Object> checks = new LinkedHashMap<>();
    boolean allUp = true;

    checks.put("db", checkDatabase());
    if (!"UP".equals(checks.get("db"))) allUp = false;

    checks.put("redis", checkRedis());
    if (!"UP".equals(checks.get("redis")) && !"SKIPPED".equals(checks.get("redis"))) allUp = false;

    checks.put("rabbitmq", checkRabbit());
    if (!"UP".equals(checks.get("rabbitmq")) && !"SKIPPED".equals(checks.get("rabbitmq")))
      allUp = false;

    checks.put("status", allUp ? "UP" : "DOWN");

    return allUp ? ResponseEntity.ok(checks) : ResponseEntity.status(503).body(checks);
  }

  private String checkDatabase() {
    try (var conn = dataSource.getConnection()) {
      try (var stmt = conn.createStatement()) {
        stmt.execute("SELECT 1");
      }
      return "UP";
    } catch (Exception e) {
      return "DOWN";
    }
  }

  private String checkRedis() {
    if (redisConnectionFactory == null) return "SKIPPED";
    try (var conn = redisConnectionFactory.getConnection()) {
      conn.ping();
      return "UP";
    } catch (Exception e) {
      return "DOWN";
    }
  }

  private String checkRabbit() {
    if (rabbitConnectionFactory == null) return "SKIPPED";
    try {
      var conn = rabbitConnectionFactory.createConnection();
      conn.close();
      return "UP";
    } catch (Exception e) {
      return "DOWN";
    }
  }
}
