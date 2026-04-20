package com.example.billing_and_statement_generator.cucumber;

import com.example.billing_and_statement_generator.BillingAndStatementGeneratorApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@CucumberContextConfiguration
@SpringBootTest(classes = BillingAndStatementGeneratorApplication.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class CucumberSpringConfiguration {
}