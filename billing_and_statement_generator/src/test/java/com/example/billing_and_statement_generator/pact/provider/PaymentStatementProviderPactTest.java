package com.example.billing_and_statement_generator.pact.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.example.billing_and_statement_generator.config.TestSecurityConfig;
import com.example.billing_and_statement_generator.entity.*;
import com.example.billing_and_statement_generator.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PROVIDER PACT TEST — PaymentService and StatementService as Providers
 *
 * Uses @Import(TestSecurityConfig.class) which permits all requests —
 * the same config already used in other controller tests in this project.
 * This is the cleanest way to bypass security in provider verification.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TestSecurityConfig.class)
@Provider("PaymentServiceProvider")
@PactFolder("src/test/resources/pacts")
@IgnoreNoPactsToVerify
public class PaymentStatementProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired private CardRepository cardRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BillingCycleRepository billingCycleRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private StatementRepository statementRepository;
    @Autowired private TransactionRepository transactionRepository;

    private static final UUID CARD_ID =
            UUID.fromString("d1e2f3a4-b5c6-7890-defa-bc1234567890");
    private static final UUID CYCLE_ID =
            UUID.fromString("e1f2a3b4-c5d6-7890-efab-cd1234567890");
    private static final UUID PAYMENT_ID =
            UUID.fromString("f1a2b3c4-d5e6-7890-fabc-de1234567890");
    private static final UUID STATEMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setupTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // ---------------------------------------------------------------------
    // PROVIDER STATES
    // ---------------------------------------------------------------------

    @State("payments exist for a card")
    @Transactional
    public void paymentsExistForACard() {
        cleanAll();
        Customer customer = buildAndSaveCustomer(
                "pay.provider@example.com", "5111222333");
        Card card = buildAndSaveCard(customer, CARD_ID,
                new BigDecimal("500.00"), BigDecimal.ZERO);
        BillingCycle cycle = buildAndSaveBillingCycle(card, CYCLE_ID);

        Payment payment = new Payment();
        payment.setPaymentId(PAYMENT_ID);
        payment.setCard(card);
        payment.setBillingCycle(cycle);
        payment.setAmountPaid(new BigDecimal("500.00"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentType(Payment.PaymentType.PARTIAL);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaymentMethod(Payment.PaymentMethod.ONLINE);
        paymentRepository.save(payment);
    }

    @State("no payments exist for a card")
    @Transactional
    public void noPaymentsExistForACard() {
        cleanAll();
        Customer customer = buildAndSaveCustomer(
                "no.pay@example.com", "5222333444");
        buildAndSaveCard(customer, CARD_ID,
                new BigDecimal("1000.00"), BigDecimal.ZERO);
    }

    @State("a card exists with balance")
    @Transactional
    public void aCardExistsWithBalance() {
        cleanAll();
        Customer customer = buildAndSaveCustomer(
                "balance@example.com", "5333444555");
        buildAndSaveCard(customer, CARD_ID,
                new BigDecimal("1000.00"), BigDecimal.ZERO);
    }

    @State("a card exists with id")
    @Transactional
    public void aCardExistsWithId() {
        cleanAll();
        Customer customer = buildAndSaveCustomer(
                "card.info@example.com", "5444555666");
        buildAndSaveCard(customer, CARD_ID,
                new BigDecimal("1000.00"), BigDecimal.ZERO);
    }

    @State("a card does not exist")
    @Transactional
    public void aCardDoesNotExist() {
        cleanAll();
    }

    @State("a statement exists for retrieval")
    @Transactional
    public void aStatementExistsForRetrieval() {
        cleanAll();
        Customer customer = buildAndSaveCustomer(
                "stmt.provider@example.com", "5555666777");
        Card card = buildAndSaveCard(customer, CARD_ID,
                new BigDecimal("1020.00"), BigDecimal.ZERO);
        BillingCycle cycle = buildAndSaveBillingCycle(card, CYCLE_ID);

        String snapshotJson = buildSnapshotJson();
        Statement statement = new Statement();
        statement.setStatementId(STATEMENT_ID);
        statement.setCard(card);
        statement.setBillingCycle(cycle);
        statement.setStatementDate(LocalDate.now());
        statement.setDueDate(LocalDate.now().plusDays(21));
        statement.setBillingStartDate(LocalDate.now().minusDays(30));
        statement.setBillingEndDate(LocalDate.now());
        statement.setStatementBalance(new BigDecimal("1020.00"));
        statement.setRemainingStatementBalance(new BigDecimal("1020.00"));
        statement.setMinimumDue(new BigDecimal("100.00"));
        statement.setTotalInterest(new BigDecimal("20.00"));
        statement.setTotalOutstanding(new BigDecimal("1020.00"));
        statement.setTotalFeeApplied(BigDecimal.ZERO);
        statement.setCashAdvanceFee(BigDecimal.ZERO);
        statement.setCarryForwardBalance(new BigDecimal("1020.00"));
        statement.setStatementStatus(Statement.StatementStatus.GENERATED);
        statement.setStatementSnapshot(snapshotJson);
        statementRepository.save(statement);
    }

    @State("a statement does not exist")
    @Transactional
    public void aStatementDoesNotExist() {
        cleanAll();
    }

    // ---------------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------------

    private void cleanAll() {
        statementRepository.deleteAll();
        paymentRepository.deleteAll();
        transactionRepository.deleteAll();
        billingCycleRepository.deleteAll();
        cardRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private Customer buildAndSaveCustomer(String email, String phone) {
        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer.setEmail(email);
        customer.setPhoneNumber(phone);
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Test St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");
        return customerRepository.save(customer);
    }

    private Card buildAndSaveCard(Customer customer, UUID cardId,
                                  BigDecimal cardBal, BigDecimal cashBal) {
        Card card = new Card();
        card.setCardId(cardId);
        card.setCustomer(customer);
        card.setCardNumber(
                "4" + (long)(Math.random() * 900000000000000L + 100000000000000L));
        card.setCardType(Card.CardType.CREDIT);
        card.setCardHolderName("Test User");
        card.setCardIssueDate(LocalDate.now().minusYears(1));
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setActive(true);
        card.setCardBalance(cardBal);
        card.setCashAdvanceBalance(cashBal);
        card.setCreditLimit(new BigDecimal("5000.00"));
        card.setAvailableCredit(
                new BigDecimal("5000.00").subtract(cardBal).subtract(cashBal));
        card.setAnnualInterestRate(new BigDecimal("0.20"));
        card.setCashAdvanceAPR(new BigDecimal("0.24"));
        card.setCashAdvanceFeeRate(new BigDecimal("0.02"));
        card.setLateFeeAmount(new BigDecimal("50.00"));
        card.setMinimumDue(new BigDecimal("100.00"));
        card.setCashAdvanceLimit(new BigDecimal("1000.00"));
        card.setAnnualMembershipFee(BigDecimal.ZERO);
        card.setSecurityCode("123");
        return cardRepository.save(card);
    }

    private BillingCycle buildAndSaveBillingCycle(Card card, UUID cycleId) {
        BillingCycle cycle = new BillingCycle();
        cycle.setCycleId(cycleId);
        cycle.setCard(card);
        cycle.setCycleStartDate(LocalDate.now().minusDays(30));
        cycle.setCycleEndDate(LocalDate.now());
        cycle.setDueDate(LocalDate.now().plusDays(21));
        cycle.setCreditLimit(new BigDecimal("5000.00"));
        cycle.setPreviousBalance(BigDecimal.ZERO);
        cycle.setTotalPurchases(new BigDecimal("1000.00"));
        cycle.setTotalCashAdvance(BigDecimal.ZERO);
        cycle.setTotalInterest(new BigDecimal("20.00"));
        cycle.setTotalOutstanding(new BigDecimal("1020.00"));
        cycle.setMinimumDue(new BigDecimal("100.00"));
        cycle.setCycleStatus("OPEN");
        return billingCycleRepository.save(cycle);
    }

    private String buildSnapshotJson() {
        return "{" +
                "\"statementId\":\"" + STATEMENT_ID + "\"," +
                "\"cycleId\":\"" + CYCLE_ID + "\"," +
                "\"cardId\":\"" + CARD_ID + "\"," +
                "\"statementDate\":\"" + LocalDate.now() + "\"," +
                "\"dueDate\":\"" + LocalDate.now().plusDays(21) + "\"," +
                "\"billingStartDate\":\"" + LocalDate.now().minusDays(30) + "\"," +
                "\"billingEndDate\":\"" + LocalDate.now() + "\"," +
                "\"statementBalance\":\"1020.00\"," +
                "\"remainingStatementBalance\":\"1020.00\"," +
                "\"minimumDue\":\"100.00\"," +
                "\"totalInterest\":\"20.00\"," +
                "\"totalOutstanding\":\"1020.00\"," +
                "\"totalFeeApplied\":\"0.00\"," +
                "\"cashAdvanceFee\":\"0.00\"," +
                "\"carryForwardBalance\":\"1020.00\"," +
                "\"amountPaid\":\"0.00\"," +
                "\"availableCredit\":\"3980.00\"," +
                "\"statementStatus\":\"GENERATED\"," +
                "\"transactions\":[]," +
                "\"payments\":[]" +
                "}";
    }
}