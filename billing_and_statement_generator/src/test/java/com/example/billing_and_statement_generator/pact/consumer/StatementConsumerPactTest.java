package com.example.billing_and_statement_generator.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**

 - CONSUMER PACT TEST — StatementService consuming BillingService

 - Defines the contract that StatementService expects from BillingService.
 - Running this test generates pact JSON files in build/pacts/
 - Share these with Person 2 (Billing owner) for provider verification.

 - Note: Each class can only have ONE @PactTestFor providerName at class level.
 - StatementService consuming PaymentService is in StatementToPaymentConsumerPactTest.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "BillingServiceProvider", pactVersion = PactSpecVersion.V3)
public class StatementConsumerPactTest {

    private static final String CARD_ID = "b1c2d3e4-f5a6-7890-bcde-fa1234567890";
    private static final String CYCLE_ID = "c1d2e3f4-a5b6-7890-cdef-ab1234567890";

    // ---------------------------------------------------------------------
    // PACT 1: Get billing cycle — cycle exists
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "BillingServiceProvider")
    public RequestResponsePact getBillingCycleForStatement(PactDslWithProvider builder) {
        return builder
                .given("a billing cycle exists for the card")
                .uponReceiving("StatementService requests billing cycle details")
                .path("/api/billing/v1/GetBillingCycleByCardAndCycleId")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .uuid("cycleId", UUID.fromString(CYCLE_ID)))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("cycleId", UUID.fromString(CYCLE_ID))
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .decimalType("totalOutstanding", 1020.00)
                        .decimalType("totalInterest", 20.00)
                        .decimalType("minimumDue", 100.00)
                        .decimalType("totalPurchases", 1000.00)
                        .decimalType("totalCashAdvance", 0.00)
                        .decimalType("feesApplied", 0.00)
                        .stringType("cycleStatus", "OPEN")
                        .date("cycleStartDate", "yyyy-MM-dd")
                        .date("cycleEndDate", "yyyy-MM-dd")
                        .date("dueDate", "yyyy-MM-dd"))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getBillingCycleForStatement")
    void testGetBillingCycleForStatement(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\", \"cycleId\": \"" + CYCLE_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/billing/v1/GetBillingCycleByCardAndCycleId",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cycleId"));
        assertTrue(response.getBody().contains("totalOutstanding"));
        assertTrue(response.getBody().contains("minimumDue"));
    }

    // ---------------------------------------------------------------------
    // PACT 2: Get billing cycle — cycle not found
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "BillingServiceProvider")
    public RequestResponsePact getBillingCycleNotFound(PactDslWithProvider builder) {
        return builder
                .given("a billing cycle does not exist")
                .uponReceiving("StatementService requests a non-existent billing cycle")
                .path("/api/billing/v1/GetBillingCycleByCardAndCycleId")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .uuid("cycleId", UUID.fromString(CYCLE_ID)))
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getBillingCycleNotFound")
    void testGetBillingCycleNotFound(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\", \"cycleId\": \"" + CYCLE_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    mockServer.getUrl() + "/api/billing/v1/GetBillingCycleByCardAndCycleId",
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            fail("Expected 404 exception");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}