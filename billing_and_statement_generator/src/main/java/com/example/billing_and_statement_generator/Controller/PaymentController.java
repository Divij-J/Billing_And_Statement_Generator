package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentRequestV1DTO;
import com.example.billing_and_statement_generator.dto.v1.PaymentResponseV1DTO;
import com.example.billing_and_statement_generator.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @PostMapping
    @Operation(summary = "Process a payment",
            description = "Submit a payment against a billing cycle for a card",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(dto));
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Get payment history",
            description = "Retrieve all payments made for a specific card",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<RetrievePaymentHistoryDTO>> getPaymentHistory(
            @PathVariable UUID cardId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(cardId));
    }

    @PostMapping("/v1")
    @Operation(summary = "Process a payment (V1)",
            description = "Submit a payment with optional payment method field",
            tags = {"Payment Controller V1"},
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponseV1DTO> processPaymentV1(
            @Valid @RequestBody PaymentRequestV1DTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPaymentV1(dto));
    }

    @GetMapping("/v1/{cardId}")
    @Operation(summary = "Get payment history (V1)",
            description = "Retrieve all payments made for a specific card",
            tags = {"Payment Controller V1"},
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<RetrievePaymentHistoryDTO>> getPaymentHistoryV1(
            @PathVariable UUID cardId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(cardId));
    }
}