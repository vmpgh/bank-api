package com.bank.bankapi.unit;

import com.bank.bankapi.dto.TransactionResponse;
import com.bank.bankapi.entity.Account;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.mapper.TransactionMapper;
import com.bank.bankapi.repository.AccountRepository;
import com.bank.bankapi.transaction.AccountTransaction;
import com.bank.bankapi.transaction.AccountTransactionRepository;
import com.bank.bankapi.transaction.TransactionHistoryFilter;
import com.bank.bankapi.transaction.TransactionServiceImpl;
import com.bank.bankapi.transaction.TransactionType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private AccountTransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account account(String balance) {
        Account account = new Account(null, new BigDecimal(balance));
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    private AccountTransaction savedTransaction() {
        ArgumentCaptor<AccountTransaction> captor = ArgumentCaptor.forClass(AccountTransaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    class RecordDeposit {

        @Test
        void savesDepositWithBalanceAfter() {
            Account account = account("700.00");

            transactionService.recordDeposit(account, new BigDecimal("200.00"));

            AccountTransaction saved = savedTransaction();
            assertThat(saved.getAccount()).isSameAs(account);
            assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(saved.getAmount()).isEqualByComparingTo("200.00");
            assertThat(saved.getBalanceAfter()).isEqualByComparingTo("700.00");
            assertThat(saved.getCounterpartyAccountId()).isNull();
            assertThat(saved.getTransferId()).isNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    class RecordWithdrawal {

        @Test
        void savesWithdrawalWithBalanceAfter() {
            Account account = account("300.00");

            transactionService.recordWithdrawal(account, new BigDecimal("200.00"));

            AccountTransaction saved = savedTransaction();
            assertThat(saved.getType()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(saved.getAmount()).isEqualByComparingTo("200.00");
            assertThat(saved.getBalanceAfter()).isEqualByComparingTo("300.00");
        }
    }

    @Nested
    class RecordTransfer {

        @Test
        @SuppressWarnings("unchecked")
        void savesBothLegsLinkedByTransferId() {
            Account sender = account("800.00");
            Account receiver = account("700.00");
            UUID transferId = UUID.randomUUID();
            Instant at = Instant.now();

            transactionService.recordTransfer(sender, receiver, new BigDecimal("200.00"), transferId, at);

            ArgumentCaptor<List<AccountTransaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(transactionRepository).saveAll(captor.capture());

            List<AccountTransaction> legs = captor.getValue();
            assertThat(legs).hasSize(2);

            AccountTransaction out = legs.get(0);
            assertThat(out.getAccount()).isSameAs(sender);
            assertThat(out.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(out.getCounterpartyAccountId()).isEqualTo(receiver.getId());
            assertThat(out.getBalanceAfter()).isEqualByComparingTo("800.00");

            AccountTransaction in = legs.get(1);
            assertThat(in.getAccount()).isSameAs(receiver);
            assertThat(in.getType()).isEqualTo(TransactionType.TRANSFER_IN);
            assertThat(in.getCounterpartyAccountId()).isEqualTo(sender.getId());
            assertThat(in.getBalanceAfter()).isEqualByComparingTo("700.00");

            assertThat(legs).allSatisfy(leg -> {
                assertThat(leg.getTransferId()).isEqualTo(transferId);
                assertThat(leg.getAmount()).isEqualByComparingTo("200.00");
                assertThat(leg.getCreatedAt()).isEqualTo(at);
            });
        }
    }

    @Nested
    class GetHistory {

        @Test
        @SuppressWarnings("unchecked")
        void returnsMappedPageSortedNewestFirst() {
            UUID accountId = UUID.randomUUID();
            AccountTransaction entity = AccountTransaction.deposit(account("100.00"), BigDecimal.TEN, Instant.now());
            TransactionResponse response = new TransactionResponse();

            when(accountRepository.existsById(accountId)).thenReturn(true);
            when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 6));
            when(transactionMapper.toResponse(entity)).thenReturn(response);

            Page<TransactionResponse> page =
                    transactionService.getHistory(accountId, TransactionHistoryFilter.none(), PageRequest.of(1, 5));

            assertThat(page.getContent()).containsExactly(response);
            assertThat(page.getTotalElements()).isEqualTo(6);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(transactionRepository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable used = pageableCaptor.getValue();
            assertThat(used.getPageNumber()).isEqualTo(1);
            assertThat(used.getPageSize()).isEqualTo(5);
            assertThat(used.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(used.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        void throwsWhenAccountDoesNotExist() {
            UUID accountId = UUID.randomUUID();
            when(accountRepository.existsById(accountId)).thenReturn(false);

            assertThatThrownBy(() ->
                    transactionService.getHistory(accountId, TransactionHistoryFilter.none(), PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(accountId.toString());

            verifyNoInteractions(transactionRepository, transactionMapper);
        }
    }

    @Nested
    class Filter {

        @Test
        void rejectsFromAfterTo() {
            Instant now = Instant.now();

            assertThatThrownBy(() ->
                    new TransactionHistoryFilter(null, now, now.minusSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'from' must not be after 'to'");
        }

        @Test
        void acceptsOpenEndedRange() {
            Instant now = Instant.now();

            assertThat(new TransactionHistoryFilter(TransactionType.DEPOSIT, now, null).from()).isEqualTo(now);
            assertThat(new TransactionHistoryFilter(null, null, now).to()).isEqualTo(now);
        }
    }
}
