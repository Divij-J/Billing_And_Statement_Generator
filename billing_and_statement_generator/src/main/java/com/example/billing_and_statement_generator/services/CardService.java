package com.example.billing_and_statement_generator.services;

import com.example.billing_and_statement_generator.dto.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;
import com.example.billing_and_statement_generator.entity.Customer;
import com.example.billing_and_statement_generator.mapper.CardMapper;
import com.example.billing_and_statement_generator.repository.CardRepository;
import com.example.billing_and_statement_generator.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        // Server-managed fields (Card ID, Card isActive, Card Balance)
        card.setCardId(UUID.randomUUID());
        card.setActive(true);
        card.setCardBalance(BigDecimal.ZERO);

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

        if (newBalance.compareTo(card.getCreditLimit()) > 0) {
            throw new LimitExceededException("Credit limit exceeded");
        }

        card.setCardBalance(newBalance);
        cardRepository.save(card);
        log.debug("Purchase posted: cardId={}, amount={}, prevBalance={}, newBalance={}", cardId, amount, prevBalance, newBalance);
        return newBalance;
    }

    /** Cash Advance
     * Apply a cash advance amount.
     * Enforces cash advance limit for the single advance.
     * (Cash advance fees/interest can be handled by Transactions/Billing modules.)
     */
    @Transactional
    public BigDecimal applyCashAdvance(UUID cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Cash advance amount must be > 0");
        }

        Card card = loadCard(cardId);
        assertCardActive(card); // checks if card is active

        if (card.getCashAdvanceLimit() != null && amount.compareTo(card.getCashAdvanceLimit()) > 0) {
            throw new LimitExceededException("Cash advance exceeds per-advance limit");
        }

        BigDecimal prevBalance = card.getCardBalance(); // used for logging
        BigDecimal newBalance = card.getCardBalance().add(amount);

        // Ensures credit limit is not exceeded
        if (newBalance.compareTo(card.getCreditLimit()) > 0) {
            throw new LimitExceededException("Credit limit exceeded after cash advance");
        }

        card.setCardBalance(newBalance);
        cardRepository.save(card);
        log.debug("Cash advance posted: cardId={}, amount={}, prevBalance={} ,newBalance={}", cardId, amount, prevBalance, newBalance);
        return newBalance;
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

        BigDecimal prevBalance = card.getCardBalance();

        // Currently set to reject overpayments
        if(amount.compareTo(prevBalance) > 0) {
            throw new ValidationException("Payment amount exceeds current balance");
        }

        BigDecimal newBalance = card.getCardBalance().subtract(amount);

        card.setCardBalance(newBalance);
        cardRepository.save(card);

        log.debug("Payment posted: cardId={}, amount={}, prevBalance={}, newBalance={}", cardId, amount, prevBalance, newBalance);
        return newBalance;
    }

    // Reads this card's balance
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID cardId) {
        return loadCard(cardId).getCardBalance();
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