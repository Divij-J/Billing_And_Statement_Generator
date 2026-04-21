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

 - CONSUMER PACT TEST — StatementService consuming PaymentService

 - Defines the contract that StatementService expects from PaymentService
 - when retrieving payment history for a billing cycle.

 - Note: Separated from StatementConsumerPactTest because each Pact consumer
 - test class can only target ONE provider via class-level @PactTestFor.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "PaymentServiceProvider", pactVersion = PactSpecVersion.V3)
public class StatementToPaymentConsumerPactTest {

    private static final String CARD_ID = "d1e2f3a4-b5c6-7890-defa-bc1234567890";
    private static final String CYCLE_ID = "e1f2a3b4-c5d6-7890-efab-cd1234567890";
    private static final String PAYMENT_ID = "f1a2b3c4-d5e6-7890-fabc-de1234567890";

    // ---------------------------------------------------------------------
    // PACT 1: Get payment history — payments exist
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "PaymentServiceProvider")
    public RequestResponsePact getPaymentHistoryForStatement(PactDslWithProvider builder) {
        return builder
                .given("payments exist for a card")
                .uponReceiving("StatementService requests payment history")
                .path("/payments/v1/history")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .stringType("cardId", CARD_ID))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonArray()
                        .object()
                        .uuid("paymentId", UUID.fromString(PAYMENT_ID))
                        .stringType("cardId", CARD_ID)
                        .stringType("cycleId", CYCLE_ID)
                        .stringType("amountPaid", "500.00")
                        .stringType("paymentType", "PARTIAL")
                        .stringType("paymentStatus", "SUCCESS")
                        .stringType("paymentMethod", "ONLINE")
                        .closeObject())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getPaymentHistoryForStatement")
    void testGetPaymentHistoryForStatement(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/payments/v1/history",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("paymentId"));
        assertTrue(response.getBody().contains("amountPaid"));
        assertTrue(response.getBody().contains("paymentStatus"));
    }

    // ---------------------------------------------------------------------
    // PACT 2: Get payment history — no payments exist
    // ---------------------------------------------------------------------

    @Pact(consumer = "StatementServiceConsumer", provider = "PaymentServiceProvider")
    public RequestResponsePact getEmptyPaymentHistoryForStatement(PactDslWithProvider builder) {
        return builder
                .given("no payments exist for a card")
                .uponReceiving("StatementService requests payment history when none exist")
                .path("/payments/v1/history")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .stringType("cardId", CARD_ID))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body("[]")
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getEmptyPaymentHistoryForStatement")
    void testGetEmptyPaymentHistoryForStatement(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/payments/v1/history",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }
}