package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BillingCycleRepositoryTest {

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Card card;

    @BeforeEach
    void setup() {
        // ---- Create Customer
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setMiddleInitial("A");
        customer.setEmail("john@example.com");
        customer.setPhoneNumber("1234567890");
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Test St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");
        customerRepository.save(customer);

        // ---- Create Card
        card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCustomer(customer);
        card.setCardNumber("4111111111111111");
        card.setCardType(Card.CardType.CREDIT);

        card.setCardHolderName("John Doe");
        card.setCardIssueDate(LocalDate.now().minusYears(1));

        card.setExpiryDate(LocalDate.now().plusYears(2));
        card.setSecurityCode("123");
        card.setActive(true);

        card.setCreditLimit(new BigDecimal("5000"));
        card.setCardBalance(BigDecimal.ZERO);
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        card.setAvailableCredit(BigDecimal.valueOf(5000));
        card.setAnnualInterestRate(new BigDecimal("0.20"));
        card.setCashAdvanceAPR(new BigDecimal("0.24"));
        card.setLateFeeAmount(new BigDecimal("25"));
        card.setMinimumDue(BigDecimal.ZERO);

        cardRepository.save(card);

        // ---- Insert Billing Cycles
        BillingCycle cycle1 = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleEndDate(LocalDate.of(2024, 1, 1))
                .cycleStatus("OPEN")
                .previousBalance(BigDecimal.ZERO)
                .totalPurchases(BigDecimal.ZERO)
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(BigDecimal.ZERO)
                .totalOutstanding(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .creditLimit(card.getCreditLimit())
                .dueDate(LocalDate.of(2024, 1, 25))
                .build();

        BillingCycle cycle2 = BillingCycle.builder()
                .cycleId(UUID.randomUUID())
                .card(card)
                .cycleEndDate(LocalDate.of(2024, 2, 1)) // MOST RECENT
                .cycleStatus("CLOSED")
                .previousBalance(BigDecimal.ZERO)
                .totalPurchases(BigDecimal.ZERO)
                .totalCashAdvance(BigDecimal.ZERO)
                .totalInterest(BigDecimal.ZERO)
                .totalOutstanding(BigDecimal.ZERO)
                .minimumDue(BigDecimal.ZERO)
                .creditLimit(card.getCreditLimit())
                .dueDate(LocalDate.of(2024, 2, 25))
                .build();

        billingCycleRepository.save(cycle1);
        billingCycleRepository.save(cycle2);
    }

    // -----------------------------------------------------------------------
    // TEST 1 — findByCardCardId()
    // -----------------------------------------------------------------------
    @Test
    void findByCardCardId_shouldReturnAllCycles() {
        List<BillingCycle> cycles = billingCycleRepository.findByCardCardId(card.getCardId());

        assertEquals(2, cycles.size());
        assertTrue(cycles.stream()
                .allMatch(c -> c.getCard().getCardId().equals(card.getCardId())));
    }

    // -----------------------------------------------------------------------
    // TEST 2 — findTopByCardCardIdOrderByCycleEndDateDesc()
    // -----------------------------------------------------------------------
    @Test
    void findTopByCardCardIdOrderByCycleEndDateDesc_shouldReturnMostRecentCycle() {
        Optional<BillingCycle> result =
                billingCycleRepository.findTopByCardCardIdOrderByCycleEndDateDesc(card.getCardId());

        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2024, 2, 1), result.get().getCycleEndDate());
    }

    // -----------------------------------------------------------------------
    // TEST 3 — findByCardCardIdAndCycleStatus()
    // -----------------------------------------------------------------------
    @Test
    void findByCardCardIdAndCycleStatus_shouldReturnOnlyFilteredCycles() {
        List<BillingCycle> openCycles =
                billingCycleRepository.findByCardCardIdAndCycleStatus(card.getCardId(), "OPEN");

        assertEquals(1, openCycles.size());
        assertEquals("OPEN", openCycles.get(0).getCycleStatus());
    }
}
