package com.example.billing_and_statement_generator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BillingUtils {

  private BillingUtils() {}

  // Due date is always 21 days after the cycle end date
          public static LocalDate calculateDueDate(LocalDate cycleEndDate) {
    return cycleEndDate.plusDays(21);
  }

  // Daily interest formula: outstanding × (annualRate ÷ 365) × numberOfDays
          public static BigDecimal calculateInterest(BigDecimal outstanding,
                        BigDecimal annualRate,
                        LocalDate startDate,
                        LocalDate endDate) {
    long days = ChronoUnit.DAYS.between(startDate, endDate);
    if (days <= 0) return BigDecimal.ZERO;
    return outstanding
        .multiply(annualRate)
        .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP)
        .multiply(new BigDecimal(days))
        .setScale(2, RoundingMode.HALF_UP);
  }

  // Minimum due = max(5% of totalOutstanding, $100 floor)
          public static BigDecimal calculateMinimumDue(BigDecimal totalOutstanding) {
    BigDecimal fivePercent = totalOutstanding
        .multiply(new BigDecimal("0.05"))
        .setScale(2, RoundingMode.HALF_UP);
    return fivePercent.max(new BigDecimal("100.00"));
  }

  // Cash advance fee = feeRate × totalCashAdvance
          public static BigDecimal calculateCashAdvanceFee(BigDecimal totalCashAdvance,
                           BigDecimal feeRate) {
    return totalCashAdvance
        .multiply(feeRate)
        .setScale(2, RoundingMode.HALF_UP);
  }
}