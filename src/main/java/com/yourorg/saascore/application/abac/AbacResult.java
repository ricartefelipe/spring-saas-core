package com.yourorg.saascore.application.abac;

import java.util.UUID;

public record AbacResult(boolean allowed, UUID policyId, String reason) {

    public static AbacResult allow() {
        return new AbacResult(true, null, "no_deny_policy");
    }

    public static AbacResult deny(UUID policyId, String reason) {
        return new AbacResult(false, policyId, reason);
    }
}
