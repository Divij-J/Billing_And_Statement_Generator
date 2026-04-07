package com.example.billing_and_statement_generator.repository;

import com.example.billing_and_statement_generator.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    private Payment createTestPayment(String email, String phone,
                                      BigDecimal amount,
                                      Payment.PaymentType type,
                                      Payment.PaymentStatus status) {

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail(email);
        customer.setPhoneNumber(phone);
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Main St");
        customer.setCity("Toledo");
        customer.setState("OH");
        customer.setZipcode("43601");
        customerRepository.save(customer);

        Card card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCustomer(customer);
        card.setCardNumber("4539578763621486");
        card.setCardType(Card.CardType.CREDIT);
        card.setCardHolderName("John Doe");
        card.setCardIssueDate(LocalDate.now().minusYears(1));
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setActive(true);
        card.setCardBalance(BigDecimal.ZERO);
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        card.setAvailableCredit(new BigDecimal("5000.00"));
        card.setCreditLimit(new BigDecimal("5000.00"));
        card.setAnnualInterestRate(new BigDecimal("0.24"));
        card.setBillingCycleDate(LocalDate.now());
        card.setLateFeeAmount(new BigDecimal("50.00"));
        card.setCashAdvanceFeeRate(new BigDecimal("0.02"));
        card.setSecurityCode("123");
        card.setAnnualMembershipFee(new BigDecimal("0.00"));
        card.setCashAdvanceLimit(new BigDecimal("1000.00"));
        cardRepository.save(card);

        BillingCycle cycle = new BillingCycle();
        cycle.setCycleId(UUID.randomUUID());
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
        billingCycleRepository.save(cycle);

        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setCard(card);
        payment.setBillingCycle(cycle);
        payment.setAmountPaid(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentType(type);
        payment.setPaymentStatus(status);
        payment.setPaymentMethod(Payment.PaymentMethod.ONLINE);
        return paymentRepository.save(payment);
    }

    @Test
    void givenPayment_whenSaved_thenCanBeFoundById() {
        Payment payment = createTestPayment(
                "test1@test.com", "1111111111",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        Payment found = paymentRepository
                .findById(payment.getPaymentId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getAmountPaid())
                .isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(found.getPaymentType())
                .isEqualTo(Payment.PaymentType.PARTIAL);
        assertThat(found.getPaymentMethod())
                .isEqualTo(Payment.PaymentMethod.ONLINE);
    }

    @Test
    void givenPayments_whenFindByCardIdCalled_thenReturnsAllPaymentsForCard() {
        Payment p1 = createTestPayment(
                "test2@test.com", "2222222222",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        List<Payment> payments = paymentRepository
                .findByCardId(p1.getCard().getCardId());

        assertThat(payments).isNotNull();
        assertThat(payments).hasSize(1);
    }

    @Test
    void givenPayments_whenFindByCycleIdCalled_thenReturnsAllPaymentsForCycle() {
        Payment p1 = createTestPayment(
                "test3@test.com", "3333333333",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        List<Payment> payments = paymentRepository
                .findByCycleId(p1.getBillingCycle().getCycleId());

        assertThat(payments).isNotNull();
        assertThat(payments).hasSize(1);
    }

    @Test
    void givenPayments_whenFindByCardIdAndCycleIdCalled_thenReturnsCorrectPayments() {
        Payment p1 = createTestPayment(
                "test4@test.com", "4444444444",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        List<Payment> payments = paymentRepository.findByCardIdAndCycleId(
                p1.getCard().getCardId(),
                p1.getBillingCycle().getCycleId());

        assertThat(payments).isNotNull();
        assertThat(payments).hasSize(1);
    }

    @Test
    void givenPayments_whenFindByPaymentStatusCalled_thenReturnsCorrectPayments() {
        createTestPayment(
                "test5@test.com", "5555555555",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        List<Payment> successPayments = paymentRepository
                .findByPaymentStatus(Payment.PaymentStatus.SUCCESS);

        assertThat(successPayments).isNotNull();
        assertThat(successPayments).isNotEmpty();
        assertThat(successPayments).allSatisfy(p ->
                assertThat(p.getPaymentStatus())
                        .isEqualTo(Payment.PaymentStatus.SUCCESS));
    }

    @Test
    void givenSuccessPayments_whenFindTotalPaidByCycleIdCalled_thenReturnsTotalAmount() {
        Payment p1 = createTestPayment(
                "test6@test.com", "6666666666",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        BigDecimal total = paymentRepository
                .findTotalPaidByCycleId(p1.getBillingCycle().getCycleId());

        assertThat(total).isNotNull();
        assertThat(total).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void givenNoPayments_whenFindByPaymentStatusCalled_thenReturnsEmptyList() {
        List<Payment> pending = paymentRepository
                .findByPaymentStatus(Payment.PaymentStatus.PENDING);

        assertThat(pending).isEmpty();
    }

    @Test
    void givenPayment_whenDeleted_thenCannotBeFoundById() {
        Payment payment = createTestPayment(
                "test7@test.com", "7777777777",
                new BigDecimal("500.00"),
                Payment.PaymentType.FULL,
                Payment.PaymentStatus.SUCCESS);

        UUID paymentId = payment.getPaymentId();
        paymentRepository.deleteById(paymentId);

        Payment found = paymentRepository.findById(paymentId).orElse(null);
        assertThat(found).isNull();
    }

    @Test
    void givenPaymentsWithinCycle_whenFindPaymentsWithinCycleCalled_thenReturnsCorrectPayments() {
        Payment payment = createTestPayment(
                "test8@test.com", "8888888888",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        List<Payment> payments = paymentRepository.findPaymentsWithinCycle(
                payment.getCard().getCardId(),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1));

        assertThat(payments).isNotNull();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmountPaid())
                .isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void givenNoPaymentsWithinCycle_whenFindPaymentsWithinCycleCalled_thenReturnsEmptyList() {
        Payment payment = createTestPayment(
                "test9@test.com", "9999999999",
                new BigDecimal("500.00"),
                Payment.PaymentType.PARTIAL,
                Payment.PaymentStatus.SUCCESS);

        List<Payment> payments = paymentRepository.findPaymentsWithinCycle(
                payment.getCard().getCardId(),
                LocalDate.now().minusDays(60),
                LocalDate.now().minusDays(31));

        assertThat(payments).isNotNull();
        assertThat(payments).isEmpty();
    }
}