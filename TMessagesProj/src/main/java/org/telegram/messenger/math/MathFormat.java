package org.telegram.messenger.math;

import java.math.BigDecimal;
import java.math.MathContext;

public final class MathFormat {

    public static final MathFormat INSTANCE = new MathFormat();

    private MathFormat() {
    }

    public String format(double value, char decimalSeparator) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        double abs = Math.abs(value);
        if (abs >= 1.0e12d) {
            return null;
        }
        if (abs != 0.0d && abs < 1.0e-9d) {
            return null;
        }
        try {
            BigDecimal bd = BigDecimal.valueOf(value).round(new MathContext(12)).stripTrailingZeros();
            String plain = bd.toPlainString();
            if ("-0".equals(plain)) {
                plain = "0";
            }
            if (plain.length() > 24) {
                return null;
            }
            if (decimalSeparator != '.') {
                return plain.replace('.', decimalSeparator);
            }
            return plain;
        } catch (Exception e) {
            return null;
        }
    }
}
