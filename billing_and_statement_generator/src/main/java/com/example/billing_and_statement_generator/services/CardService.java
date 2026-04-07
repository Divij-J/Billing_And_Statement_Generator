package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.card.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.card.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.dto.card.GetCardBalanceResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.mapper.CardMapper;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.CustomerRepository;
import com.example.billing_and_statement_generator.services.TransactionService.InterestType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final CardMapper cardMapper;

    // Create card method
    @Transactional
    public CreateCardResponseDTO create(CreateCardRequestDTO dto) {
        // Validates that the customer exists
        UUID customerId = dto.getCustomerId();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        // Enforces a unique card number when creating a card
        if (cardRepository.existsByCardNumber(dto.getCardNumber())) {
            throw new ConflictException("Card number already exists");
        }

        // Map DTO to Entity (mapper creates shallow Customer; we replace with managed one)
        Card card = cardMapper.toEntity(dto);
        card.setCustomer(customer);

        // Server-managed fields (Card Info and Balances/Fees/Interest Rates)
        card.setCardId(UUID.randomUUID());
        card.setActive(true);
        card.setCardIssueDate(LocalDate.now());
        card.setExpiryDate(LocalDate.now().plusYears(4));

        //Set interest rates and fees
        card.setAnnualInterestRate(BigDecimal.valueOf(0.2));
        card.setCashAdvanceAPR(BigDecimal.valueOf(0.24));
        card.setCashAdvanceFeeRate(BigDecimal.valueOf(0.02));
        card.setLateFeeAmount(BigDecimal.valueOf(50));
        card.setAnnualMembershipFee(BigDecimal.valueOf(100));

        // Set balances + minimum due
        card.setCardBalance(BigDecimal.ZERO);
        card.setCashAdvanceBalance(BigDecimal.ZERO);
        card.setMinimumDue(BigDecimal.ZERO);

        // Set credit limits (default values, can be changed later)
        card.setCreditLimit(BigDecimal.valueOf(10000));
        card.setCashAdvanceLimit(BigDecimal.valueOf(2500));
        card.setAvailableCredit(BigDecimal.valueOf(10000));

        Card saved = cardRepository.save(card);

        log.info("Created card: cardId={}, customerId={}", saved.getCardId(), customerId);
        return cardMapper.toResponse(saved);
    }

    // Reads card from the Card ID and returns Card info
    @Transactional(readOnly = true)
    public CreateCardResponseDTO getById(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
        return cardMapper.toResponse(card);
    }

    // Reads and lists cards owned by customer ID
    @Transactional(readOnly = true)
    public List<CreateCardResponseDTO> getByCustomer(UUID customerId) {
        return cardRepository.findByCustomerCustomerId(customerId).stream()
                .map(cardMapper::toResponse)
                .toList();
    }

    // Balance operations (Transaction, Cash Advance, Payment)
    /** Transactions/Purchases
     * Apply a purchase amount to the card balance.
     * Enforces credit limit: (balance + amount) <= creditLimit.
     * Returns the new balance.
     */
    @Transactional
    public BigDecimal applyPurchase(UUID cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Purchase amount must be > 0");
        }

        Card card = loadCard(cardId);
        assertCardActive(card); // checks if card is active

        BigDecimal prevBalance = card.getCardBalance(); // used for logging
        BigDecimal newBalance = card.getCardBalance().add(amount);

        BigDecimal totalBalance = newBalance.add(card.getCashAdvanceBalance()); // Checking if card balance + cash advance balance are still under credit limit

        if (totalBalance.compareTo(card.getCreditLimit()) > 0) {
            throw new LimitExceededException("Credit limit exceeded");
        }

        card.setCardBalance(newBalance);
        adjustAvailableCredit(card);

        cardRepository.save(card);
        log.debug("Purchase posted: cardId={}, amount={}, prevBalance={}, newBalance={}", cardId, amount, prevBalance, newBalance);
        return newBalance;
    }

    /** Cash Advance
     * Apply a cash advance amount.
     * Enforces cash advance limit for the single advance.
     * Cash advance fees/interest handled by Transactions/Billing
     */
    @Transactional
    public BigDecimal applyCashAdvance(UUID cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Cash advance amount must be > 0");
        }

        Card card = loadCard(cardId);
        assertCardActive(card); // checks if card is active

        // Checks if the amount exceeds the cash advance limit
        if (card.getCashAdvanceLimit() != null && amount.compareTo(card.getCashAdvanceLimit()) > 0) {
            throw new LimitExceededException("Cash advance exceeds per-advance limit");
        }

        // Add amount to Cash Advance Balance
        BigDecimal prevCashAdvanceBalance = card.getCashAdvanceBalance();
        BigDecimal newCashAdvanceBalance = prevCashAdvanceBalance.add(amount);

        BigDecimal totalBalance = card.getCardBalance().add(newCashAdvanceBalance);

        // Ensures credit limit is not exceeded
        if (totalBalance.compareTo(card.getCreditLimit()) > 0) {
            throw new LimitExceededException("Credit limit exceeded after cash advance");
        }
        else if(newCashAdvanceBalance.compareTo(card.getCashAdvanceLimit())>0){
            throw new LimitExceededException("Cash advance limit exceeded after cash advance");
        }

        card.setCashAdvanceBalance(newCashAdvanceBalance);
        adjustAvailableCredit(card);

        cardRepository.save(card);
        log.debug("Cash advance posted: cardId={}, amount={}, prevCashAdvanceBalance={}, newCashAdvanceBalance={}", cardId, amount, prevCashAdvanceBalance, newCashAdvanceBalance);
        return newCashAdvanceBalance;
    }

    /** Payments
     * Apply a payment to reduce the balance
     * Prevents negative balances
     */
    @Transactional
    public BigDecimal applyPayment(UUID cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Payment amount must be > 0");
        }

        Card card = loadCard(cardId);

        BigDecimal prevCardBalance = card.getCardBalance();
        BigDecimal prevCashAdvanceBalance = card.getCashAdvanceBalance();

        BigDecimal newCardBalance = prevCardBalance;
        BigDecimal newCashAdvanceBalance = prevCashAdvanceBalance;

        BigDecimal totalPrevBalance = prevCardBalance.add(prevCashAdvanceBalance);
        // Currently set to reject overpayments
        if(amount.compareTo(totalPrevBalance) > 0) {
            throw new LimitExceededException("Payment amount exceeds current balance");
        }

        BigDecimal remainder = amount;

        // Subtracts amount from the Cash Advance Balance, then subtracts from the regular card balance
        if(prevCashAdvanceBalance.signum() > 0){
            BigDecimal min = prevCashAdvanceBalance.min(remainder); // chooses the lesser of the two values
            newCashAdvanceBalance = prevCashAdvanceBalance.subtract(min);
            remainder = remainder.subtract(min);
        }
        if(remainder.signum() > 0){
            newCardBalance = prevCardBalance.subtract(remainder);
        }

        card.setCardBalance(newCardBalance);
        card.setCashAdvanceBalance(newCashAdvanceBalance);
        adjustAvailableCredit(card);
        cardRepository.save(card);

        log.debug("CardService: Payment posted: cardId={}, amount={}, prevCardBalance={}, newCardBalance={}, prevCashAdvanceBalance={}, newCashAdvanceBalance={}", cardId, amount, prevCardBalance, newCardBalance, prevCashAdvanceBalance, newCashAdvanceBalance);

        return newCardBalance.add(newCashAdvanceBalance);
    }

    /** Interest
     * Apply an interest amount to a balance (cardBalance or cashAdvanceBalance)
     */
    @Transactional
    public BigDecimal applyInterest(UUID cardId, BigDecimal amount, InterestType type){
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Interest amount must be > 0");
        }

        Card card = loadCard(cardId);
        BigDecimal prevBalance = card.getCardBalance();

        BigDecimal newBalance;

        if(type == InterestType.CARDBALANCE) {
            newBalance = card.getCardBalance().add(amount);
            card.setCardBalance(newBalance);
        }
        else if(type == InterestType.CASHADVANCE){
            newBalance = card.getCashAdvanceBalance().add(amount);
            card.setCashAdvanceBalance(newBalance);
        }
        else{
            throw new ValidationException("Invalid Interest Type");
        }

        adjustAvailableCredit(card);
        cardRepository.save(card);

        log.debug("CardService: Interest applied: cardId={}, amount={}, prevBalance={}, newBalance={}, interestType={}", cardId, amount, prevBalance, newBalance, type);
        return newBalance;
    }

    /** Fee
     * Applies fee to the card balance
     */
    @Transactional
    public BigDecimal applyFee(UUID cardId, BigDecimal amount){
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Purchase amount must be > 0");
        }

        Card card = loadCard(cardId);
        assertCardActive(card); // checks if card is active

        BigDecimal prevBalance = card.getCardBalance(); // used for logging
        BigDecimal newBalance = card.getCardBalance().add(amount);

        BigDecimal totalBalance = newBalance.add(card.getCashAdvanceBalance());

        if (totalBalance.compareTo(card.getCreditLimit()) > 0) {
            throw new LimitExceededException("Credit limit exceeded");
        }

        card.setCardBalance(newBalance);
        adjustAvailableCredit(card);
        cardRepository.save(card);
        log.debug("CardService: Fee applied: cardId={}, amount={}, prevBalance={}, newBalance={}", cardId, amount, prevBalance, newBalance);
        return newBalance;
    }

    // Adjust card's minimum due (calculated from Billing Cycle)
    @Transactional
    public void setMinimumDue(UUID cardId, BigDecimal amount){
        Card card = loadCard(cardId);
        card.setMinimumDue(amount);
        log.debug("CardService: Minimum due adjusted to {}", amount);
    }

    // Adjust card's due date (calculated from Billing Cycle)
    @Transactional
    public void setDueDate(UUID cardId, LocalDate date){
        Card card = loadCard(cardId);
        card.setBillingCycleDate(date);
        log.debug("CardService: Billing Date adjusted to {}", date);
    }

    // Adjusts cards available credit
    public void adjustAvailableCredit(Card card){
        BigDecimal cardBalance = card.getCardBalance();
        BigDecimal cashAdvanceBalance = card.getCashAdvanceBalance();
        BigDecimal creditLimit = card.getCreditLimit();

        card.setAvailableCredit(creditLimit.subtract(cardBalance).subtract(cashAdvanceBalance));
    }

    // Reads this card's balance
    @Transactional(readOnly = true)
    public GetCardBalanceResponseDTO getBalances(UUID cardId) {
        Card card = loadCard(cardId);

        GetCardBalanceResponseDTO dto = new GetCardBalanceResponseDTO();
        dto.setCardId(cardId);
        dto.setCardBalance(card.getCardBalance());
        dto.setCashAdvanceBalance(card.getCashAdvanceBalance());
        dto.setAvailableCredit(card.getAvailableCredit());

        BigDecimal total = card.getCardBalance().add(card.getCashAdvanceBalance());
        dto.setTotalBalance(total);
        return dto;
    }

    // Helper method to check if card exists, otherwise loads the Card's info
    private Card loadCard(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));
    }

    // Checks if card is active
    private void assertCardActive(Card card) {
        if (!card.isActive()) {
            throw new ValidationException("Card is inactive");
        }
    }


    // Exceptions
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String msg) { super(msg); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String msg) { super(msg); }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String msg) { super(msg); }
    }

    public static class LimitExceededException extends RuntimeException {
        public LimitExceededException(String msg) { super(msg); }
    }
}