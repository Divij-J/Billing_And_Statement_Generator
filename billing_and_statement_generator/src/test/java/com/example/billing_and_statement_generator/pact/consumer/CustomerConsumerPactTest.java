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
 * CONSUMER PACT TEST — BillingService consuming CustomerService
 *
 * Defines the contract that BillingService expects from CustomerService.
 * Running this test generates pact JSON files under target/pacts/.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
        providerName = "CustomerServiceProvider",
        pactVersion = PactSpecVersion.V3
)
public class CustomerConsumerPactTest {

    private static final String CUSTOMER_ID =
            "11111111-2222-3333-4444-555555555555";

    // ---------------------------------------------------------------------
    // PACT 1: Create customer successfully
    // ---------------------------------------------------------------------

    @Pact(consumer = "BillingServiceConsumer", provider = "CustomerServiceProvider")
    public RequestResponsePact createCustomer(PactDslWithProvider builder) {
        return builder
                .given("customer does not exist")
                .uponReceiving("BillingService creates a customer")
                .path("/api/customers")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
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
                .willRespondWith()
                .status(201)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .uuid("customerId", UUID.fromString(CUSTOMER_ID))
                        .stringType("email", "jane.doe@test.com")
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createCustomer")
    void testCreateCustomer(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "email": "jane.doe@test.com",
                  "phoneNumber": "1234567890",
                  "phoneType": "MOBILE",
                  "address1": "123 Main St",
                  "city": "Chicago",
                  "state": "IL",
                  "zipcode": "60601"
                }
                """;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/customers",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("customerId"));
        assertTrue(response.getBody().contains("email"));
    }

    // ---------------------------------------------------------------------
    // PACT 2: Retrieve existing customer
    // ---------------------------------------------------------------------

    @Pact(consumer = "BillingServiceConsumer", provider = "CustomerServiceProvider")
    public RequestResponsePact getCustomerById(PactDslWithProvider builder) {
        return builder
                .given("customer exists with id")
                .uponReceiving("BillingService retrieves a customer by id")
                .path("/api/customers/" + CUSTOMER_ID)
                .method("GET")
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
                )
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getCustomerById")
    void testGetCustomerById(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                mockServer.getUrl() + "/api/customers/" + CUSTOMER_ID,
                HttpMethod.GET,
                null,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("firstName"));
        assertTrue(response.getBody().contains("email"));
    }

    // ---------------------------------------------------------------------
    // PACT 3: Customer not found
    // ---------------------------------------------------------------------

    @Pact(consumer = "BillingServiceConsumer", provider = "CustomerServiceProvider")
    public RequestResponsePact getCustomerNotFound(PactDslWithProvider builder) {
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
    @PactTestFor(pactMethod = "getCustomerNotFound")
    void testGetCustomerNotFound(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.exchange(
                    mockServer.getUrl() + "/api/customers/" + CUSTOMER_ID,
                    HttpMethod.GET,
                    null,
                    String.class
            );
            fail("Expected 404 when customer not found");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}