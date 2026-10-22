package org.telegram.messenger.math;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PostfixOperator {

    public interface PostfixHandler {
        double apply(double value);
    }

    private final List<String> symbols;
    private final boolean percent;
    private final PostfixHandler handler;

    public PostfixOperator(List<String> symbols, boolean percent, PostfixHandler handler) {
        this.symbols = symbols;
        this.percent = percent;
        this.handler = handler;
    }

    public PostfixOperator(String symbol, boolean percent, PostfixHandler handler) {
        this(Collections.singletonList(symbol), percent, handler);
    }

    public PostfixOperator(String[] symbols, boolean percent, PostfixHandler handler) {
        this(Arrays.asList(symbols), percent, handler);
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public boolean isPercent() {
        return percent;
    }

    public double apply(double value) {
        return handler.apply(value);
    }
}
