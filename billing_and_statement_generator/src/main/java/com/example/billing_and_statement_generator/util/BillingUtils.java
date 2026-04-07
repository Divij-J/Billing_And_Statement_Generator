package com.example.billing_and_statement_generator.util;

import com.example.billing_and_statement_generator.entity.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

public class BillingUtils {

    private BillingUtils() {
    }

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
        if (totalOutstanding == null || totalOutstanding.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal fivePercent = totalOutstanding
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_UP);
        return fivePercent.max(new BigDecimal("100.00"));
    }

    // Helper method for calculating total fees
    public static BigDecimal calculateTotalFees(List<Transaction> transactions) {
        // Return 0 when no transactions exist
        if(transactions == null || transactions.isEmpty())
            return BigDecimal.ZERO;

        EnumSet<Transaction.transactionType> FEE_TYPES = EnumSet.of(
                Transaction.transactionType.LATEFEE,
                Transaction.transactionType.CASHADVANCEFEE,
                Transaction.transactionType.ANNUALMEMBERSHIPFEE
        );

        return transactions.stream()
                .filter(tx -> FEE_TYPES.contains(tx.getTransactionType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Helper method for calculating total cash advance fees
    public static BigDecimal calculateCashAdvanceFees(List<Transaction> transactions) {
        // Return 0 when no transactions exist
        if(transactions == null || transactions.isEmpty())
            return BigDecimal.ZERO;

        return transactions.stream()
                .filter(tx -> tx.getTransactionType() == Transaction.transactionType.CASHADVANCEFEE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}