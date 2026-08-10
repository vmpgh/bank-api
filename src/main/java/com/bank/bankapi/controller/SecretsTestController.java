package com.bank.bankapi.controller;

import com.bank.bankapi.service.AwsSecretsManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bank.bankapi.service.AwsSecretsManagerService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SecretsTestController {



    private final AwsSecretsManagerService secretsManagerService;

    @Value("${aws.secrets-manager.rds-secret-arn}")
    private String secretArn;

    @GetMapping("/internal/test-secrets-manager")
    public Map<String, Object> testSecretsManager() {

        String secret = secretsManagerService.getSecret(secretArn);

        return Map.of(
                "success", true,
                "secretRetrieved", secret != null && !secret.isBlank(),
                "secretLength", secret == null ? 0 : secret.length()
        );
    }
}