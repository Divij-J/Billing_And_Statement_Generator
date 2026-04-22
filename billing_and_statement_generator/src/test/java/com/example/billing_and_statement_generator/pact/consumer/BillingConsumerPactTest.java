package com.example.billing_and_statement_generator.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
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
 * CONSUMER PACT TEST — StatementService consuming BillingService
 *
 * Defines the contract that StatementService expects from BillingService.
 * Pact files generated under target/pacts/.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "BillingServiceProvider",
        pactVersion = PactSpecVersion.V3
)
public class BillingConsumerPactTest {

    private static final String CARD_ID =
            "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String CYCLE_ID =
            "11111111-2222-3333-4444-555555555555";

    // ---------------------------------------------------------------------
    // PACT 1: Generate billing cycle successfully
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "BillingServiceProvider")
    public RequestResponsePact generateBillingCycle(PactDslWithProvider builder) {
        return builder
                .given("a card exists with transactions")
                .uponReceiving("StatementService generates a billing cycle")
                .path("/api/billing/generate/" + CARD_ID)
                .method("POST")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("cycleId", UUID.fromString(CYCLE_ID))
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .stringType("cycleStatus", "OPEN")
                        .decimalType("totalPurchases", 0.00)
                        .decimalType("totalCashAdvance", 20.00)
                        .decimalType("totalInterest", 0.00)
                        .decimalType("cashAdvanceFee", 10.00)
                        .decimalType("feesApplied", 10.00)
                        .decimalType("totalOutstanding", 30.00)
                        .decimalType("minimumDue", 100.00)
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generateBillingCycle")
    void testGenerateBillingCycle(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/billing/generate/" + CARD_ID,
                HttpMethod.POST,
                null,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cycleId"));
        assertTrue(response.getBody().contains("totalOutstanding"));
        assertTrue(response.getBody().contains("minimumDue"));
    }

    // ---------------------------------------------------------------------
    // PACT 2: Retrieve billing cycle by id
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "BillingServiceProvider")
    public RequestResponsePact getBillingCycleById(PactDslWithProvider builder) {
        return builder
                .given("a billing cycle exists")
                .uponReceiving("StatementService retrieves billing cycle by id")
                .path("/api/billing/" + CARD_ID + "/" + CYCLE_ID)
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("cycleId", UUID.fromString(CYCLE_ID))
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .decimalType("totalOutstanding", 30.00)
                        .decimalType("cashAdvanceFee", 10.00)
                        .stringType("cycleStatus", "OPEN")
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getBillingCycleById")
    void testGetBillingCycleById(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/billing/" + CARD_ID + "/" + CYCLE_ID,
                HttpMethod.GET,
                null,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cycleStatus"));
    }

    // ---------------------------------------------------------------------
    // PACT 3: Billing cycle not found
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "BillingServiceProvider")
    public RequestResponsePact billingCycleNotFound(PactDslWithProvider builder) {
        return builder
                .given("billing cycle does not exist")
                .uponReceiving("StatementService requests non-existent billing cycle")
                .path("/api/billing/" + CARD_ID + "/" + CYCLE_ID)
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "billingCycleNotFound")
    void testBillingCycleNotFound(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.exchange(
                    mockServer.getUrl() + "/api/billing/" + CARD_ID + "/" + CYCLE_ID,
                    HttpMethod.GET,
                    null,
                    String.class
            );
            fail("Expected 404 when billing cycle not found");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}