package com.example.billing_and_statement_generator.cucumber.stepdefs;

import com.example.billing_and_statement_generator.dto.payment.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.payment.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.entity.*;
import com.example.billing_and_statement_generator.repository.*;
import com.example.billing_and_statement_generator.services.PaymentService;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentStepDefinitions {

    @Autowired private PaymentService paymentService;
    @Autowired private CardRepository cardRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BillingCycleRepository billingCycleRepository;

    // Scenario-scoped state
    private Customer customer;
    private Card card;
    private BillingCycle billingCycle;
    private PaymentResponseDTO paymentResponse;
    private Exception thrownException;

    // -----------------------------------------------------------------------
    // BACKGROUND STEPS
    // -----------------------------------------------------------------------

    @Given("a payment customer exists in the system")
    public void aPaymentCustomerExistsInTheSystem() {
        customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("pay." + UUID.randomUUID() + "@example.com");
        customer.setPhoneNumber(generateUniquePhone());
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Main St");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");
        customer = customerRepository.save(customer);
        // reset scenario state
        paymentResponse = null;
        thrownException = null;
        billingCycle = null;
    }

    @Given("a payment card exists with a credit limit of {double}")
    public void aPaymentCardExistsWithACreditLimitOf(double creditLimit) {
        card = buildCard(customer, creditLimit);
        card = cardRepository.save(card);
    }

    @Given("the payment card has a balance of {double} and cash advance balance of {double}")
    public void thePaymentCardHasABalanceOfAndCashAdvanceBalanceOf(
            double cardBal, double cashBal) {
        card.setCardBalance(BigDecimal.valueOf(cardBal));
        card.setCashAdvanceBalance(BigDecimal.valueOf(cashBal));
        card.setAvailableCredit(card.getCreditLimit()
                .subtract(BigDecimal.valueOf(cardBal))
                .subtract(BigDecimal.valueOf(cashBal)));
        card = cardRepository.save(card);
    }

    @Given("the payment card has a minimum due of {double}")
    public void thePaymentCardHasAMinimumDueOf(double minimumDue) {
        card.setMinimumDue(BigDecimal.valueOf(minimumDue));
        card = cardRepository.save(card);
    }

    // -----------------------------------------------------------------------
    // WHEN STEPS
    // -----------------------------------------------------------------------

    @When("a payment of {double} is made using {word} method")
    public void aPaymentOfIsMadeUsingMethod(double amount, String method) {
        if (billingCycle == null) {
            createDefaultBillingCycle();
        }
        PaymentRequestDTO dto = PaymentRequestDTO.builder()
                .cardId(card.getCardId().toString())
                .amountPaid(String.valueOf(amount))
                .paymentMethod(method)
                .build();
        try {
            paymentResponse = paymentService.processPayment(dto);
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
            paymentResponse = null;
        }
    }

    // -----------------------------------------------------------------------
    // THEN STEPS
    // -----------------------------------------------------------------------

    @Then("the payment should be saved successfully")
    public void thePaymentShouldBeSavedSuccessfully() {
        assertNull(thrownException,
                "Expected no exception but got: " +
                        (thrownException != null ? thrownException.getMessage() : ""));
        assertNotNull(paymentResponse, "Payment response should not be null");
        assertNotNull(paymentResponse.getPaymentId(), "Payment ID should not be null");
    }

    @Then("the payment type should be {word}")
    public void thePaymentTypeShouldBe(String expectedType) {
        assertEquals(expectedType, paymentResponse.getPaymentType());
    }

    @Then("the payment status should be {word}")
    public void thePaymentStatusShouldBe(String expectedStatus) {
        assertEquals(expectedStatus, paymentResponse.getPaymentStatus());
    }

    @Then("the card total balance should be {double}")
    public void theCardTotalBalanceShouldBe(double expectedTotal) {
        Card updated = cardRepository.findById(card.getCardId()).orElseThrow();
        BigDecimal actual = updated.getCardBalance().add(updated.getCashAdvanceBalance());
        assertEquals(0, BigDecimal.valueOf(expectedTotal).compareTo(actual),
                "Card total balance mismatch. Expected: " + expectedTotal
                        + " but was: " + actual);
    }

    @Then("the payment should be rejected with an error")
    public void thePaymentShouldBeRejectedWithAnError() {
        assertNotNull(thrownException, "Expected an exception for overpayment");
        assertNull(paymentResponse, "Payment response should be null");
    }

    @Then("the cash advance balance should be {double}")
    public void theCashAdvanceBalanceShouldBe(double expected) {
        Card updated = cardRepository.findById(card.getCardId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(expected)
                        .compareTo(updated.getCashAdvanceBalance()),
                "Cash advance balance mismatch");
    }

    @Then("the card balance should be {double}")
    public void theCardBalanceShouldBe(double expected) {
        Card updated = cardRepository.findById(card.getCardId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(expected)
                        .compareTo(updated.getCardBalance()),
                "Card balance mismatch");
    }

    @Then("the payment history for the card should contain {int} payment")
    public void thePaymentHistoryForTheCardShouldContainPayment(int expectedCount) {
        List<RetrievePaymentHistoryDTO> history =
                paymentService.getPaymentHistory(card.getCardId());
        assertEquals(expectedCount, history.size(),
                "Payment history count mismatch");
    }

    @Then("the payment history should show amount of {double}")
    public void thePaymentHistoryShouldShowAmountOf(double expectedAmount) {
        List<RetrievePaymentHistoryDTO> history =
                paymentService.getPaymentHistory(card.getCardId());
        assertFalse(history.isEmpty(), "Payment history should not be empty");
        BigDecimal actual = new BigDecimal(history.get(0).getAmountPaid());
        assertEquals(0, BigDecimal.valueOf(expectedAmount).compareTo(actual),
                "Payment history amount mismatch");
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private void createDefaultBillingCycle() {
        BillingCycle cycle = new BillingCycle();
        cycle.setCycleId(UUID.randomUUID());
        cycle.setCard(card);
        cycle.setCycleStartDate(LocalDate.now().minusDays(30));
        cycle.setCycleEndDate(LocalDate.now());
        cycle.setDueDate(LocalDate.now().plusDays(21));
        cycle.setCreditLimit(card.getCreditLimit());
        cycle.setPreviousBalance(BigDecimal.ZERO);
        cycle.setTotalPurchases(card.getCardBalance());
        cycle.setTotalCashAdvance(card.getCashAdvanceBalance());
        cycle.setTotalInterest(BigDecimal.ZERO);
        cycle.setTotalOutstanding(
                card.getCardBalance().add(card.getCashAdvanceBalance()));
        cycle.setMinimumDue(card.getMinimumDue() != null
                ? card.getMinimumDue() : new BigDecimal("100.00"));
        cycle.setCycleStatus("OPEN");
        billingCycle = billingCycleRepository.save(cycle);
    }

    private Card buildCard(Customer owner, double creditLimit) {
        Card c = new Card();
        c.setCardId(UUID.randomUUID());
        c.setCustomer(owner);
        c.setCardNumber(generateUniqueCardNumber());
        c.setCardType(Card.CardType.CREDIT);
        c.setCardHolderName("John Doe");
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

    // Generates a unique 16-digit card number using UUID to avoid collisions
    private String generateUniqueCardNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "4" + uuid.substring(0, 15);
    }

    // Generates a unique phone number using current time nanos
    private String generateUniquePhone() {
        long nano = System.nanoTime() % 1_000_000_000L;
        return String.format("5%09d", Math.abs(nano));
    }
}