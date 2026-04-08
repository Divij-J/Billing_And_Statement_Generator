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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class StatementRepositoryTest {

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    private Statement createTestStatement(String email, String phone) {

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
        card.setCreditLimit(new BigDecimal("5000.00"));
        card.setAvailableCredit(new BigDecimal("5000.00"));
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

        Statement statement = new Statement();
        statement.setStatementId(UUID.randomUUID());
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
        statement.setTotalFeeApplied(new BigDecimal("50.00"));
        statement.setCashAdvanceFee(new BigDecimal("20.00"));
        statement.setCarryForwardBalance(new BigDecimal("1020.00"));
        statement.setStatementStatus(Statement.StatementStatus.GENERATED);
        return statementRepository.save(statement);
    }

    @Test
    void givenStatement_whenSaved_thenCanBeFoundById() {
        Statement statement = createTestStatement(
                "test1@test.com", "1111111111");

        Optional<Statement> found = statementRepository
                .findById(statement.getStatementId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatementBalance())
                .isEqualByComparingTo(new BigDecimal("1020.00"));
        assertThat(found.get().getStatementStatus())
                .isEqualTo(Statement.StatementStatus.GENERATED);
    }

    @Test
    void givenStatement_whenFindByCycleIdCalled_thenReturnsStatement() {
        Statement statement = createTestStatement(
                "test2@test.com", "2222222222");

        Optional<Statement> found = statementRepository
                .findByCycleId(statement.getBillingCycle().getCycleId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatementId())
                .isEqualTo(statement.getStatementId());
    }

    @Test
    void givenStatement_whenExistsByCycleIdCalled_thenReturnsTrue() {
        Statement statement = createTestStatement(
                "test3@test.com", "3333333333");

        boolean exists = statementRepository
                .existsByCycleId(statement.getBillingCycle().getCycleId());

        assertThat(exists).isTrue();
    }

    @Test
    void givenNoStatement_whenExistsByCycleIdCalled_thenReturnsFalse() {
        boolean exists = statementRepository
                .existsByCycleId(UUID.randomUUID());

        assertThat(exists).isFalse();
    }

    @Test
    void givenStatement_whenFindByStatementStatusCalled_thenReturnsCorrectStatements() {
        createTestStatement("test4@test.com", "4444444444");

        List<Statement> statements = statementRepository
                .findByStatementStatus(Statement.StatementStatus.GENERATED);

        assertThat(statements).isNotNull();
        assertThat(statements).isNotEmpty();
        assertThat(statements).allSatisfy(s ->
                assertThat(s.getStatementStatus())
                        .isEqualTo(Statement.StatementStatus.GENERATED));
    }

    @Test
    void givenNoStatements_whenFindByStatementStatusCalled_thenReturnsEmptyList() {
        List<Statement> statements = statementRepository
                .findByStatementStatus(Statement.StatementStatus.PAID);

        assertThat(statements).isEmpty();
    }

    @Test
    void givenStatement_whenFindByCardIdAndStatementStatusCalled_thenReturnsCorrectStatements() {
        Statement statement = createTestStatement(
                "test5@test.com", "5555555555");

        List<Statement> statements = statementRepository
                .findByCardIdAndStatementStatus(
                        statement.getCard().getCardId(),
                        Statement.StatementStatus.GENERATED);

        assertThat(statements).isNotNull();
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).getStatementStatus())
                .isEqualTo(Statement.StatementStatus.GENERATED);
    }

    @Test
    void givenStatement_whenDeleted_thenCannotBeFoundById() {
        Statement statement = createTestStatement(
                "test6@test.com", "6666666666");

        UUID statementId = statement.getStatementId();
        statementRepository.deleteById(statementId);

        Optional<Statement> found = statementRepository.findById(statementId);
        assertThat(found).isEmpty();
    }
}