package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.card.CreateCardRequestDTO;
import com.example.billing_and_statement_generator.dto.card.CreateCardResponseDTO;
import com.example.billing_and_statement_generator.entity.Card;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CardMapperTest {

    private final CardMapper mapper = Mappers.getMapper(CardMapper.class);

    @Test
    void testCardDTOtoCardEntityMapping() {
        // Create Card Request
        CreateCardRequestDTO dto = CreateCardRequestDTO.builder()
                .cardNumber("4111111111111111")
                .cardType(Card.CardType.CREDIT)
                .cardHolderName("Jane Doe")
                .securityCode("123")
                .build();

        // Create card according to DTO
        Card entity = mapper.toEntity(dto);

        // Assert Mapped Fields
        assertEquals(entity.getCardNumber(), dto.getCardNumber(), "Card number does not match Card DTO Request");
        assertEquals(entity.getCardHolderName(), dto.getCardHolderName(), "Card Holder Name does not match Card DTO Request");
        assertEquals(entity.getCardType(), dto.getCardType(), "Card Type does not match Card DTO Request");
        assertEquals(entity.getSecurityCode(), dto.getSecurityCode(),"Card Security Code does not match Card DTO Security Code");
    }

    @Test
    void testCardEntityToCardDTOResponseMappings() {
        // Create card entity
        Card card = Card.builder()
                .cardId(UUID.randomUUID())
                .cardNumber("4111111111111111")
                .cardType(Card.CardType.CREDIT)
                .cardHolderName("John Smith")
                .active(true)
                .cardBalance(BigDecimal.valueOf(123.45))
                .cashAdvanceBalance(BigDecimal.valueOf(50.00))
                .creditLimit(BigDecimal.valueOf(1000))
                .annualInterestRate(BigDecimal.valueOf(0.05))
                .cashAdvanceAPR(BigDecimal.valueOf(0.3))
                .billingCycleDate(LocalDate.now())
                .lateFeeAmount(BigDecimal.valueOf(50))
                .cashAdvanceFeeRate(BigDecimal.valueOf(0.02))
                .annualMembershipFee(BigDecimal.valueOf(50))
                .cashAdvanceLimit(BigDecimal.valueOf(500))
                .minimumDue(BigDecimal.valueOf(2))
                .build();

        // Translate card to Response DTO
        CreateCardResponseDTO dto = mapper.toResponse(card);

        // Assert
        assertEquals(dto.getCardId(), card.getCardId(), "Card DTO Card ID does not match Card Entity Card ID");
        assertEquals(dto.getCardNumber(), card.getCardNumber(), "Card DTO Card Number does not match Card Entity Card Number");
        assertEquals(dto.getCardHolderName(), card.getCardHolderName(), "Card DTO Card Holder Name does not match Card Entity Card Holder Name");
        assertEquals(dto.getCardType(), card.getCardType(), "Card DTO Card Type does not match Card Entity Card Type");
        assertEquals(dto.getCardIssueDate(), card.getCardIssueDate(), "Card DTO Card Issue Date does not match Card Entity Card Issue Date");
        assertTrue(dto.isActive(), "Card DTO Active does not match Card Entity Active");
        assertEquals(dto.getCardBalance(), card.getCardBalance(), "Card DTO Card Balance does not match Card Entity Card Balance");
        assertEquals(dto.getCashAdvanceBalance(), card.getCashAdvanceBalance(), "Card DTO Cash Advance Balance does not match Card Entity Cash Advance Balance");
        assertEquals(dto.getAnnualInterestRate(), card.getAnnualInterestRate(), "Card DTO Annual Interest Rate does not match Card Entity Annual Interest Rate");
        assertEquals(dto.getCashAdvanceAPR(), card.getCashAdvanceAPR(), "Card DTO Cash Advance APR does not match Card Entity Cash Advance APR");
        assertEquals(dto.getBillingCycleDate(), card.getBillingCycleDate(), "Card DTO Billing Cycle Date does not match Card Entity Billing Cycle Date");
        assertEquals(dto.getLateFeeAmount(), card.getLateFeeAmount(), "Card DTO Late Fee Amount does not match Card Entity Late Fee Amount");
        assertEquals(dto.getCashAdvanceFeeRate(), card.getCashAdvanceFeeRate(), "Card DTO Cash Advance Fee Rate does not match Card Entity Cash Advance Fee Rate");
        assertEquals(dto.getAnnualMembershipFee(), card.getAnnualMembershipFee(), "Card DTO Annual Membership Fee does not match Card Entity Annual Membership Fee");
        assertEquals(dto.getCashAdvanceLimit(), card.getCashAdvanceLimit(), "Card DTO Cash Advance Limit does not match Card Entity Cash Advance Limit");
        assertEquals(dto.getMinimumDue(), card.getMinimumDue(), "Card DTO Minimum Due does not match Card Entity Minimum Due");
    }
}