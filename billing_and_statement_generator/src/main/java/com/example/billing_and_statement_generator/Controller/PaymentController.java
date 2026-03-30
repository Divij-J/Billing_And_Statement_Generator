package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.payment.GetPaymentHistoryRequestDTO;
import com.example.billing_and_statement_generator.dto.payment.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.payment.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.payment.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "Endpoints for processing payments and retrieving payment history")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/v1")
    @Operation(summary = "Process a payment (V1)",
            description = "Submit a payment against a billing cycle for a card")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(dto));
    }

    @PostMapping("/v1/history")
    @Operation(summary = "Get payment history (V1)",
            description = "Retrieve all payments made for a specific card")
    public ResponseEntity<List<RetrievePaymentHistoryDTO>> getPaymentHistory(
            @Valid @RequestBody GetPaymentHistoryRequestDTO dto) {
        return ResponseEntity.ok(
                paymentService.getPaymentHistory(UUID.fromString(dto.getCardId())));
    }
}