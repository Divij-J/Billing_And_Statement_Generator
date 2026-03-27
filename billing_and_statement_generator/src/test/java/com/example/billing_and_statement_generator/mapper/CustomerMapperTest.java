package com.example.billing_and_statement_generator.mapper;

import com.example.billing_and_statement_generator.dto.CreateCustomerRequestDTO;
import com.example.billing_and_statement_generator.dto.CustomerResponseDTO;
import com.example.billing_and_statement_generator.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setup() {
        mapper = Mappers.getMapper(CustomerMapper.class);
    }

    // ------------------------------------------------------------------------
    // TEST: toDTO()
    // ------------------------------------------------------------------------
    @Test
    void toDTO_shouldMapCustomerToDTO() {
        Customer customer = new Customer();
        customer.setCustomerId(java.util.UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setMiddleInitial("A");
        customer.setEmail("john@example.com");
        customer.setPhoneNumber("1234567890");
        customer.setPhoneType(Customer.PhoneType.MOBILE);
        customer.setAddress1("123 Test St");
        customer.setAddress2("Unit 4");
        customer.setCity("Chicago");
        customer.setState("IL");
        customer.setZipcode("60601");

        CustomerResponseDTO dto = mapper.toDTO(customer);

        assertNotNull(dto);
        assertEquals(customer.getCustomerId(), dto.getCustomerId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("A", dto.getMiddleInitial());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("1234567890", dto.getPhoneNumber());
        assertEquals("MOBILE", dto.getPhoneType());  // via @Named mapping
        assertEquals("60601", dto.getZipcode());
    }

    // ------------------------------------------------------------------------
    // TEST: updateEntityFromRequest()
    // ------------------------------------------------------------------------
    @Test
    void updateEntityFromRequest_shouldUpdateFieldsCorrectly() {
        Customer customer = new Customer();
        customer.setCustomerId(java.util.UUID.randomUUID()); // ignored by mapper
        customer.setFirstName("Old");
        customer.setLastName("User");
        customer.setPhoneType(Customer.PhoneType.HOME);

        CreateCustomerRequestDTO request = new CreateCustomerRequestDTO();
        request.setFirstName("New");
        request.setLastName("Name");
        request.setMiddleInitial("B");
        request.setEmail("new@example.com");
        request.setPhoneNumber("5555555555");
        request.setPhoneType("mobile");  // lowercase on purpose
        request.setAddress1("789 New St");
        request.setAddress2("Suite 22");
        request.setCity("Austin");
        request.setState("TX");
        request.setZipcode("73301");

        Customer updated = mapper.updateEntityFromRequest(request, customer);

        assertNotNull(updated);
        assertEquals("New", updated.getFirstName());
        assertEquals("Name", updated.getLastName());
        assertEquals("B", updated.getMiddleInitial());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("5555555555", updated.getPhoneNumber());
        assertEquals(Customer.PhoneType.MOBILE, updated.getPhoneType()); // uppercase + enum conversion
        assertEquals("Austin", updated.getCity());
        assertEquals("TX", updated.getState());
        assertEquals("73301", updated.getZipcode());
    }

    // ------------------------------------------------------------------------
    // TEST: phoneTypeToString() custom mapping
    // ------------------------------------------------------------------------
    @Test
    void phoneTypeToString_shouldReturnCorrectString() {
        assertEquals("MOBILE", mapper.phoneTypeToString(Customer.PhoneType.MOBILE));
        assertNull(mapper.phoneTypeToString(null));  // covers null branch
    }
}
