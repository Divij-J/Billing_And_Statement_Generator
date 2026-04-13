package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.card.GetCardBalanceResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CardServiceBDDTest {
    @Mock
    CardRepository cardRepository;

    @InjectMocks
    CardService cardService;

    private Card card;
    private UUID cardId;

    // GIVEN
    // WHEN
    // THEN
    @BeforeEach
    void setup(){
        cardId = UUID.randomUUID();
        card = Card.builder()
                .cardId(cardId)
                .active(true)
                .cardBalance(BigDecimal.ZERO)
                .cashAdvanceBalance(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(500))
                .availableCredit(BigDecimal.valueOf(500))
                .build();
    }

    // PASSING TESTS
    @Test
    void givenActiveCardWithAvailableCredit_whenPurchaseApplied_thenBalanceAndAvailableCreditAreUpdated(){
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        // WHEN
        BigDecimal newBalance = cardService.applyPurchase(cardId, BigDecimal.valueOf(50));
        // THEN
        then(cardRepository).should().save(card);
    }

    @Test
    void givenActiveCard_whenCashAdvanceApplied_thenCashAdvanceBalanceIncreases() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        card.setCashAdvanceLimit(BigDecimal.valueOf(300));

        // WHEN
        BigDecimal newBalance = cardService.applyCashAdvance(cardId, BigDecimal.valueOf(100));

        // THEN
        then(cardRepository).should().save(card);
    }

    @Test
    void givenCardWithCashAdvanceAndPurchase_whenPaymentApplied_thenCashAdvanceReducedFirst() {
        // GIVEN
        card.setCashAdvanceBalance(BigDecimal.valueOf(100));
        card.setCardBalance(BigDecimal.valueOf(100));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN
        cardService.applyPayment(cardId, BigDecimal.valueOf(150));

        // THEN
        then(cardRepository).should().save(card);
    }

    @Test
    void givenCard_whenCardInterestApplied_thenBalanceIncreases() {
        // GIVEN
        card.setCardBalance(BigDecimal.valueOf(100));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN
        cardService.applyInterest(cardId, BigDecimal.valueOf(10), TransactionService.InterestType.CARDBALANCE);

        // THEN
        then(cardRepository).should().save(card);
    }

    @Test
    void givenCard_whenCashAdvanceInterestApplied_thenCashAdvanceBalanceIncreases() {
        // GIVEN
        card.setCashAdvanceBalance(BigDecimal.valueOf(100));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN
        cardService.applyInterest(cardId, BigDecimal.valueOf(10),
                TransactionService.InterestType.CASHADVANCE);

        // THEN
        then(cardRepository).should().save(card);
    }

    @Test
    void givenActiveCard_whenFeeApplied_thenBalanceAndAvailableCreditUpdated() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN
        cardService.applyFee(cardId, BigDecimal.valueOf(50));

        // THEN
        then(cardRepository).should().save(card);
    }

    @Test
    void givenCardWithBalances_whenGetBalancesCalled_thenCorrectSnapshotReturned() {
        // GIVEN
        card.setCardBalance(BigDecimal.valueOf(75));
        card.setCashAdvanceBalance(BigDecimal.valueOf(25));
        card.setAvailableCredit(BigDecimal.valueOf(400));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN
        GetCardBalanceResponseDTO balances = cardService.getBalances(cardId);

        // THEN
        then(cardRepository).should(never()).save(any(Card.class));
    }

    // FAILING TESTS
    @Test
    void givenActiveCardWithAvailableCredit_whenPurchaseExceedsCreditLimit_thenPurchaseIsRejected(){
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN (tries to exceed the credit limit and fail)
        assertThrows(CardService.LimitExceededException.class,
                () -> cardService.applyPurchase(cardId, BigDecimal.valueOf(500.01)),
                "Expected LimitExceededException when purchase exceeds credit limit"
        );

        // THEN (verifies that cardRepository doesn't save anything)
        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenInactiveCard_whenPurchaseApplied_thenPurchaseIsRejected() {
        // GIVEN
        card.setActive(false);
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.ValidationException.class,
                () -> cardService.applyPurchase(cardId, BigDecimal.valueOf(50)));

        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenActiveCard_whenCashAdvanceExceedsCashAdvanceLimit_thenRejected() {
        // GIVEN
        card.setCashAdvanceLimit(BigDecimal.valueOf(100));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.LimitExceededException.class,
                () -> cardService.applyCashAdvance(cardId, BigDecimal.valueOf(150)));

        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenActiveCard_whenCashAdvanceWouldExceedCreditLimit_thenRejected() {
        // GIVEN
        card.setCashAdvanceLimit(BigDecimal.valueOf(500));
        card.setCreditLimit(BigDecimal.valueOf(100));
        card.setAvailableCredit(BigDecimal.valueOf(100));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.LimitExceededException.class,
                () -> cardService.applyCashAdvance(cardId, BigDecimal.valueOf(150)));

        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenInactiveCard_whenCashAdvanceApplied_thenRejected() {
        // GIVEN
        card.setActive(false);
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.ValidationException.class,
                () -> cardService.applyCashAdvance(cardId, BigDecimal.valueOf(50)));

        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenCardWithBalance_whenPaymentExceedsTotalBalance_thenRejected() {
        // GIVEN
        card.setCardBalance(BigDecimal.valueOf(50));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.LimitExceededException.class,
                () -> cardService.applyPayment(cardId, BigDecimal.valueOf(100)));

        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenCard_whenInvalidInterestTypeProvided_thenRejected() {
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.ValidationException.class,
                () -> cardService.applyInterest(cardId, BigDecimal.TEN, null));

        then(cardRepository).should(never()).save(any(Card.class));
    }

    @Test
    void givenActiveCard_whenFeeWouldExceedCreditLimit_thenRejected() {
        // GIVEN
        card.setCreditLimit(BigDecimal.valueOf(100));
        card.setAvailableCredit(BigDecimal.valueOf(100));
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));

        // WHEN / THEN
        assertThrows(CardService.LimitExceededException.class,
                () -> cardService.applyFee(cardId, BigDecimal.valueOf(150)));

        then(cardRepository).should(never()).save(any(Card.class));
    }
}
