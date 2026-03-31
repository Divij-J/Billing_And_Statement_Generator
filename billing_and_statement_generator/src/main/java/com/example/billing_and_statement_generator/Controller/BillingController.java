package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.dto.card.CardIdDTO;
import com.example.billing_and_statement_generator.dto.transaction.GetTransactionsByCardIdCycleIdRequestDTO;
import com.example.billing_and_statement_generator.services.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "APIs for generating and retrieving billing cycles")
public class BillingController {

  private final BillingService billingService;

  // V1 — fee breakdown shown separately (lateFee, cashAdvanceFee, annualMembershipFee)
          @PostMapping("/v1/GenerateBillingCycleFeeBreakdown")
  @Operation(
    summary = "Generate billing cycle V1 — includes fee breakdown",
          description = "Generates a billing cycle and returns fee details broken down by type"
  )
          public ResponseEntity<BillingCycleResponseDTO> generateBillingCycle(
      @Valid @RequestBody CardIdDTO cardId) {
    log.info("POST /api/billing/v1/GenerateBillingCycleFeeBreakdown - request received for cardId={}", cardId.getCardId());
    BillingCycleResponseDTO response =
      billingService.generateBillingCycle(cardId.getCardId());
    log.info("POST /api/billing/v1/GenerateBillingCycleFeeBreakdown - cycle {} generated", response.getCycleId());
    return ResponseEntity.status(201).body(response);
  }

    // V1 POST — returns fee breakdown
    @PostMapping("/v1/GetBillingCycleByCardAndCycleId")
    @Operation(
            summary = "Get billing cycle V1 — includes fee breakdown",
            description = "Get billing cycle by card and cycle ID"
    )
    public ResponseEntity<BillingCycleResponseDTO> getBillingCycleV1(
            @Valid @RequestBody GetTransactionsByCardIdCycleIdRequestDTO request) {

        UUID cardId = request.getCardId();
        UUID cycleId = request.getCycleId();

        log.info("POST /api/billing/v1/GetBillingCycleByCardAndCycleId - request received for cardId={} and cycleId={}",
                cardId, cycleId);
        BillingCycleResponseDTO response =
                billingService.getBillingCycle(cardId, cycleId);
        log.info("POST /api/billing/v1/GetBillingCycleByCardAndCycleId - successfully retrieved cycle={}",
                cycleId);
        return ResponseEntity.ok(response);
    }
}