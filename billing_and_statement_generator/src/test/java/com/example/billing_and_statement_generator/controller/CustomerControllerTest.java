package com.example.billing_and_statement_generator.controller;
import com.example.billing_and_statement_generator.Controller.CustomerController;
import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.services.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // TEST: POST /api/customers
    // -------------------------------------------------------------------------
    @WithMockUser
    @Test
    void createCustomer_shouldReturn201AndResponseDTO() throws Exception {

        CreateCustomerRequestDTO request = new CreateCustomerRequestDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setMiddleInitial("A");
        request.setEmail("john@example.com");
        request.setPhoneNumber("1234567890");
        request.setPhoneType("MOBILE");
        request.setAddress1("123 Test St");
        request.setCity("Chicago");
        request.setState("IL");
        request.setZipcode("60601");

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(UUID.randomUUID());
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setMiddleInitial("A");
        response.setEmail("john@example.com");

        Mockito.when(customerService.createCustomer(any(CreateCustomerRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .with(csrf())        // ✅ REQUIRED FOR POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(response.getCustomerId().toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    // -------------------------------------------------------------------------
    // TEST: GET /api/customers/{id}
    // -------------------------------------------------------------------------
    @WithMockUser
    @Test
    void getCustomer_shouldReturn200AndCustomer() throws Exception {
        UUID id = UUID.randomUUID();

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setCustomerId(id);
        response.setFirstName("Jane");
        response.setLastName("Smith");
        response.setEmail("jane@example.com");

        Mockito.when(customerService.getCustomer(id)).thenReturn(response);

        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    // -------------------------------------------------------------------------
    // TEST: PUT /api/customers/{id}
    // -------------------------------------------------------------------------
    @WithMockUser
    @Test
    void updateCustomer_shouldReturn200AndUpdatedResponse() throws Exception {
        UUID id = UUID.randomUUID();

        CreateCustomerRequestDTO request = new CreateCustomerRequestDTO();
        request.setFirstName("Updated");
        request.setLastName("User");
        request.setEmail("updated@example.com");
        request.setPhoneNumber("9999999999");
        request.setPhoneType("HOME");
        request.setAddress1("New Address");
        request.setCity("New City");
        request.setState("TX");
        request.setZipcode("73301");

        CustomerResponseDTO updated = new CustomerResponseDTO();
        updated.setCustomerId(id);
        updated.setFirstName("Updated");
        updated.setLastName("User");
        updated.setEmail("updated@example.com");

        Mockito.when(customerService.updateCustomer(eq(id), any(CreateCustomerRequestDTO.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/customers/" + id)
                        .with(csrf())   // ✅ REQUIRED for PUT
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }
}
