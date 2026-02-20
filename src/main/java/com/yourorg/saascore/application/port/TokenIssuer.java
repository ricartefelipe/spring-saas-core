package com.yourorg.saascore.application.port;

import com.yourorg.saascore.domain.Tenant;
import com.yourorg.saascore.domain.User;
import java.util.List;

public interface TokenIssuer {

    String issue(User user, Tenant tenant, List<String> roles, List<String> perms);
}
