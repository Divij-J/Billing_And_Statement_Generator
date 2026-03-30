package com.example.billing_and_statement_generator.dto;

import com.example.billing_and_statement_generator.entity.Card.CardType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCardRequestDTO {
    @NotNull
    private UUID customerId;

    @NotBlank
    @Size(min = 12, max = 19)
    @Pattern(regexp = "^[0-9]+$", message = "cardNumber must be digits only")
    private String cardNumber;

    @NotNull
    private CardType cardType;

    @NotBlank
    private String cardHolderName;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "^[0-9]+$", message = "securityCode must be digits only")
    private String securityCode;
}
