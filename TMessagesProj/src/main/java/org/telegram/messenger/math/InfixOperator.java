package org.telegram.messenger.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class InfixOperator {

    public interface InfixHandler {
        double apply(double left, double right, boolean rightIsPercent);
    }

    private final List<String> symbols;
    private final int precedence;
    private final boolean rightAssociative;
    private final InfixHandler handler;

    public InfixOperator(List<String> symbols, int precedence, boolean rightAssociative, InfixHandler handler) {
        this.symbols = symbols;
        this.precedence = precedence;
        this.rightAssociative = rightAssociative;
        this.handler = handler;
    }

    public InfixOperator(String symbol, int precedence, boolean rightAssociative, InfixHandler handler) {
        this(Collections.singletonList(symbol), precedence, rightAssociative, handler);
    }

    public InfixOperator(String[] symbols, int precedence, boolean rightAssociative, InfixHandler handler) {
        this(Arrays.asList(symbols), precedence, rightAssociative, handler);
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean getRightAssociative() {
        return rightAssociative;
    }

    public double apply(double left, double right, boolean rightIsPercent) {
        return handler.apply(left, right, rightIsPercent);
    }
}
