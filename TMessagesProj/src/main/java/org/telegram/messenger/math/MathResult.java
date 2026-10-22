package org.telegram.messenger.math;

public final class MathResult {

    private final double value;
    private final boolean hasOperation;
    private final char decimalSeparator;

    public MathResult(double value, boolean hasOperation, char decimalSeparator) {
        this.value = value;
        this.hasOperation = hasOperation;
        this.decimalSeparator = decimalSeparator;
    }

    public double getValue() {
        return value;
    }

    public boolean getHasOperation() {
        return hasOperation;
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }
}
