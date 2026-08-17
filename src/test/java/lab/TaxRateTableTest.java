package lab;

import java.math.BigDecimal;

public final class TaxRateTableTest {
    public static void main(String[] args) {
        sameNumericRateWithDifferentScaleIsAccepted();
        exactScaleRateStillCalculatesTax();
        System.out.println("PASS: all tests");
    }

    static void sameNumericRateWithDifferentScaleIsAccepted() {
        TaxRateTable table = new TaxRateTable();
        table.register(new BigDecimal("0.10"), new BigDecimal("0.10"));
        InvoiceCalculator calculator = new InvoiceCalculator(table);

        BigDecimal actual = calculator.calculateTax(new BigDecimal("100.00"), new BigDecimal("0.100"));

        assertEquals(new BigDecimal("10.00"), actual,
                "同じ数値の税率はスケールが違っても税額を計算できる");
    }

    static void exactScaleRateStillCalculatesTax() {
        TaxRateTable table = new TaxRateTable();
        table.register(new BigDecimal("0.10"), new BigDecimal("0.10"));
        InvoiceCalculator calculator = new InvoiceCalculator(table);

        BigDecimal actual = calculator.calculateTax(new BigDecimal("100.00"), new BigDecimal("0.10"));

        assertEquals(new BigDecimal("10.00"), actual,
                "登録時と同じスケールの税率は計算できる");
    }

    private static void assertEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
