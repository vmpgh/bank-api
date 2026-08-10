package com.bank.bankapi.config;

import com.bank.bankapi.entity.Role;
import com.bank.bankapi.entity.User;
import com.bank.bankapi.metrics.BankMetrics;
import com.bank.bankapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankMetrics bankMetrics;


    @Profile({"local", "docker", "test", "aws"})
    @Bean
    CommandLineRunner initUsers(UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {

        return args -> {

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    "admin",
                    "password",
                    "Admin",
                    "Test",
                    "admin@test.org",
                    Role.ADMIN
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    "john",
                    "password",
                    "John",
                    "Test",
                    "john@test.org",
                    Role.USER
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    "alice",
                    "password",
                    "Alice",
                    "Test",
                    "alice@test.org",
                    Role.USER
            );
        };
    }

    private void createUserIfMissing(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     String username,
                                     String password,
                                     String firstName,
                                     String lastName,
                                     String email,
                                     Role role) {

        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(Instant.now());
        user.setEnabled(true);


        userRepository.save(user);

        log.info("Created {} user '{}'", role, username);
    }
}