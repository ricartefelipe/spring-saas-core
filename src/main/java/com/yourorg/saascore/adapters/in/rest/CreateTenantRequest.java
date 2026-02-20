package com.yourorg.saascore.adapters.in.rest;

import com.yourorg.saascore.domain.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTenantRequest {

    @NotBlank
    private String name;

    @NotNull
    private Tenant.Plan plan = Tenant.Plan.free;

    @NotBlank
    private String primaryRegion = "region-a";

    @NotBlank
    private String shardKey = "shard-a";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tenant.Plan getPlan() {
        return plan;
    }

    public void setPlan(Tenant.Plan plan) {
        this.plan = plan;
    }

    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public void setPrimaryRegion(String primaryRegion) {
        this.primaryRegion = primaryRegion;
    }

    public String getShardKey() {
        return shardKey;
    }

    public void setShardKey(String shardKey) {
        this.shardKey = shardKey;
    }
}
