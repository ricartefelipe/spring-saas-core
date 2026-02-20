package com.yourorg.saascore.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class LiquibaseConfig {

    @Bean("liquibaseShardA")
    public SpringLiquibase liquibaseShardA(@Qualifier("shardADataSource") DataSource dataSource) {
        SpringLiquibase lb = new SpringLiquibase();
        lb.setDataSource(dataSource);
        lb.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        lb.setContexts("shard-a");
        lb.setShouldRun(true);
        return lb;
    }

    @Bean("liquibaseShardB")
    @DependsOn("liquibaseShardA")
    public SpringLiquibase liquibaseShardB(@Qualifier("shardBDataSource") DataSource dataSource) {
        SpringLiquibase lb = new SpringLiquibase();
        lb.setDataSource(dataSource);
        lb.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        lb.setContexts("shard-b");
        lb.setShouldRun(true);
        return lb;
    }
}
