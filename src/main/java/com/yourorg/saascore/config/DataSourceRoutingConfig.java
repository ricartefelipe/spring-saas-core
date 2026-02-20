package com.yourorg.saascore.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Configuration
public class DataSourceRoutingConfig {

    public static final String SHARD_A = "shard-a";
    public static final String SHARD_B = "shard-b";

    @Bean("shardADataSource")
    public DataSource shardADataSource(
            @Value("${app.datasource.shard-a.jdbc-url}") String url,
            @Value("${app.datasource.shard-a.username}") String user,
            @Value("${app.datasource.shard-a.password}") String pass) {
        HikariDataSource d = new HikariDataSource();
        d.setJdbcUrl(url);
        d.setUsername(user);
        d.setPassword(pass);
        d.setMaximumPoolSize(10);
        d.setMinimumIdle(2);
        return d;
    }

    @Bean("shardBDataSource")
    public DataSource shardBDataSource(
            @Value("${app.datasource.shard-b.jdbc-url}") String url,
            @Value("${app.datasource.shard-b.username}") String user,
            @Value("${app.datasource.shard-b.password}") String pass) {
        HikariDataSource d = new HikariDataSource();
        d.setJdbcUrl(url);
        d.setUsername(user);
        d.setPassword(pass);
        d.setMaximumPoolSize(10);
        d.setMinimumIdle(2);
        return d;
    }

    @Bean("routingDataSource")
    @Primary
    public DataSource routingDataSource(
            @Qualifier("shardADataSource") DataSource shardA,
            @Qualifier("shardBDataSource") DataSource shardB) {
        AbstractRoutingDataSource ds = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                String key = TenantContext.getShardKey();
                if (key == null) {
                    return SHARD_A;
                }
                return key.startsWith("shard-b") ? SHARD_B : SHARD_A;
            }
        };
        Map<Object, Object> targets = new HashMap<>();
        targets.put(SHARD_A, shardA);
        targets.put(SHARD_B, shardB);
        ds.setTargetDataSources(targets);
        ds.setDefaultTargetDataSource(shardA);
        return ds;
    }
}
