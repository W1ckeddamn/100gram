package org.telegram.messenger.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PrefixOperator {

    public interface PrefixHandler {
        double apply(double value);
    }

    private final List<String> symbols;
    private final int precedence;
    private final boolean operation;
    private final PrefixHandler handler;

    public PrefixOperator(List<String> symbols, int precedence, boolean operation, PrefixHandler handler) {
        this.symbols = symbols;
        this.precedence = precedence;
        this.operation = operation;
        this.handler = handler;
    }

    public PrefixOperator(String symbol, int precedence, boolean operation, PrefixHandler handler) {
        this(Collections.singletonList(symbol), precedence, operation, handler);
    }

    public PrefixOperator(String[] symbols, int precedence, boolean operation, PrefixHandler handler) {
        this(Arrays.asList(symbols), precedence, operation, handler);
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean isOperation() {
        return operation;
    }

    public double apply(double value) {
        return handler.apply(value);
    }
}
