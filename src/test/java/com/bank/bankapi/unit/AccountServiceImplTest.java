package com.bank.bankapi.unit;

import com.bank.bankapi.dto.*;
import com.bank.bankapi.entity.Account;
import com.bank.bankapi.entity.User;
import com.bank.bankapi.exception.InsufficientFundsException;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.mapper.AccountMapper;
import com.bank.bankapi.metrics.BankMetrics;
import com.bank.bankapi.repository.AccountRepository;
import com.bank.bankapi.repository.UserRepository;
import com.bank.bankapi.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private BankMetrics bankMetrics;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user(String username) {

        User user = new User();

        user.setId(UUID.randomUUID());
        user.setUsername(username);

        return user;
    }

    private Account account(User owner, BigDecimal balance) {

        Account account = new Account(owner, balance);

        account.setId(UUID.randomUUID());

        return account;
    }

    private AccountResponse response(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getOwner().getId(),
                account.getOwner().getUsername(),
                account.getBalance()
        );
    }

    // ===========================================================
    // CREATE
    // ===========================================================

    @Nested
    class CreateAccount {

        @Test
        void createsAccountSuccessfully() {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

            Account account = account(john, BigDecimal.valueOf(100));

            AccountResponse response = response(account);

            when(userRepository.findById(john.getId()))
                    .thenReturn(Optional.of(john));

            when(accountRepository.save(any(Account.class)))
                    .thenReturn(account);

            when(accountMapper.toResponse(account))
                    .thenReturn(response);

            AccountResponse result = accountService.create(request);

            assertThat(result).isEqualTo(response);

            verify(accountRepository).save(any(Account.class));

            verify(bankMetrics).incrementAccountsCreated();
        }

        @Test
        void throwsWhenUserDoesNotExist() {

            UUID id = UUID.randomUUID();

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(id);
            request.setBalance(BigDecimal.TEN);

            when(userRepository.findById(id))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(accountRepository, never()).save(any());
        }
    }

    // ===========================================================
    // GET BY ID
    // ===========================================================

    @Nested
    class GetById {

        @Test
        void returnsExistingAccount() {

            User john = user("john");

            Account account = account(john, BigDecimal.valueOf(500));

            AccountResponse response = response(account);

            when(accountRepository.findByIdAndDeletedFalse(account.getId()))
                    .thenReturn(Optional.of(account));

            when(accountMapper.toResponse(account))
                    .thenReturn(response);

            AccountResponse result =
                    accountService.getById(account.getId());

            assertThat(result).isEqualTo(response);

            verify(accountRepository)
                    .findByIdAndDeletedFalse(account.getId());
        }

        @Test
        void throwsWhenAccountDoesNotExist() {

            UUID id = UUID.randomUUID();

            when(accountRepository.findByIdAndDeletedFalse(id))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account");

            verify(accountMapper, never()).toResponse(any());
        }
    }

    @Nested
    class FindAll {

        @Test
        void returnsAllAccounts() {

            User john = user("john");
            User alice = user("alice");

            Account johnAccount =
                    account(john, BigDecimal.valueOf(500));

            Account aliceAccount =
                    account(alice, BigDecimal.valueOf(1000));

            AccountResponse johnResponse =
                    response(johnAccount);

            AccountResponse aliceResponse =
                    response(aliceAccount);

            when(accountRepository.findAll())
                    .thenReturn(List.of(johnAccount, aliceAccount));

            when(accountMapper.toResponse(johnAccount))
                    .thenReturn(johnResponse);

            when(accountMapper.toResponse(aliceAccount))
                    .thenReturn(aliceResponse);

            List<AccountResponse> result =
                    accountService.findAll();

            assertThat(result)
                    .hasSize(2)
                    .containsExactly(johnResponse, aliceResponse);

            verify(accountRepository).findAll();

            verify(accountMapper).toResponse(johnAccount);
            verify(accountMapper).toResponse(aliceAccount);
        }

        @Test
        void returnsEmptyListWhenNoAccountsExist() {

            when(accountRepository.findAll())
                    .thenReturn(List.of());

            List<AccountResponse> result =
                    accountService.findAll();

            assertThat(result).isEmpty();

            verify(accountRepository).findAll();

            verify(accountMapper, never()).toResponse(any());
        }
    }

    // ===========================================================
    // DEPOSIT
    // ===========================================================

    @Nested
    class Deposit {

        @Test
        void depositsMoneySuccessfully() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(500));

            DepositRequest request =
                    new DepositRequest();

            request.setDepositAmount(BigDecimal.valueOf(200));

            AccountResponse response =
                    new AccountResponse(
                            account.getId(),
                            john.getId(),
                            john.getUsername(),
                            BigDecimal.valueOf(700)
                    );

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            when(accountMapper.toResponse(account))
                    .thenReturn(response);

            AccountResponse result =
                    accountService.deposit(
                            account.getId(),
                            request
                    );

            assertThat(result.getBalance())
                    .isEqualByComparingTo("700");

            verify(bankMetrics)
                    .incrementDepositsMade();

            verify(accountMapper)
                    .toResponse(account);
        }

        @Test
        void throwsWhenAccountDoesNotExist() {

            UUID id = UUID.randomUUID();

            DepositRequest request =
                    new DepositRequest();

            request.setDepositAmount(BigDecimal.TEN);

            when(accountRepository.findById(id))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    accountService.deposit(id, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account");

            verify(bankMetrics, never())
                    .incrementDepositsMade();

            verify(accountMapper, never())
                    .toResponse(any());
        }

        @Test
        void depositActuallyChangesAccountBalance() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(100));

            DepositRequest request =
                    new DepositRequest();

            request.setDepositAmount(BigDecimal.valueOf(50));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            when(accountMapper.toResponse(any()))
                    .thenAnswer(invocation ->
                            response(invocation.getArgument(0)));

            accountService.deposit(account.getId(), request);

            assertThat(account.getBalance())
                    .isEqualByComparingTo("150");

            verify(bankMetrics)
                    .incrementDepositsMade();
        }

        @Test
        void mapperReceivesUpdatedAccount() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(100));

            DepositRequest request =
                    new DepositRequest();

            request.setDepositAmount(BigDecimal.valueOf(20));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            when(accountMapper.toResponse(any()))
                    .thenAnswer(invocation ->
                            response(invocation.getArgument(0)));

            accountService.deposit(
                    account.getId(),
                    request
            );

            verify(accountMapper)
                    .toResponse(account);

            assertThat(account.getBalance())
                    .isEqualByComparingTo("120");
        }
    }

    @Nested
    class Withdraw {

        @Test
        void withdrawsMoneySuccessfully() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(500));

            WithdrawRequest request =
                    new WithdrawRequest();

            request.setWithdrawAmount(BigDecimal.valueOf(200));

            AccountResponse response =
                    new AccountResponse(
                            account.getId(),
                            john.getId(),
                            john.getUsername(),
                            BigDecimal.valueOf(300)
                    );

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            when(accountMapper.toResponse(account))
                    .thenReturn(response);

            AccountResponse result =
                    accountService.withdraw(
                            account.getId(),
                            request
                    );

            assertThat(result.getBalance())
                    .isEqualByComparingTo("300");

            verify(bankMetrics)
                    .incrementWithdrawalsMade();

            verify(accountMapper)
                    .toResponse(account);
        }

        @Test
        void throwsWhenAccountDoesNotExist() {

            UUID id = UUID.randomUUID();

            WithdrawRequest request =
                    new WithdrawRequest();

            request.setWithdrawAmount(BigDecimal.TEN);

            when(accountRepository.findById(id))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    accountService.withdraw(id, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account");

            verify(bankMetrics, never())
                    .incrementWithdrawalsMade();
        }

        @Test
        void throwsWhenInsufficientFunds() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(100));

            WithdrawRequest request =
                    new WithdrawRequest();

            request.setWithdrawAmount(BigDecimal.valueOf(200));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            assertThatThrownBy(() ->
                    accountService.withdraw(
                            account.getId(),
                            request))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("insufficient");

            verify(bankMetrics, never())
                    .incrementWithdrawalsMade();

            verify(accountMapper, never())
                    .toResponse(any());
        }

        @Test
        void withdrawActuallyChangesBalance() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(300));

            WithdrawRequest request =
                    new WithdrawRequest();

            request.setWithdrawAmount(BigDecimal.valueOf(100));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            when(accountMapper.toResponse(any()))
                    .thenAnswer(invocation ->
                            response(invocation.getArgument(0)));

            accountService.withdraw(
                    account.getId(),
                    request);

            assertThat(account.getBalance())
                    .isEqualByComparingTo("200");

            verify(bankMetrics)
                    .incrementWithdrawalsMade();
        }
    }

    @Nested
    class DeleteAccount {

        @Test
        void softDeletesAccount() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(500));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            accountService.delete(account.getId());

            assertThat(account.isDeleted())
                    .isTrue();

            assertThat(account.getDeletedAt())
                    .isNotNull();

            verify(accountRepository)
                    .save(account);
        }

        @Test
        void throwsWhenAccountDoesNotExist() {

            UUID id = UUID.randomUUID();

            when(accountRepository.findById(id))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    accountService.delete(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account");

            verify(accountRepository, never())
                    .save(any());
        }

        @Test
        void deleteSetsDeletedTimestamp() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(100));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            accountService.delete(account.getId());

            Instant deletedAt = account.getDeletedAt();

            assertThat(deletedAt).isNotNull();

            assertThat(deletedAt)
                    .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        void deleteMarksDeletedFlag() {

            User john = user("john");

            Account account =
                    account(john, BigDecimal.valueOf(100));

            when(accountRepository.findById(account.getId()))
                    .thenReturn(Optional.of(account));

            accountService.delete(account.getId());

            assertThat(account.isDeleted())
                    .isTrue();
        }
    }

}


