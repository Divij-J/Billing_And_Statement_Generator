package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionRequestDTO;
import com.example.billing_and_statement_generator.dto.transaction.CreateTransactionResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.mapper.TransactionMapper;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceBDDTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    CardRepository cardRepository;

    @Mock
    TransactionMapper transactionMapper;

    @Mock
    CardService cardService;

    @InjectMocks
    TransactionService transactionService;

    private UUID cardId;
    private Card card;
    private CreateTransactionRequestDTO dto;
    private Transaction tx;

    @BeforeEach
    void setup() {
        cardId = UUID.randomUUID();

        card = Card.builder()
                .cardId(cardId)
                .cashAdvanceFeeRate(BigDecimal.valueOf(0.02))
                .build();

        dto = CreateTransactionRequestDTO.builder()
                .cardId(cardId)
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(BigDecimal.valueOf(100))
                .transactionDate(LocalDate.now())
                .merchantName("BDD Test")
                .build();

        tx = Transaction.builder()
                .card(card)
                .amount(dto.getAmount())
                .transactionType(dto.getTransactionType())
                .transactionDate(dto.getTransactionDate())
                .build();
    }

    @Test
    void givenValidPurchase_whenCreated_thenTransactionIsPersisted() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(transactionMapper.toEntity(dto)).willReturn(tx);
        given(cardService.applyPurchase(cardId, dto.getAmount()))
                .willReturn(BigDecimal.TEN);

        // WHEN
        transactionService.create(dto);

        // THEN
        then(cardService).should().applyPurchase(cardId, dto.getAmount());
        then(transactionRepository).should().save(tx);
    }

    @Test
    void givenPurchaseExceedsLimit_whenCreated_thenTransactionIsDeclinedAndSaved() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(transactionMapper.toEntity(dto)).willReturn(tx);

        willThrow(new CardService.LimitExceededException("limit"))
                .given(cardService).applyPurchase(cardId, dto.getAmount());

        // WHEN / THEN
        assertThrows(CardService.LimitExceededException.class,
                () -> transactionService.create(dto));

        then(transactionRepository).should().save(tx);
    }

    @Test
    void givenValidCashAdvance_whenCreated_thenBalanceAreAppliedAndSaved() {
        // GIVEN
        dto.setTransactionType(Transaction.transactionType.CASHADVANCE);

        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(transactionMapper.toEntity(dto)).willReturn(tx);
        given(cardService.applyCashAdvance(cardId, dto.getAmount()))
                .willReturn(BigDecimal.TEN);

        // WHEN
        transactionService.create(dto);

        // THEN
        then(cardService).should().applyCashAdvance(cardId, dto.getAmount());
        then(cardService).should(never())
                .applyFee(any(), any());
        then(transactionRepository).should().save(tx);
    }

    @Test
    void givenUnsupportedTransactionType_whenCreated_thenRejectedWithoutSaving() {
        // GIVEN
        dto.setTransactionType(Transaction.transactionType.FEE);

        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        given(transactionMapper.toEntity(dto)).willReturn(tx);

        // WHEN / THEN
        assertThrows(TransactionService.ValidationException.class,
                () -> transactionService.create(dto));

        then(transactionRepository).should(never()).save(any());
    }

    @Test
    void givenMissingCard_whenTransactionCreated_thenRejected() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(TransactionService.NotFoundException.class,
                () -> transactionService.create(dto));

        then(transactionRepository).should(never()).save(any());
    }

    @Test
    void givenTransactionsRequestedByCard_whenFetched_thenNoMutationOccurs() {
        // GIVEN
        given(transactionRepository.findByCardCardId(cardId))
                .willReturn(List.of(tx));
        given(transactionMapper.toResponse(any()))
                .willReturn(new CreateTransactionResponseDTO());

        // WHEN
        transactionService.listByCard(cardId);

        // THEN
        then(transactionRepository).should(never()).save(any());
    }
}
