package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.card.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.card.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.mapper.CardMapper;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.CustomerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private UUID customerId;
    private Customer customer;
    private CreateCardRequestDTO dto;
    private Card cardEntity;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();

        customer = Customer.builder()
                .customerId(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("5553334444")
                .phoneType(Customer.PhoneType.MOBILE)
                .address1("123 Main St")
                .city("Chicago")
                .state("IL")
                .zipcode("60601")
                .build();

        dto = CreateCardRequestDTO.builder()
                .customerId(customerId)
                .cardNumber("4111111111111111")
                .cardType(Card.CardType.CREDIT)
                .cardHolderName("John Doe")
                .securityCode("123")
                .build();

        cardEntity = Card.builder()
                .customer(customer)
                .cardNumber(dto.getCardNumber())
                .cardType(dto.getCardType())
                .cardHolderName(dto.getCardHolderName())
                .securityCode(dto.getSecurityCode())
                .build();
    }

    @Test
    void testCreateCardSuccess() {
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));
        when(cardRepository.existsByCardNumber(dto.getCardNumber()))
                .thenReturn(false);
        when(cardMapper.toEntity(dto)).thenReturn(cardEntity);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));


        CreateCardResponseDTO responseDTO = CreateCardResponseDTO.builder()
                .cardId(UUID.randomUUID())
                .customerId(customerId)
                .build();

        when(cardMapper.toResponse(any(Card.class))).thenReturn(responseDTO);

        CreateCardResponseDTO result = cardService.create(dto);

        assertNotNull(result, "Expected a non-null response when creating a card");
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toEntity(dto);
        verify(cardMapper).toResponse(any(Card.class));
    }

    @Test
    void testCreateCardCustomerNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(CardService.NotFoundException.class, () ->
            cardService.create(dto),
            "Expected NotFoundException when creating a card with missing customer"
        );
    }

    @Test
    void testCreateCardDuplicateNumber() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cardRepository.existsByCardNumber(dto.getCardNumber())).thenReturn(true);

        assertThrows(CardService.ConflictException.class, () ->
            cardService.create(dto),
            "Expected ConflictException when creating a card with a duplicate card number"
        );
    }

    @Test
    void testApplyPurchaseSuccess() {
        UUID cardId = UUID.randomUUID();

        Card card = Card.builder()
                .cardId(cardId)
                .active(true)
                .cardBalance(BigDecimal.ZERO)
                .cashAdvanceBalance(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(1000))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        BigDecimal result = cardService.applyPurchase(cardId, BigDecimal.valueOf(100));

        assertEquals(BigDecimal.valueOf(100), result, "Card balance after purchase did not match expected value");
        verify(cardRepository).save(card);
    }

    @Test
    void testApplyPurchaseExceedsLimit() {
        UUID cardId = UUID.randomUUID();

        Card card = Card.builder()
                .cardId(cardId)
                .active(true)
                .cardBalance(BigDecimal.valueOf(900))
                .cashAdvanceBalance(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(1000))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(CardService.LimitExceededException.class, () ->
                cardService.applyPurchase(cardId, BigDecimal.valueOf(200)),
                "Expected LimitExceededException when purchase exceeds credit limit");
    }

    @Test
    void testApplyPaymentSuccess() {
        UUID cardId = UUID.randomUUID();

        Card card = Card.builder()
                .cardId(cardId)
                .cardBalance(BigDecimal.valueOf(300))
                .cashAdvanceBalance(BigDecimal.valueOf(100))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        BigDecimal total = cardService.applyPayment(cardId, BigDecimal.valueOf(150));

        assertEquals(BigDecimal.valueOf(250), total, "Expected remaining balance after payment to match calculation");
    }

    @Test
    void testApplyInterestCardBalance() {
        UUID cardId = UUID.randomUUID();

        Card card = Card.builder()
                .cardId(cardId)
                .cardBalance(BigDecimal.valueOf(200))
                .cashAdvanceBalance(BigDecimal.ZERO)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        BigDecimal newBal = cardService.applyInterest(cardId, BigDecimal.valueOf(20), TransactionService.InterestType.CARDBALANCE);

        assertEquals(BigDecimal.valueOf(220), newBal, "Expected card balance after applying interest to match calculation");
    }

    @Test
    void testGetById() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(cardEntity));

        CreateCardResponseDTO responseDTO = new CreateCardResponseDTO();
        when(cardMapper.toResponse(cardEntity)).thenReturn(responseDTO);
        CreateCardResponseDTO result = cardService.getById(cardId);

        assertNotNull(result, "Expected non-null response when retrieving card by ID");
        verify(cardRepository).findById(cardId);
        verify(cardMapper).toResponse(cardEntity);
    }

    @Test
    void testApplyPurchaseInactiveCard() {
        UUID cardId = UUID.randomUUID();

        Card card = Card.builder()
                .cardId(cardId)
                .active(false)
                .cardBalance(BigDecimal.ZERO)
                .cashAdvanceBalance(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(1000))
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(CardService.ValidationException.class,
                () -> cardService.applyPurchase(cardId, BigDecimal.valueOf(50)),
                "Expected ValidationException for inactive card");
    }
}

