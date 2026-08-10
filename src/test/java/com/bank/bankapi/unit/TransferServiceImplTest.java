package com.bank.bankapi.unit;


import com.bank.bankapi.entity.Account;
import com.bank.bankapi.exception.InsufficientFundsException;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.metrics.BankMetrics;
import com.bank.bankapi.outbox.OutboxService;
import com.bank.bankapi.repository.AccountRepository;

import com.bank.bankapi.dto.TransferRequest;
import com.bank.bankapi.dto.TransferResponse;

import com.bank.bankapi.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {


    @Mock
    private AccountRepository accountRepository;


    @Mock
    private OutboxService outboxService;


    @Mock
    private BankMetrics bankMetrics;


    @InjectMocks
    private TransferServiceImpl transferService;


    private UUID senderId;
    private UUID receiverId;


    private Account sender;
    private Account receiver;


    @BeforeEach
    void setUp() {

        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();


        sender = new Account(null, new BigDecimal("1000.00"));
        receiver = new Account(null, new BigDecimal("500.00"));


        ReflectionTestUtils.setField(
                sender,
                "id",
                senderId
        );


        ReflectionTestUtils.setField(
                receiver,
                "id",
                receiverId
        );
    }


    private TransferRequest createRequest(BigDecimal amount) {

        TransferRequest request = new TransferRequest();

        request.setSenderAccountId(senderId);
        request.setReceiverAccountId(receiverId);
        request.setAmount(amount);

        return request;
    }


    @Nested
    class SuccessfulTransfer {


        @Test
        void transfersMoney() {


            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.of(sender));

            when(accountRepository.findById(receiverId))
                    .thenReturn(Optional.of(receiver));


            TransferRequest request =
                    createRequest(new BigDecimal("200.00"));

            TransferResponse response =
                    transferService.transfer(request);

            assertThat(sender.getBalance())
                    .isEqualByComparingTo("800.00");


            assertThat(receiver.getBalance())
                    .isEqualByComparingTo("700.00");


            assertThat(response.getSenderAccountId())
                    .isEqualTo(senderId);

            assertThat(response.getReceiverAccountId())
                    .isEqualTo(receiverId);
        }


        @Test
        void createsOutboxEvent() {

            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.of(sender));

            when(accountRepository.findById(receiverId))
                    .thenReturn(Optional.of(receiver));


            TransferRequest request =
                    createRequest(new BigDecimal("200.00"));


            transferService.transfer(request);


            verify(outboxService)
                    .save(
                            eq("TransferCompleted"),
                            any(TransferCompletedEvent.class)
                    );
        }


        @Test
        void incrementsMetrics() {

            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.of(sender));

            when(accountRepository.findById(receiverId))
                    .thenReturn(Optional.of(receiver));


            TransferRequest request =
                    createRequest(new BigDecimal("200.00"));


            transferService.transfer(request);


            verify(bankMetrics)
                    .incrementTransfersMade();
        }
    }
    @Nested
    class SenderMissing {


        @Test
        void throwsResourceNotFound() {

            TransferRequest request =
                    createRequest(new BigDecimal("100.00"));


            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.empty());


            assertThatThrownBy(() ->
                    transferService.transfer(request)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account")
                    .hasMessageContaining(senderId.toString());


            verify(accountRepository)
                    .findById(senderId);


            verify(accountRepository, never())
                    .findById(receiverId);


            verifyNoInteractions(outboxService);
        }
    }




    @Nested
    class ReceiverMissing {


        @Test
        void throwsResourceNotFound() {


            TransferRequest request =
                    createRequest(new BigDecimal("100.00"));


            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.of(sender));


            when(accountRepository.findById(receiverId))
                    .thenReturn(Optional.empty());



            assertThatThrownBy(() ->
                    transferService.transfer(request)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account")
                    .hasMessageContaining(receiverId.toString());



            verify(accountRepository)
                    .findById(senderId);


            verify(accountRepository)
                    .findById(receiverId);


            verifyNoInteractions(outboxService);
        }
    }





    @Nested
    class InsufficientFunds {


        @Test
        void throwsInsufficientFundsException() {


            sender =
                    new Account(
                            null,
                            new BigDecimal("50.00")
                    );


            ReflectionTestUtils.setField(
                    sender,
                    "id",
                    senderId
            );


            TransferRequest request =
                    createRequest(new BigDecimal("100.00"));



            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.of(sender));


            when(accountRepository.findById(receiverId))
                    .thenReturn(Optional.of(receiver));



            assertThatThrownBy(() ->
                    transferService.transfer(request)
            )
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining(senderId.toString());



            // receiver should not receive money
            assertThat(receiver.getBalance())
                    .isEqualByComparingTo("500.00");


            verifyNoInteractions(outboxService);


            verify(bankMetrics, never())
                    .incrementTransfersMade();
        }
    }





    @Nested
    class SameAccount {


        @Test
        void cannotTransferToYourself() {


            TransferRequest request =
                    new TransferRequest();


            request.setSenderAccountId(senderId);

            request.setReceiverAccountId(senderId);

            request.setAmount(
                    new BigDecimal("100.00")
            );



            assertThatThrownBy(() ->
                    transferService.transfer(request)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "Sender and receiver accounts must be different"
                    );



            verifyNoInteractions(accountRepository);

            verifyNoInteractions(outboxService);

            verify(bankMetrics, never())
                    .incrementTransfersMade();
        }
    }

    @Nested
    class EventCreation {


        @Test
        void publishesCorrectEvent() {

            when(accountRepository.findById(senderId))
                    .thenReturn(Optional.of(sender));

            when(accountRepository.findById(receiverId))
                    .thenReturn(Optional.of(receiver));


            TransferRequest request =
                    createRequest(new BigDecimal("200.00"));


            transferService.transfer(request);


            ArgumentCaptor<Object> eventCaptor =
                    ArgumentCaptor.forClass(Object.class);


            verify(outboxService)
                    .save(
                            eq("TransferCompleted"),
                            eventCaptor.capture()
                    );


            Object capturedEvent =
                    eventCaptor.getValue();


            assertThat(capturedEvent)
                    .isInstanceOf(TransferCompletedEvent.class);


            TransferCompletedEvent event =
                    (TransferCompletedEvent) capturedEvent;


            assertThat(event.eventId())
                    .isNotNull();


            assertThat(event.fromAccountId())
                    .isEqualTo(senderId);


            assertThat(event.toAccountId())
                    .isEqualTo(receiverId);


            assertThat(event.amount())
                    .isEqualByComparingTo("200.00");


            assertThat(event.occurredAt())
                    .isNotNull();
        }
    }



}
