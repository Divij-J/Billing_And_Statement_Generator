package com.example.billing_and_statement_generator.dto.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetCardBalanceResponseDTO {
    UUID cardId;
    BigDecimal cardBalance;
    BigDecimal cashAdvanceBalance;
    BigDecimal totalBalance;
    BigDecimal availableCredit;
}
