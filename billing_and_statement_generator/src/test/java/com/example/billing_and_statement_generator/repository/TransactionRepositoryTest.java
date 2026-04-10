package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired(required = false)
    private BillingCycleRepository billingCycleRepository;

    private Customer customer;
    private Card card;
    private BillingCycle billingCycle;

    @BeforeEach
    void setup() {
        customer = Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .middleInitial("A")
                .email("john.doe@example.com")
                .phoneNumber("5551234567")
                .phoneType(Customer.PhoneType.MOBILE)
                .address1("123 Main St")
                .address2("Apt 4")
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
                .availableCredit(BigDecimal.valueOf(5000))
                .creditLimit(BigDecimal.valueOf(5000))
                .securityCode("123")
                .build();

        card = cardRepository.save(card);

        if (billingCycleRepository != null) {
            billingCycle = BillingCycle.builder()
                    .cycleId(UUID.randomUUID())
                    .cycleStartDate(LocalDate.now().minusDays(30))
                    .cycleEndDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(20))
                    .build();

            billingCycle = billingCycleRepository.save(billingCycle);
        }
    }

    // Helper method to create transactions
    private Transaction buildTransaction(LocalDate date, BillingCycle cycle) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID())
                .card(card)
                .billingCycle(cycle)
                .transactionDate(date)
                .transactionType(Transaction.transactionType.PURCHASE)
                .amount(BigDecimal.valueOf(100.00))
                .merchantName("Test Transaction")
                .status(Transaction.Status.SENT)
                .build();
    }

    @Test
    void testSaveTransaction() {
        Transaction tx = buildTransaction(LocalDate.now(), billingCycle);

        Transaction saved = transactionRepository.save(tx);

        assertNotNull(saved.getTransactionId(),"Transaction was not saved correctly; transactionId is null");
        assertEquals("Test Transaction", saved.getMerchantName(), "Merchant name was not saved or retrieved correctly");
        assertEquals(card.getCardId(), saved.getCard().getCardId(), "Saved transaction is not linked to the correct card");
        assertEquals(tx.getAmount(), saved.getAmount(), "Transaction amount was not saved correctly");
    }

    @Test
    void testFindByCardCardId() {
        Transaction tx = transactionRepository.save(buildTransaction(LocalDate.now(), billingCycle));

        List<Transaction> result = transactionRepository.findByCardCardId(card.getCardId());

        assertEquals(1, result.size(), "Expected exactly 1 transaction for this card, but found a different number");
        assertEquals("Test Transaction", result.get(0).getMerchantName(), "Merchant name in retrieved transaction does not match expected value");
    }

    @Test
    void testFindByBillingCycleCycleId() {
        Transaction tx = transactionRepository.save(buildTransaction(LocalDate.now(), billingCycle));

        List<Transaction> result = transactionRepository.findByBillingCycleCycleId(billingCycle.getCycleId());

        assertEquals(1, result.size(), "Expected exactly 1 transaction linked to this billing cycle");
    }

    @Test
    void testFindByCardIdAndCycleId() {
        Transaction tx = transactionRepository.save(buildTransaction(LocalDate.now(), billingCycle));

        List<Transaction> result =
                transactionRepository.findByCardCardIdAndBillingCycleCycleId(card.getCardId(), billingCycle.getCycleId());

        assertEquals(1, result.size(), "Expected 1 transaction for the given card and billing cycle");
    }

    @Test
    void testFindBetweenDates() {
        Transaction t1 = transactionRepository.save(buildTransaction(LocalDate.now().minusDays(5), billingCycle));
        Transaction t2 = transactionRepository.save(buildTransaction(LocalDate.now().minusDays(1), billingCycle));
        transactionRepository.save(buildTransaction(LocalDate.now().minusDays(10), billingCycle)); // Outside range

        List<Transaction> result = transactionRepository.findByCardCardIdAndTransactionDateBetween(
                card.getCardId(),
                LocalDate.now().minusDays(7),
                LocalDate.now()
        );

        assertEquals(2, result.size(), "Query for transactions in date range returned an incorrect number of results");
    }

    @Test
    void testFindByCardAndBillingCycleIsNull() {
        Transaction t1 = transactionRepository.save(buildTransaction(LocalDate.now(), null));
        transactionRepository.save(buildTransaction(LocalDate.now(), billingCycle));

        List<Transaction> result =
                transactionRepository.findByCardCardIdAndBillingCycleIsNull(card.getCardId());

        assertEquals(1, result.size(), "Expected exactly 1 transaction with a null billing cycle for this card");
        assertNull(result.get(0).getBillingCycle(), "Returned transaction was expected to have a null billing cycle but did not");
    }
}