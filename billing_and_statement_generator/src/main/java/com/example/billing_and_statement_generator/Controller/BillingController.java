package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.dto.v1.BillingCycleResponseV1DTO;
import com.example.billing_and_statement_generator.services.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

  // Original — combined feesApplied in response
          @PostMapping("/generate/{cardId}")
  @Operation(
    summary = "Generate billing cycle for a card",
                security = @SecurityRequirement(name = "bearerAuth")
  )
          public ResponseEntity<BillingCycleResponseDTO> generateBillingCycle(
      @PathVariable UUID cardId) {
    log.info("POST /api/billing/generate/{} - request received", cardId);
    BillingCycleResponseDTO response =
      billingService.generateBillingCycle(cardId);
    log.info("POST /api/billing/generate/{} - cycle {} generated",
                      cardId, response.getCycleId());
    return ResponseEntity.status(201).body(response);
  }

  // V1 — fee breakdown shown separately (lateFee, cashAdvanceFee, annualMembershipFee)
          @PostMapping("/v1/generate/{cardId}")
  @Operation(
    summary = "Generate billing cycle V1 — includes fee breakdown",
                security = @SecurityRequirement(name = "bearerAuth"),
                tags = {"billing-controller-V1"}
  )
          public ResponseEntity<BillingCycleResponseV1DTO> generateBillingCycleV1(
      @PathVariable UUID cardId) {
    log.info("POST /api/billing/v1/generate/{} - V1 request received", cardId);
    BillingCycleResponseV1DTO response =
      billingService.generateBillingCycleV1(cardId);
    log.info("POST /api/billing/v1/generate/{} - cycle {} generated V1",
                      cardId, response.getCycleId());
    return ResponseEntity.status(201).body(response);
  }

    // Original POST
    @PostMapping("/{cardId}/{cycleId}")
    @Operation(
            summary = "Get billing cycle by card and cycle ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<BillingCycleResponseDTO> getBillingCycle(
            @PathVariable UUID cardId,
            @PathVariable UUID cycleId) {
        log.info("POST /api/billing/{}/{} - request received", cardId, cycleId);
        BillingCycleResponseDTO response =
                billingService.getBillingCycle(cardId, cycleId);
        log.info("POST /api/billing/{}/{} - successfully retrieved",
                cardId, cycleId);
        return ResponseEntity.ok(response);
    }

    // V1 POST — returns fee breakdown
    @PostMapping("/v1/{cardId}/{cycleId}")
    @Operation(
            summary = "Get billing cycle V1 — includes fee breakdown",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"billing-controller-V1"}
    )
    public ResponseEntity<BillingCycleResponseV1DTO> getBillingCycleV1(
            @PathVariable UUID cardId,
            @PathVariable UUID cycleId) {
        log.info("POST /api/billing/v1/{}/{} - V1 request received",
                cardId, cycleId);
        BillingCycleResponseV1DTO response =
                billingService.getBillingCycleV1(cardId, cycleId);
        log.info("POST /api/billing/v1/{}/{} - successfully retrieved V1",
                cardId, cycleId);
        return ResponseEntity.ok(response);
    }
}