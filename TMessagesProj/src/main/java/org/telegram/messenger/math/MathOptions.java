package org.telegram.messenger.math;

public final class MathOptions {

    private final char decimalSeparator;

    public MathOptions() {
        this('.');
    }

    public MathOptions(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }
}
