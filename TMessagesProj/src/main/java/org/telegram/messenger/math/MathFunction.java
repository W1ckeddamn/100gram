package org.telegram.messenger.math;

public final class MathFunction {

    public interface FunctionHandler {
        double apply(double[] args);
    }

    private final String name;
    private final int minArity;
    private final int maxArity;
    private final FunctionHandler handler;

    public MathFunction(String name, int arity, FunctionHandler handler) {
        this(name, arity, arity, handler);
    }

    public MathFunction(String name, int minArity, int maxArity, FunctionHandler handler) {
        this.name = name;
        this.minArity = minArity;
        this.maxArity = maxArity;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public int getMinArity() {
        return minArity;
    }

    public int getMaxArity() {
        return maxArity;
    }

    public double apply(double[] args) {
        return handler.apply(args);
    }
}
