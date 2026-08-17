package lab;

import java.math.BigDecimal;

public final class InvoiceCalculator {
    private final TaxRateTable taxRateTable;

    public InvoiceCalculator(TaxRateTable taxRateTable) {
        this.taxRateTable = taxRateTable;
    }

    public BigDecimal calculateTax(BigDecimal subtotal, BigDecimal requestedRate) {
        BigDecimal taxPercent = taxRateTable.findTaxPercent(requestedRate)
                .orElseThrow(() -> new IllegalArgumentException("税率が登録されていません: " + requestedRate));
        return subtotal.multiply(taxPercent).setScale(2);
    }
}
