package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class CardRepositoryTest {
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CustomerRepository customerRepository;

    private Customer customer;
    private Card card;

    @BeforeEach
    void init(){
        customer = Customer.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .middleInitial("A")
                    .email("john.doe@example.com")
                    .phoneNumber("5551234567")
                    .phoneType(Customer.PhoneType.MOBILE)
                    .address1("123 Main St")
                    .city("Chicago")
                    .state("IL")
                    .zipcode("60601")
                    .build();

        customer = customerRepository.save(customer);

        card = Card.builder()
                .cardId(UUID.randomUUID())
                        .customer(customer)
                        .cardNumber("4111111111111111")
                        .cardType(Card.CardType.CREDIT)
                        .cardHolderName("John Doe")
                        .cardIssueDate(LocalDate.now().minusYears(1))
                        .expiryDate(LocalDate.now().plusYears(3))
                        .active(true)
                        .cardBalance(BigDecimal.ZERO)
                        .cashAdvanceBalance(BigDecimal.ZERO)
                        .creditLimit(BigDecimal.valueOf(5000))
                        .securityCode("123")
                        .build();
    }

    @Test
    void testCardSave(){
        Card saved = cardRepository.save(card);

        assertNotNull(saved.getCardId(), "Card was not saved correctly and is missing");
        assertEquals(card.getCardNumber(), saved.getCardNumber(), "Saved cardNumber does not match the original value");
        assertEquals(card.getCardHolderName(), saved.getCardHolderName(), "Saved cardHolderName does not match the original value");
        assertEquals(card.getCreditLimit(), saved.getCreditLimit(), "Saved creditLimit does not match the original value");
        assertEquals(customer.getCustomerId(), saved.getCustomer().getCustomerId(), "Saved card is not linked to the correct customer");

        Card found = cardRepository.findById(saved.getCardId()).orElseThrow();
        assertEquals("4111111111111111", found.getCardNumber(), "Could not retrieve the saved card or cardNumber was incorrect");
    }


    @Test
    void testFindCardsByCustomerId() {
        cardRepository.save(card);

        List<Card> cards = cardRepository.findByCustomerCustomerId(customer.getCustomerId());

        assertEquals(1, cards.size(),"Expected exactly 1 card for this customer, but found a different count");
        assertEquals(customer.getCustomerId(), cards.get(0).getCustomer().getCustomerId(),"Retrieved card does not belong to the expected customer");
        assertEquals("4111111111111111", cards.get(0).getCardNumber(),"Retrieved cardNumber does not match the saved value");
    }


    @Test
    void testMultipleCardsForCustomer() {
        Card card2 = Card.builder()
                .cardId(UUID.randomUUID())
                .customer(customer)
                .cardNumber("5555444433332222")
                .cardType(Card.CardType.DEBIT)
                .cardHolderName("John Doe")
                .cardIssueDate(LocalDate.now().minusYears(2))
                .expiryDate(LocalDate.now().plusYears(2))
                .active(true)
                .cardBalance(BigDecimal.ZERO)
                .cashAdvanceBalance(BigDecimal.ZERO)
                .creditLimit(BigDecimal.valueOf(3000))
                .securityCode("789")
                .build();

        cardRepository.save(card);
        cardRepository.save(card2);

        List<Card> cards = cardRepository.findByCustomerCustomerId(customer.getCustomerId());
        assertEquals(2, cards.size(), "Expected 2 cards for customer, but result count was incorrect");
    }
}
