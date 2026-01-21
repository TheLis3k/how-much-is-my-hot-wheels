package pl.thelis3k.howmuchismyhotwheels.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceUtil {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static int toCents(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) return 0;

        String clean = rawPrice.replaceAll("[^0-9,.]", "").replace(",", ".");

        try {
            return new BigDecimal(clean).multiply(HUNDRED).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public static BigDecimal toMajor(int cents) {
        return BigDecimal.valueOf(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}