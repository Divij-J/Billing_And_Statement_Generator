package com.example.billing_and_statement_generator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.validator.constraints.CreditCardNumber;
import java.util.List;
import java.util.UUID;


@Entity @Table(name="customer")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer {
    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastNname;

    @Size(max = 1)
    @Column(name = "middle_initial")
    private String middleInitial;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "phone_number", nullable = false, unique = true,  length = 15)
    private String phoneNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "phone_type", nullable = false)
    private String phonetype;

    @NotBlank
    @Column(name = "address_line_1", nullable = false)
    private String address1;

    @Column(name = "address_line_2")
    private String address2;

    @NotBlank
    @Column(name = "city", nullable = false)
    private String city;

    @NotBlank
    @Size(min = 2, max = 2)
    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @NotBlank
    @Column(name = "zip_code", nullable = false, length = 5)
    private String zipcode;

    @OneToMany(mappedBy = "customer")
    private List<Card> cards;

    public enum CardType {
        Mobile,
        Home,
        Work
    }
}