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
 *
 * CONSUMER PACT TEST — BillingService consuming CustomerService
 *
 * Defines the contract that BillingService expects from CustomerService
 * when validating and retrieving customer information.
 *
 * Note: Separate class because each consumer test targets ONE provider.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "CustomerServiceProvider",
        pactVersion = PactSpecVersion.V3
)
public class BillingToCustomerConsumerPactTest {

    private static final String CUSTOMER_ID =
            "a1111111-b222-c333-d444-e55555555555";

    // ---------------------------------------------------------------------
    // PACT 1: Retrieve customer — customer exists
    // ---------------------------------------------------------------------

//    @Pact(consumer = "BillingServiceConsumer", provider = "CustomerServiceProvider")
//    public RequestResponsePact getCustomerForBilling(PactDslWithProvider builder) {
//        return builder
//                .given("customer exists")
//                .uponReceiving("BillingService retrieves customer details")
//                .path("/api/customers/" + CUSTOMER_ID)
//                .method("GET")
//                .willRespondWith()
//                .status(200)
//                .headers(Map.of("Content-Type", "application/json"))
//                .body(new PactDslJsonBody()
//                        .uuid("customerId", UUID.fromString(CUSTOMER_ID))
//                        .stringType("firstName", "Jane")
//                        .stringType("lastName", "Doe")
//                        .stringType("email", "jane.doe@test.com")
//                        .stringType("phoneNumber", "1234567890")
//                        .stringType("phoneType", "MOBILE")
//                        .stringType("address1", "123 Main St")
//                        .stringType("city", "Chicago")
//                        .stringType("state", "IL")
//                        .stringType("zipcode", "60601")
//                )
//                .toPact();
//    }

    @Pact(consumer = "BillingServiceConsumer", provider = "CustomerServiceProvider")
    public RequestResponsePact getCustomerForBilling(PactDslWithProvider builder) {
        return builder
                .given("customer exists")
                .uponReceiving("BillingService retrieves customer details")
                .path("/api/customers/getCustomerById")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                        .uuid("customerId", UUID.fromString(CUSTOMER_ID)))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("customerId", UUID.fromString(CUSTOMER_ID))
                        .stringType("firstName", "Jane")
                        .stringType("lastName", "Doe")
                        .stringType("email", "jane.doe@test.com")
                        .stringType("phoneNumber", "1234567890")
                        .stringType("phoneType", "MOBILE")
                        .stringType("address1", "123 Main St")
                        .stringType("city", "Chicago")
                        .stringType("state", "IL")
                        .stringType("zipcode", "60601")
                )
                .toPact();
    }

//    @Test
//    @PactTestFor(pactMethod = "getCustomerForBilling")
//    void testGetCustomerForBilling(MockServer mockServer) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        ResponseEntity<String> response = restTemplate.exchange(
//                mockServer.getUrl() + "/api/customers/" + CUSTOMER_ID,
//                HttpMethod.GET,
//                null,
//                String.class
//        );
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().contains("customerId"));
//        assertTrue(response.getBody().contains("email"));
//    }

    @Test
    @PactTestFor(pactMethod = "getCustomerForBilling")
    void testGetCustomerForBilling(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{ \"customerId\": \"" + CUSTOMER_ID + "\" }";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/customers/getCustomerById",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("customerId"));
        assertTrue(response.getBody().contains("email"));
    }

    // ---------------------------------------------------------------------
    // PACT 2: Customer not found
    // ---------------------------------------------------------------------

    @Pact(consumer = "BillingServiceConsumer", provider = "CustomerServiceProvider")
    public RequestResponsePact customerNotFoundForBilling(PactDslWithProvider builder) {
        return builder
                .given("customer does not exist")
                .uponReceiving("BillingService retrieves a non-existent customer")
                .path("/api/customers/" + CUSTOMER_ID)
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "customerNotFoundForBilling")
    void testCustomerNotFoundForBilling(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.exchange(
                    mockServer.getUrl() + "/api/customers/" + CUSTOMER_ID,
                    HttpMethod.GET,
                    null,
                    String.class
            );
            fail("Expected 404 when customer is not found");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}