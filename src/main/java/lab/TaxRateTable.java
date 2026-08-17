package lab;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TaxRateTable {
    private final Map<BigDecimal, BigDecimal> rates = new HashMap<>();

    public void register(BigDecimal rate, BigDecimal taxPercent) {
        rates.put(canonicalize(rate), taxPercent);
    }

    public Optional<BigDecimal> findTaxPercent(BigDecimal rate) {
        return Optional.ofNullable(rates.get(canonicalize(rate)));
    }

    private static BigDecimal canonicalize(BigDecimal rate) {
        return rate.stripTrailingZeros();
    }
}
