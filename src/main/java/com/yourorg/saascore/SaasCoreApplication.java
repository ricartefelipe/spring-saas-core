package com.yourorg.saascore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        exclude = {
            DataSourceAutoConfiguration.class,
            LiquibaseAutoConfiguration.class,
        })
@EnableScheduling
@EnableAsync
public class SaasCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasCoreApplication.class, args);
    }
}
