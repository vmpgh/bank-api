package com.bank.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Service
@RequiredArgsConstructor
public class AwsSecretsManagerService {

    private final SecretsManagerClient secretsManagerClient;

    public String getSecret(String secretArn) {

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretArn)
                .build();

        return secretsManagerClient
                .getSecretValue(request)
                .secretString();
    }
}
