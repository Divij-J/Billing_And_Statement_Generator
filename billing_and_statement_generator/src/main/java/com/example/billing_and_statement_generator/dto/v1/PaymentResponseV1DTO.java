package com.example.billing_and_statement_generator.dto.v1;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseV1DTO {

    private String paymentId;
    private String cycleId;
    private String cardId;
    private String amountPaid;
    private String paymentDate;
    private String paymentType;
    private String paymentStatus;
    private String paymentMethod;
}
