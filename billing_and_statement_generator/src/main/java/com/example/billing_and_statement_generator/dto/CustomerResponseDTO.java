package com.example.billing_and_statement_generator.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter

public class CustomerResponseDTO {

    private UUID customerId;
    private String firstName;
    private String lastName;
    private String middleInitial;
    private String email;
    private String phoneNumber;
    private String phoneType;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zipcode;
}