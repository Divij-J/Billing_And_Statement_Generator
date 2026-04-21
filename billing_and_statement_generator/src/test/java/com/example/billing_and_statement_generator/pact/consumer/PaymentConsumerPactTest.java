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

 - CONSUMER PACT TEST — PaymentService consuming CardService

 - Defines the contract that PaymentService expects from CardService.
 - Running this test generates pact JSON files in build/pacts/
 - Share these with Person 1 (Card owner) for provider verification.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "CardServiceProvider", pactVersion = PactSpecVersion.V3)
public class PaymentConsumerPactTest {

    private static final String CARD_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    // ---------------------------------------------------------------------
    // PACT 1: Get card balance — card exists
    // ---------------------------------------------------------------------

    @Pact(consumer = "PaymentServiceConsumer", provider = "CardServiceProvider")
    public RequestResponsePact getCardBalanceForPayment(PactDslWithProvider builder) {
        return builder
                .given("a card exists with balance")
                .uponReceiving("PaymentService requests card balance")
                .path("/api/cards/v1/getCardBalanceByCardId")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID)))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .decimalType("cardBalance", 1000.00)
                        .decimalType("cashAdvanceBalance", 0.00)
                        .decimalType("totalBalance", 1000.00)
                        .decimalType("availableCredit", 4000.00))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getCardBalanceForPayment")
    void testGetCardBalanceForPayment(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/cards/v1/getCardBalanceByCardId",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("cardBalance"));
        assertTrue(response.getBody().contains("totalBalance"));
    }

    // ---------------------------------------------------------------------
    // PACT 2: Get card info — card exists
    // ---------------------------------------------------------------------

    @Pact(consumer = "PaymentServiceConsumer", provider = "CardServiceProvider")
    public RequestResponsePact getCardInfoForPayment(PactDslWithProvider builder) {
        return builder
                .given("a card exists with id")
                .uponReceiving("PaymentService requests card info")
                .path("/api/cards/v1/getCardInfo")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID)))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID))
                        .decimalType("cardBalance", 1000.00)
                        .decimalType("cashAdvanceBalance", 0.00)
                        .decimalType("creditLimit", 5000.00)
                        .decimalType("availableCredit", 4000.00)
                        .decimalType("minimumDue", 100.00)
                        .booleanType("active", true))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getCardInfoForPayment")
    void testGetCardInfoForPayment(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/cards/v1/getCardInfo",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("creditLimit"));
        assertTrue(response.getBody().contains("minimumDue"));
    }

    // ---------------------------------------------------------------------
    // PACT 3: Get card info — card not found
    // ---------------------------------------------------------------------

    @Pact(consumer = "PaymentServiceConsumer", provider = "CardServiceProvider")
    public RequestResponsePact getCardNotFound(PactDslWithProvider builder) {
        return builder
                .given("a card does not exist")
                .uponReceiving("PaymentService requests a non-existent card")
                .path("/api/cards/v1/getCardInfo")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .uuid("cardId", UUID.fromString(CARD_ID)))
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getCardNotFound")
    void testGetCardNotFound(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"cardId\": \"" + CARD_ID + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    mockServer.getUrl() + "/api/cards/v1/getCardInfo",
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