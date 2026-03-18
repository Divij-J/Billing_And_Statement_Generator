package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.PaymentRequestDTO;
import com.example.billing_and_statement_generator.dto.PaymentResponseDTO;
import com.example.billing_and_statement_generator.dto.RetrievePaymentHistoryDTO;
import com.example.billing_and_statement_generator.services.PaymentService;
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
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(dto));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<List<RetrievePaymentHistoryDTO>> getPaymentHistory(
            @PathVariable UUID cardId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(cardId));
    }
}