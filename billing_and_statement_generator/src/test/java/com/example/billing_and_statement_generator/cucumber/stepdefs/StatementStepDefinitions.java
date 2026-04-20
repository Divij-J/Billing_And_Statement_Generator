package com.example.billing_and_statement_generator.cucumber.stepdefs;

import com.example.billing_and_statement_generator.dto.payment.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.statement.GenerateStatementResponseDTO;
import com.example.billing_and_statement_generator.dto.statement.RetrieveStatementResponseDTO;
import com.example.billing_and_statement_generator.entity.*;
import com.example.billing_and_statement_generator.repository.*;
import com.example.billing_and_statement_generator.services.PaymentService;
import com.example.billing_and_statement_generator.services.StatementService;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StatementStepDefinitions {

    @Autowired private StatementService statementService;
    @Autowired private PaymentService paymentService;
    @Autowired private CardRepository cardRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BillingCycleRepository billingCycleRepository;
    @Autowired private StatementRepository statementRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private TransactionRepository transactionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Scenario-scoped state
    private Customer customer;
    private Card card;
    private Card differentCard;
    private BillingCycle billingCycle;
    private GenerateStatementResponseDTO generateResponse;
    private RetrieveStatementResponseDTO retrieveResponse;
    private Exception thrownException;
    private UUID generatedStatementId;

    @Before
    @Transactional
    public void reset() {
        statementRepository.deleteAll();
        paymentRepository.deleteAll();
        transactionRepository.deleteAll();
        billingCycleRepository.deleteAll();
        cardRepository.deleteAll();
        customerRepository.deleteAll();
        generateResponse = null;
        retrieveResponse = null;
        thrownException = null;
        generatedStatementId = null;
        differentCard = null;
        customer = null;
        card = null;
        billingCycle = null;
    }

// BACKGROUND STEPS

    @Given("a statement customer exists in the system")
    @Transactional
    public void aStatementCustomerExistsInTheSystem() {
        customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("stmt." + UUID.randomUUID() + "@example.com");
        customer.setPhoneNumber("6" + (int)(Math.random() * 900000000 + 100000000));
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("456 Oak Ave");
        customer.setCity("Austin");
        customer.setState("TX");
        customer.setZipcode("73301");
        customer = customerRepository.save(customer);
    }

    @Given("a statement card exists with a credit limit of {double}")
    @Transactional
    public void aStatementCardExistsWithACreditLimitOf(double creditLimit) {
        card = buildCard(customer, creditLimit);
        card = cardRepository.save(card);
    }

    @Given("the statement card has a balance of {double} and cash advance balance of {double}")
    @Transactional
    public void theStatementCardHasABalanceOfAndCashAdvanceBalanceOf(
            double cardBal, double cashBal) {
        card = cardRepository.findById(card.getCardId()).orElseThrow();
        card.setCardBalance(BigDecimal.valueOf(cardBal));
        card.setCashAdvanceBalance(BigDecimal.valueOf(cashBal));
        card.setAvailableCredit(card.getCreditLimit()
                .subtract(BigDecimal.valueOf(cardBal))
                .subtract(BigDecimal.valueOf(cashBal)));
        card = cardRepository.save(card);
    }

    @Given("a billing cycle exists for the statement card with total outstanding of {double} and minimum due of {double}")
    @Transactional
    public void aBillingCycleExistsForTheStatementCardWithTotalOutstandingAndMinimumDue(
            double totalOutstanding, double minimumDue) {
        card = cardRepository.findById(card.getCardId()).orElseThrow();
        billingCycle = new BillingCycle();
        billingCycle.setCycleId(UUID.randomUUID());
        billingCycle.setCard(card);
        billingCycle.setCycleStartDate(LocalDate.now().minusDays(30));
        billingCycle.setCycleEndDate(LocalDate.now());
        billingCycle.setDueDate(LocalDate.now().plusDays(21));
        billingCycle.setCreditLimit(card.getCreditLimit());
        billingCycle.setPreviousBalance(BigDecimal.ZERO);
        billingCycle.setTotalPurchases(BigDecimal.valueOf(totalOutstanding));
        billingCycle.setTotalCashAdvance(BigDecimal.ZERO);
        billingCycle.setTotalInterest(BigDecimal.ZERO);
        billingCycle.setTotalOutstanding(BigDecimal.valueOf(totalOutstanding));
        billingCycle.setMinimumDue(BigDecimal.valueOf(minimumDue));
        billingCycle.setCycleStatus("OPEN");
        billingCycle = billingCycleRepository.save(billingCycle);
    }

// GIVEN STEPS — statement specific

    @Given("a statement already exists for the billing cycle")
    public void aStatementAlreadyExistsForTheBillingCycle() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(card.getCardId().toString())
                .cycleId(billingCycle.getCycleId().toString())
                .build();
        statementService.generateStatement(dto);
    }

    @Given("a different statement card exists in the system")
    @Transactional
    public void aDifferentStatementCardExistsInTheSystem() {
        customer = customerRepository.findById(customer.getCustomerId()).orElseThrow();
        differentCard = buildCard(customer, 3000.00);
        differentCard = cardRepository.save(differentCard);
    }

    @Given("a statement has been generated for the card and billing cycle")
    public void aStatementHasBeenGeneratedForTheCardAndBillingCycle() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(card.getCardId().toString())
                .cycleId(billingCycle.getCycleId().toString())
                .build();
        GenerateStatementResponseDTO response = statementService.generateStatement(dto);
        generatedStatementId = UUID.fromString(response.getStatementId());
    }

    @Given("a payment of {double} has been made against the statement card")
    public void aPaymentOfHasBeenMadeAgainstTheStatementCard(double amount) {
        PaymentRequestDTO paymentDTO = PaymentRequestDTO.builder()
                .cardId(card.getCardId().toString())
                .amountPaid(String.valueOf(amount))
                .paymentMethod("ONLINE")
                .build();
        paymentService.processPayment(paymentDTO);
    }

// WHEN STEPS

    @When("a statement is generated for the card and billing cycle")
    public void aStatementIsGeneratedForTheCardAndBillingCycle() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(card.getCardId().toString())
                .cycleId(billingCycle.getCycleId().toString())
                .build();
        try {
            generateResponse = statementService.generateStatement(dto);
            generatedStatementId = UUID.fromString(generateResponse.getStatementId());
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
            generateResponse = null;
        }
    }

    @When("a statement is generated for the different card using the original billing cycle")
    public void aStatementIsGeneratedForTheDifferentCardUsingTheOriginalBillingCycle() {
        GenerateStatementRequestDTO dto = GenerateStatementRequestDTO.builder()
                .cardId(differentCard.getCardId().toString())
                .cycleId(billingCycle.getCycleId().toString())
                .build();
        try {
            generateResponse = statementService.generateStatement(dto);
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
            generateResponse = null;
        }
    }

    @When("the statement is retrieved by its statement ID")
    public void theStatementIsRetrievedByItsStatementId() {
        try {
            retrieveResponse = statementService.getStatement(generatedStatementId);
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
            retrieveResponse = null;
        }
    }

    @When("a statement is retrieved with a random non-existent ID")
    public void aStatementIsRetrievedWithARandomNonExistentId() {
        try {
            retrieveResponse = statementService.getStatement(UUID.randomUUID());
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
            retrieveResponse = null;
        }
    }

// THEN STEPS

    @Then("the statement should be saved successfully")
    public void theStatementShouldBeSavedSuccessfully() {
        assertNull(thrownException,
                "Expected no exception but got: " +
                        (thrownException != null ? thrownException.getMessage() : ""));
        assertNotNull(generateResponse, "Generate response should not be null");
        assertNotNull(generateResponse.getStatementId(), "Statement ID should not be null");
    }

    @Then("the statement status should be {word}")
    public void theStatementStatusShouldBe(String expectedStatus) {
        assertEquals(expectedStatus, generateResponse.getStatementStatus());
    }

    @Then("the statement balance should be {double}")
    public void theStatementBalanceShouldBe(double expectedBalance) {
        RetrieveStatementResponseDTO stmt =
                statementService.getStatement(generatedStatementId);
        assertEquals(0, BigDecimal.valueOf(expectedBalance)
                        .compareTo(new BigDecimal(stmt.getStatementBalance())),
                "Statement balance mismatch. Expected: " + expectedBalance
                        + " but was: " + stmt.getStatementBalance());
    }

    @Then("the minimum due should be {double}")
    public void theMinimumDueShouldBe(double expectedMinDue) {
        RetrieveStatementResponseDTO stmt =
                statementService.getStatement(generatedStatementId);
        assertEquals(0, BigDecimal.valueOf(expectedMinDue)
                        .compareTo(new BigDecimal(stmt.getMinimumDue())),
                "Minimum due mismatch. Expected: " + expectedMinDue
                        + " but was: " + stmt.getMinimumDue());
    }

    @Then("the statement generation should fail with an error")
    public void theStatementGenerationShouldFailWithAnError() {
        assertNotNull(thrownException, "Expected an exception to be thrown");
        assertNull(generateResponse, "Generate response should be null on failure");
    }

    @Then("the retrieved statement status should be {word}")
    public void theRetrievedStatementStatusShouldBe(String expectedStatus) {
        assertNotNull(retrieveResponse, "Retrieved statement should not be null");
        assertEquals(expectedStatus, retrieveResponse.getStatementStatus());
    }

    @Then("the retrieved statement balance should be {double}")
    public void theRetrievedStatementBalanceShouldBe(double expectedBalance) {
        assertNotNull(retrieveResponse, "Retrieved statement should not be null");
        assertEquals(0, BigDecimal.valueOf(expectedBalance)
                .compareTo(new BigDecimal(retrieveResponse.getStatementBalance())));
    }

    @Then("the retrieved statement should contain the transactions list")
    public void theRetrievedStatementShouldContainTheTransactionsList() {
        assertNotNull(retrieveResponse.getTransactions(),
                "Transactions list should not be null");
    }

    @Then("the retrieved statement should contain the payments list")
    public void theRetrievedStatementShouldContainThePaymentsList() {
        assertNotNull(retrieveResponse.getPayments(),
                "Payments list should not be null");
    }

    @Then("the retrieval should fail with an error")
    public void theRetrievalShouldFailWithAnError() {
        assertNotNull(thrownException, "Expected an exception to be thrown");
        assertNull(retrieveResponse, "Retrieve response should be null on failure");
    }

    @Then("the carry forward balance should equal the statement balance of {double}")
    public void theCarryForwardBalanceShouldEqualTheStatementBalanceOf(double expected) {
        RetrieveStatementResponseDTO stmt =
                statementService.getStatement(generatedStatementId);
        assertEquals(0, BigDecimal.valueOf(expected)
                        .compareTo(new BigDecimal(stmt.getCarryForwardBalance())),
                "Carry forward balance mismatch. Expected: " + expected
                        + " but was: " + stmt.getCarryForwardBalance());
    }

    @Then("the remaining statement balance should be less than the statement balance")
    public void theRemainingStatementBalanceShouldBeLessThanTheStatementBalance() {
        RetrieveStatementResponseDTO stmt =
                statementService.getStatement(generatedStatementId);
        BigDecimal remaining = new BigDecimal(stmt.getRemainingStatementBalance());
        BigDecimal balance = new BigDecimal(stmt.getStatementBalance());
        assertTrue(remaining.compareTo(balance) < 0,
                "Remaining balance " + remaining
                        + " should be less than statement balance " + balance);
    }

// HELPER

    private Card buildCard(Customer owner, double creditLimit) {
        Card c = new Card();
        c.setCardId(UUID.randomUUID());
        c.setCustomer(owner);
        c.setCardNumber("4" + (long)(Math.random() * 900000000000000L + 100000000000000L));
        c.setCardType(Card.CardType.CREDIT);
        c.setCardHolderName("Jane Smith");
        c.setCardIssueDate(LocalDate.now().minusYears(1));
        c.setExpiryDate(LocalDate.now().plusYears(3));
        c.setActive(true);
        c.setCardBalance(BigDecimal.ZERO);
        c.setCashAdvanceBalance(BigDecimal.ZERO);
        c.setCreditLimit(BigDecimal.valueOf(creditLimit));
        c.setAvailableCredit(BigDecimal.valueOf(creditLimit));
        c.setAnnualInterestRate(new BigDecimal("0.20"));
        c.setCashAdvanceAPR(new BigDecimal("0.24"));
        c.setCashAdvanceFeeRate(new BigDecimal("0.02"));
        c.setLateFeeAmount(new BigDecimal("50.00"));
        c.setMinimumDue(new BigDecimal("100.00"));
        c.setCashAdvanceLimit(new BigDecimal("1000.00"));
        c.setAnnualMembershipFee(BigDecimal.ZERO);
        c.setSecurityCode("123");
        return c;
    }

}