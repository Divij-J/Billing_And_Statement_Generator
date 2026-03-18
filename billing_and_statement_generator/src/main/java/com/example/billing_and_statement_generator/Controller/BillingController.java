package com.example.billing_and_statement_generator.Controller;

import com.example.billing_and_statement_generator.dto.BillingCycleResponseDTO;
import com.example.billing_and_statement_generator.services.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/billing")
public class BillingController {

  private final BillingService billingService;

  @PostMapping("/generate/{cardId}")
  public ResponseEntity<BillingCycleResponseDTO> generateBillingCycle(
      @PathVariable UUID cardId) {
    log.info("/api/billing/generate/{} - request received", cardId);

    BillingCycleResponseDTO response =
      billingService.generateBillingCycle(cardId);

    log.info("/api/billing/generate/{} - cycle {} generated",
                      cardId, response.getCycleId());
    return ResponseEntity.status(201).body(response);
  }

  @GetMapping("/{cardId}/{cycleId}")
  public ResponseEntity<BillingCycleResponseDTO> getBillingCycle(
      @PathVariable UUID cardId,
      @PathVariable UUID cycleId) {
    log.info("/api/billing/{}/{} - request received", cardId, cycleId);

    BillingCycleResponseDTO response =
      billingService.getBillingCycle(cardId, cycleId);

    log.info("/api/billing/{}/{} - successfully retrieved",
                      cardId, cycleId);
    return ResponseEntity.ok(response);
  }
}