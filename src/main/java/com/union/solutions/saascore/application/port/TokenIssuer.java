package com.union.solutions.saascore.application.port;

import java.util.List;

public interface TokenIssuer {

  default String issue(
      String sub, String tid, List<String> roles, List<String> perms, String plan, String region) {
    return issue(sub, tid, roles, perms, plan, region, false);
  }

  /**
   * @param mustChangePassword when true, JWT carries claim {@code mcp}; client must force password
   *     change before full app access.
   */
  String issue(
      String sub,
      String tid,
      List<String> roles,
      List<String> perms,
      String plan,
      String region,
      boolean mustChangePassword);
}
