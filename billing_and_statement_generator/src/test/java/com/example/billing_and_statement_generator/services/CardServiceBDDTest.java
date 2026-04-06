package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.mapper.CardMapper;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
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

    @Mock
    CardMapper cardMapper;

    @Mock
    CustomerRepository customerRepository;

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
                .build();
    }

    @Test
    void shouldIncreaseBalanceWhenPurchaseApplied(){
        // GIVEN
        given(cardRepository.findById(cardId)).willReturn(Optional.of(card));
        // WHEN
        BigDecimal newBalance = cardService.applyPurchase(cardId, BigDecimal.valueOf(50));
        // THEN
        assertEquals(newBalance, card.getCardBalance());
        then(cardRepository).should().save(card);
    }

    @Test
    void shouldFailPurchaseWhenCreditLimitReached(){
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

    /* Other tests to consider
    - shouldCreateCardSuccessfully
    - shouldFailWhenCustomerNotFound
    - shouldFailWhenCardNumberExists

    - shouldIncreaseBalanceWhenPurchaseApplied
    - shouldFailPurchaseWhenCreditLimitExceeded
    - shouldFailPurchaseWhenCardInactive

    - shouldIncreaseCashAdvanceBalance
    - shouldFailCashAdvanceWhenOverLimit
    - shouldFailCashAdvanceWhenInactive

    - shouldApplyPaymentToCashAdvanceFirst
    - shouldFailWhenPaymentExceedsTotalBalance

    - shouldApplyInterestToCardBalance
    - shouldApplyInterestToCashAdvanceBalance
    - shouldFailForInvalidInterestType

    - shouldApplyFeeAndUpdateBalance
    - shouldFailFeeWhenCreditLimitExceeded

    - shouldReturnBalancesCorrectly
    - shouldFailGetBalanceWhenCardNotFound
    */
}
