package com.example.billing_and_statement_generator.controller;

import com.example.billing_and_statement_generator.Controller.CustomerController;
import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerIdDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.dto.UpdateCustomerRequestDTO;
import com.example.billing_and_statement_generator.services.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false) // disable Spring Security
class CustomerControllerBDDTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateCustomer() throws Exception {
        // GIVEN
        UUID customerId = UUID.randomUUID();

        CreateCustomerRequestDTO request = new CreateCustomerRequestDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@test.com");
        request.setPhoneNumber("1234567890");
        request.setPhoneType("MOBILE");
        request.setAddress1("123 Main St");
        request.setCity("Chicago");
        request.setState("IL");
        request.setZipcode("60616");

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(customerId);
        response.setEmail("john.doe@test.com");

        given(customerService.createCustomer(any(CreateCustomerRequestDTO.class)))
                .willReturn(response);

        // WHEN / THEN
        mockMvc.perform(post("/api/customers/createCustomer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId")
                        .value(customerId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("john.doe@test.com"));

        then(customerService).should()
                .createCustomer(any(CreateCustomerRequestDTO.class));
    }

    @Test
    void shouldGetCustomerById() throws Exception {
        // GIVEN
        UUID customerId = UUID.randomUUID();

        CustomerIdDTO request = new CustomerIdDTO();
        request.setCustomerId(customerId);

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(customerId);
        response.setEmail("john.doe@test.com");

        given(customerService.getCustomer(customerId))
                .willReturn(response);

        // WHEN / THEN
        mockMvc.perform(post("/api/customers/getCustomerById")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId")
                        .value(customerId.toString()));

        then(customerService).should().getCustomer(customerId);
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        // GIVEN
        UUID customerId = UUID.randomUUID();

        CreateCustomerRequestDTO updateData = new CreateCustomerRequestDTO();
        updateData.setFirstName("Jane");
        updateData.setLastName("Doe");
        updateData.setEmail("jane.doe@test.com");
        updateData.setPhoneNumber("9876543210");
        updateData.setPhoneType("MOBILE");
        updateData.setAddress1("456 Oak Street");
        updateData.setCity("Chicago");
        updateData.setState("IL");
        updateData.setZipcode("60616");

        UpdateCustomerRequestDTO request = new UpdateCustomerRequestDTO();
        request.setCustomerId(customerId);
        request.setUpdateData(updateData);

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(customerId);
        response.setEmail("jane.doe@test.com");

        given(customerService.updateCustomer(
                eq(customerId), any(CreateCustomerRequestDTO.class)))
                .willReturn(response);

        // WHEN / THEN
        mockMvc.perform(put("/api/customers/updateCustomer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId")
                        .value(customerId.toString()))
                .andExpect(jsonPath("$.email")
                        .value("jane.doe@test.com"));

        then(customerService)
                .should().updateCustomer(eq(customerId),
                        any(CreateCustomerRequestDTO.class));
    }
}

