package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.config.TestSecurityConfig;
import com.example.billing_and_statement_generator.dto.GenerateStatementRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.GetPaymentHistoryRequestDTO;
import com.example.billing_and_statement_generator.entity.*;
import com.example.billing_and_statement_generator.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PaymentStatementMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private StatementRepository statementRepository;

    private Card testCard;
    private BillingCycle testBillingCycle;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("mockmvc@test.com");
        customer.setPhoneNumber("9999999999");
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Main St");
        customer.setCity("Toledo");
        customer.setState("OH");
        customer.setZipcode("43601");
        customerRepository.save(customer);

        testCard = new Card();
        testCard.setCardId(UUID.randomUUID());
        testCard.setCustomer(customer);
        testCard.setCardNumber("4539578763621486");
        testCard.setCardType(Card.CardType.CREDIT);
        testCard.setCardHolderName("John Doe");
        testCard.setCardIssueDate(LocalDate.now().minusYears(1));
        testCard.setExpiryDate(LocalDate.now().plusYears(3));
        testCard.setActive(true);
        testCard.setCardBalance(BigDecimal.ZERO);
        testCard.setCashAdvanceBalance(BigDecimal.ZERO);
        testCard.setCreditLimit(new BigDecimal("5000.00"));
        testCard.setAnnualInterestRate(new BigDecimal("0.24"));
        testCard.setBillingCycleDate(LocalDate.now());
        testCard.setLateFeeAmount(new BigDecimal("50.00"));
        testCard.setCashAdvanceFeeRate(new BigDecimal("0.02"));
        testCard.setSecurityCode("123");
        testCard.setAnnualMembershipFee(new BigDecimal("0.00"));
        testCard.setCashAdvanceLimit(new BigDecimal("1000.00"));
        cardRepository.save(testCard);

        testBillingCycle = new BillingCycle();
        testBillingCycle.setCycleId(UUID.randomUUID());
        testBillingCycle.setCard(testCard);
        testBillingCycle.setCycleStartDate(LocalDate.now().minusDays(30));
        testBillingCycle.setCycleEndDate(LocalDate.now());
        testBillingCycle.setDueDate(LocalDate.now().plusDays(21));
        testBillingCycle.setCreditLimit(new BigDecimal("5000.00"));
        testBillingCycle.setPreviousBalance(BigDecimal.ZERO);
        testBillingCycle.setTotalPurchases(new BigDecimal("1000.00"));
        testBillingCycle.setTotalCashAdvance(BigDecimal.ZERO);
        testBillingCycle.setTotalInterest(new BigDecimal("20.00"));
        testBillingCycle.setTotalOutstanding(new BigDecimal("1020.00"));
        testBillingCycle.setMinimumDue(new BigDecimal("100.00"));
        testBillingCycle.setCycleStatus("OPEN");
        billingCycleRepository.save(testBillingCycle);

        testCard.setCardBalance(new BigDecimal("1000.00"));
        cardRepository.save(testCard);
    }

    //Payment MockMvc tests

    @Test
    void shouldProcessPayment_GivenValidRequest() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .cycleId(testBillingCycle.getCycleId().toString())
                .amountPaid("500.00")
                .paymentType("PARTIAL")
                .paymentMethod("ONLINE")
                .build();

        mockMvc.perform(post("/payments/v1")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountPaid").value("500.00"))
                .andExpect(jsonPath("$.paymentType").value("PARTIAL"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentMethod").value("ONLINE"));
    }

    @Test
    void shouldProcessPaymentWithMinimumType_GivenValidRequest() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .cycleId(testBillingCycle.getCycleId().toString())
                .amountPaid("300.00")
                .paymentType("MINIMUM")
                .paymentMethod("BANK_TRANSFER")
                .build();

        mockMvc.perform(post("/payments/v1")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"));
    }

    @Test
    void shouldGetPaymentHistory_GivenValidCardId() throws Exception {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setCard(testCard);
        payment.setBillingCycle(testBillingCycle);
        payment.setAmountPaid(new BigDecimal("500.00"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentType(Payment.PaymentType.PARTIAL);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaymentMethod(Payment.PaymentMethod.ONLINE);
        paymentRepository.save(payment);

        GetPaymentHistoryRequestDTO historyRequest = GetPaymentHistoryRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .build();

        mockMvc.perform(post("/payments/v1/history")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(historyRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequest_WhenPaymentRequestIsMissingFields() throws Exception {
        PaymentRequestDTO invalidRequest = PaymentRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .build();

        mockMvc.perform(post("/payments/v1")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    //Statement MockMvc tests

    @Test
    void shouldGenerateStatement_GivenValidRequest() throws Exception {
        GenerateStatementRequestDTO request = GenerateStatementRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .cycleId(testBillingCycle.getCycleId().toString())
                .build();

        mockMvc.perform(post("/statements/v1/generate")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statementStatus").value("GENERATED"))
                .andExpect(jsonPath("$.message")
                        .value("Statement generated successfully"));
    }

    @Test
    void shouldGetStatement_GivenValidCardAndCycleId() throws Exception {
        Statement statement = new Statement();
        statement.setStatementId(UUID.randomUUID());
        statement.setCard(testCard);
        statement.setBillingCycle(testBillingCycle);
        statement.setStatementDate(LocalDate.now());
        statement.setDueDate(LocalDate.now().plusDays(21));
        statement.setBillingStartDate(LocalDate.now().minusDays(30));
        statement.setBillingEndDate(LocalDate.now());
        statement.setStatementBalance(new BigDecimal("1020.00"));
        statement.setRemainingStatementBalance(new BigDecimal("1020.00"));
        statement.setMinimumDue(new BigDecimal("100.00"));
        statement.setTotalInterest(new BigDecimal("20.00"));
        statement.setTotalOutstanding(new BigDecimal("1020.00"));
        statement.setTotalFeeApplied(new BigDecimal("50.00"));
        statement.setCashAdvanceFee(new BigDecimal("20.00"));
        statement.setCarryForwardBalance(new BigDecimal("1020.00"));
        statement.setStatementStatus(Statement.StatementStatus.GENERATED);
        statementRepository.save(statement);

        GenerateStatementRequestDTO getRequest = GenerateStatementRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .cycleId(testBillingCycle.getCycleId().toString())
                .build();

        mockMvc.perform(post("/statements/v1/get")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(getRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementStatus").value("GENERATED"));
    }

    @Test
    void shouldReturnBadRequest_WhenStatementRequestIsMissingFields() throws Exception {
        GenerateStatementRequestDTO invalidRequest = GenerateStatementRequestDTO.builder()
                .cardId(testCard.getCardId().toString())
                .build();

        mockMvc.perform(post("/statements/v1/generate")
                        .with(jwt().jwt(builder -> builder.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}