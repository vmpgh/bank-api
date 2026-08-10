package com.bank.bankapi.config;

import com.bank.bankapi.service.AwsSecretsManagerService;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.ObjectMapper;
import com.bank.bankapi.service.AwsSecretsManagerService;

import javax.sql.DataSource;

@Configuration
@Profile("aws")
public class AwsDataSourceConfig {

    private final AwsSecretsManagerService secretsManagerService;
    private final ObjectMapper objectMapper;

    @Value("${aws.secrets-manager.rds-secret-arn}")
    private String rdsSecretArn;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    public AwsDataSourceConfig(
            AwsSecretsManagerService secretsManagerService,
            ObjectMapper objectMapper) {

        this.secretsManagerService = secretsManagerService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public DataSource dataSource() throws Exception {

        String secretJson =
                secretsManagerService.getSecret(rdsSecretArn);

        RdsSecret secret =
                objectMapper.readValue(secretJson, RdsSecret.class);

        HikariDataSource dataSource =
                new HikariDataSource();

        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(secret.getUsername());
        dataSource.setPassword(secret.getPassword());

        return dataSource;
    }
}