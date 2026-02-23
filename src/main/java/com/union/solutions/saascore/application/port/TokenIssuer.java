package com.union.solutions.saascore.application.port;

import java.util.List;

public interface TokenIssuer {

  String issue(
      String sub, String tid, List<String> roles, List<String> perms, String plan, String region);
}
