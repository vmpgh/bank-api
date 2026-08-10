package com.bank.bankapi.entity;

import com.bank.bankapi.exception.InsufficientFundsException;
import com.bank.bankapi.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "accounts")
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean deleted;

    @Column
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Account(User owner, BigDecimal balance) {
        this.owner = owner;
        this.balance = balance;
    }

    public void deposit(BigDecimal amount){
        if(amount.signum() <= 0 ){
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        balance = balance.add(amount);
    }


    public void withdraw(BigDecimal amount){
        if(amount.signum() <= 0 ){
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if(balance.compareTo(amount) < 0){
            throw new InsufficientFundsException(id);
        }

        balance = balance.subtract(amount);

    }


}