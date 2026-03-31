package com.example.billing_and_statement_generator.dto.card;

import com.example.billing_and_statement_generator.entity.Card.CardType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCardRequestDTO {
    @NotNull(message = "Card ID is required")
    private UUID customerId;

    @NotBlank(message = "Card Number is required")
    @Size(min = 12, max = 19)
    @Pattern(regexp = "^[0-9]+$", message = "cardNumber must be digits only")
    private String cardNumber;

    @NotNull(message = "Card Type is required")
    private CardType cardType;

    @NotBlank(message = "Card Holder Name is required")
    private String cardHolderName;

    @NotBlank(message = "Security Code is required")
    @Size(min = 3, max = 4)
    @Pattern(regexp = "^[0-9]+$", message = "securityCode must be digits only")
    private String securityCode;
}
