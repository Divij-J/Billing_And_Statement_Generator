package com.example.billing_and_statement_generator.cucumber.stepdefs;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.entity.BillingCycle;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.entity.Transaction;
import com.example.billing_and_statement_generator.repository.BillingCycleRepository;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import com.example.billing_and_statement_generator.repository.TransactionRepository;
import com.example.billing_and_statement_generator.services.BillingService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BillingStepDefinitions {

    @Autowired private BillingService billingService;
    @Autowired private CardRepository cardRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BillingCycleRepository billingCycleRepository;

    private Customer customer;
    private Card card;
    private BillingCycleResponseDTO response;

    @Before
    public void reset() {
        transactionRepository.deleteAll();
        billingCycleRepository.deleteAll();
        cardRepository.deleteAll();
        customerRepository.deleteAll();
        response = null;
    }

    // GIVEN steps
    @Given("a billing customer exists")
    public void billingCustomerExists() {
        customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("bill." + UUID.randomUUID() + "@example.com");
        customer.setPhoneNumber("6" + (int)(Math.random() * 900000000 + 100000000));
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Main St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");
        customer = customerRepository.save(customer);
    }

    @Given("a billing card exists with credit limit of {int}")
    public void billingCardExists(int creditLimit) {
        card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCustomer(customer);
        card.setCardNumber("4111111111111111");
        card.setCardType(Card.CardType.CREDIT);
        card.setCardHolderName("John Doe");
        card.setCardIssueDate(LocalDate.now().minusYears(1));
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setSecurityCode("123");
        card.setActive(true);
        card.setCreditLimit(BigDecimal.valueOf(creditLimit));
        card.setAvailableCredit(BigDecimal.valueOf(creditLimit));
        card.setCardBalance(BigDecimal.ZERO);
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        card.setAnnualInterestRate(new BigDecimal("0.20"));
        card.setCashAdvanceAPR(new BigDecimal("0.24"));
        card.setCashAdvanceFeeRate(new BigDecimal("0.02"));
        card.setLateFeeAmount(new BigDecimal("50.00"));
        card.setMinimumDue(new BigDecimal("100.00"));
        card.setCashAdvanceLimit(new BigDecimal("1000.00"));
        card.setAnnualMembershipFee(BigDecimal.ZERO);
        cardRepository.save(card);
    }

    @Given("the billing card has zero balances")
    public void cardHasZeroBalances() {
        card.setCardBalance(BigDecimal.ZERO);
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        cardRepository.save(card);
    }

    @Given("the billing card has a cash advance of {int}")
    public void billingCardHasCashAdvance(int amount) {
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setCard(card);
        tx.setTransactionType(Transaction.transactionType.CASHADVANCE);
        tx.setAmount(BigDecimal.valueOf(amount));
        tx.setTransactionDate(LocalDate.now());
        tx.setStatus(Transaction.Status.SENT);
        transactionRepository.save(tx);

        card.setCashAdvanceBalance(card.getCashAdvanceBalance().add(BigDecimal.valueOf(amount)));
        card.setAvailableCredit(card.getAvailableCredit().subtract(BigDecimal.valueOf(amount)));
        cardRepository.save(card);
    }

    @Given("a previous billing cycle exists with outstanding balance of {double}")
    public void previousBillingCycleExists(double balance) {
        BigDecimal outstanding = BigDecimal.valueOf(balance);

        // Set card balances so interest base exists
        card.setCashAdvanceBalance(outstanding);
        card.setAvailableCredit(card.getCreditLimit().subtract(outstanding));
        cardRepository.save(card);

        // Create previous billing cycle
        BillingCycle cycle = new BillingCycle();
        cycle.setCycleId(UUID.randomUUID());
        cycle.setCard(card);
        cycle.setCycleStartDate(LocalDate.now().minusDays(60));
        cycle.setCycleEndDate(LocalDate.now().minusDays(30));
        cycle.setDueDate(LocalDate.now().minusDays(5));
        cycle.setTotalOutstanding(outstanding);
        cycle.setCycleStatus("OPEN");
        billingCycleRepository.save(cycle);
    }

    @Given("a previous billing cycle exists and is past due with balance {int}")
    public void overdueCycleExists(int balance) {
        previousBillingCycleExists(balance);
    }

    // WHEN steps
    @When("a billing cycle is generated")
    public void billingCycleGenerated() {
        response = billingService.generateBillingCycle(card.getCardId());
    }

    // THEN steps
    @Then("the billing cycle should be created")
    public void billingCycleCreated() {
        assertNotNull(response);
        assertNotNull(response.getCycleId());
    }

    @Then("the total cash advance should be {int}")
    public void totalCashAdvanceShouldBe(int expected) {
        assertEquals(
                0,
                response.getTotalCashAdvance()
                        .compareTo(BigDecimal.valueOf(expected)),
                "Total cash advance mismatch"
        );

    }

    @Then("a cash advance fee should be applied")
    public void cashAdvanceFeeApplied() {
        assertTrue(response.getCashAdvanceFee().compareTo(BigDecimal.ZERO) > 0);
    }

    @Then("interest should not be charged")
    public void noInterest() {
        assertEquals(BigDecimal.ZERO, response.getTotalInterest());
    }

    @Then("interest should be charged")
    public void interestCharged() {
        assertTrue(response.getTotalInterest().compareTo(BigDecimal.ZERO) > 0);
    }

    @Then("a late fee should be applied")
    public void lateFeeApplied() {
        assertTrue(response.getLateFee().compareTo(BigDecimal.ZERO) > 0);
    }
}
