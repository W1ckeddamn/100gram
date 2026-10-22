package org.telegram.messenger.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MathOperators {

    public static final MathOperators INSTANCE = new MathOperators();

    private final List<InfixOperator> infix;
    private final List<PrefixOperator> prefix;
    private final List<PostfixOperator> postfix;
    private final List<MathFunction> functions;
    private final Map<String, Double> constants;
    private final Set<Character> symbolChars;

    private MathOperators() {
        ArrayList<InfixOperator> infixList = new ArrayList<>();
        infixList.add(new InfixOperator(Collections.singletonList("+"), 10, false, (l, r, p) -> p ? l + (l * r / 100.0) : l + r));
        infixList.add(new InfixOperator(new String[]{"-", "−"}, 10, false, (l, r, p) -> p ? l - (l * r / 100.0) : l - r));
        infixList.add(new InfixOperator(new String[]{"*", "×"}, 20, false, (l, r, p) -> p ? l * (r / 100.0) : l * r));
        infixList.add(new InfixOperator(new String[]{"/", "÷"}, 20, false, (l, r, p) -> p ? l / (r / 100.0) : l / r));
        infixList.add(new InfixOperator(Collections.singletonList("^"), 40, true, (l, r, p) -> Math.pow(l, r)));
        this.infix = Collections.unmodifiableList(infixList);

        ArrayList<PrefixOperator> prefixList = new ArrayList<>();
        prefixList.add(new PrefixOperator(new String[]{"-", "−"}, 30, false, v -> -v));
        prefixList.add(new PrefixOperator(Collections.singletonList("+"), 30, false, v -> v));
        prefixList.add(new PrefixOperator(Collections.singletonList("√"), 40, true, Math::sqrt));
        this.prefix = Collections.unmodifiableList(prefixList);

        ArrayList<PostfixOperator> postfixList = new ArrayList<>();
        postfixList.add(new PostfixOperator(Collections.singletonList("%"), true, v -> v));
        postfixList.add(new PostfixOperator(Collections.singletonList("!"), false, MathOperators::factorial));
        this.postfix = Collections.unmodifiableList(postfixList);

        ArrayList<MathFunction> funcList = new ArrayList<>();
        funcList.add(new MathFunction("sin", 1, args -> Math.sin(args[0])));
        funcList.add(new MathFunction("cos", 1, args -> Math.cos(args[0])));
        funcList.add(new MathFunction("tan", 1, args -> Math.tan(args[0])));
        funcList.add(new MathFunction("asin", 1, args -> Math.asin(args[0])));
        funcList.add(new MathFunction("acos", 1, args -> Math.acos(args[0])));
        funcList.add(new MathFunction("atan", 1, args -> Math.atan(args[0])));
        funcList.add(new MathFunction("sinh", 1, args -> Math.sinh(args[0])));
        funcList.add(new MathFunction("cosh", 1, args -> Math.cosh(args[0])));
        funcList.add(new MathFunction("tanh", 1, args -> Math.tanh(args[0])));
        funcList.add(new MathFunction("log", 1, args -> Math.log10(args[0])));
        funcList.add(new MathFunction("ln", 1, args -> Math.log(args[0])));
        funcList.add(new MathFunction("abs", 1, args -> Math.abs(args[0])));
        funcList.add(new MathFunction("sqrt", 1, args -> Math.sqrt(args[0])));
        funcList.add(new MathFunction("cbrt", 1, args -> Math.cbrt(args[0])));
        funcList.add(new MathFunction("ceil", 1, args -> Math.ceil(args[0])));
        funcList.add(new MathFunction("floor", 1, args -> Math.floor(args[0])));
        funcList.add(new MathFunction("round", 1, args -> (double) Math.round(args[0])));
        funcList.add(new MathFunction("exp", 1, args -> Math.exp(args[0])));
        funcList.add(new MathFunction("deg", 1, args -> Math.toDegrees(args[0])));
        funcList.add(new MathFunction("rad", 1, args -> Math.toRadians(args[0])));
        funcList.add(new MathFunction("min", 2, 2, args -> Math.min(args[0], args[1])));
        funcList.add(new MathFunction("max", 2, 2, args -> Math.max(args[0], args[1])));
        funcList.add(new MathFunction("fact", 1, args -> factorial(args[0])));
        this.functions = Collections.unmodifiableList(funcList);

        HashMap<String, Double> constMap = new HashMap<>();
        constMap.put("pi", Math.PI);
        constMap.put("π", Math.PI);
        constMap.put("e", Math.E);
        constMap.put("phi", (1.0 + Math.sqrt(5.0)) / 2.0);
        this.constants = Collections.unmodifiableMap(constMap);

        HashSet<Character> chars = new HashSet<>();
        for (InfixOperator op : infix) {
            for (String s : op.getSymbols()) {
                for (int i = 0; i < s.length(); i++) {
                    chars.add(s.charAt(i));
                }
            }
        }
        for (PrefixOperator op : prefix) {
            for (String s : op.getSymbols()) {
                for (int i = 0; i < s.length(); i++) {
                    chars.add(s.charAt(i));
                }
            }
        }
        for (PostfixOperator op : postfix) {
            for (String s : op.getSymbols()) {
                for (int i = 0; i < s.length(); i++) {
                    chars.add(s.charAt(i));
                }
            }
        }
        this.symbolChars = Collections.unmodifiableSet(chars);
    }

    public static double factorial(double v) {
        if (v < 0 || v > 170 || v != Math.floor(v)) {
            return Double.NaN;
        }
        int n = (int) v;
        double res = 1.0;
        for (int i = 2; i <= n; i++) {
            res *= i;
        }
        return res;
    }

    public InfixOperator infixFor(String symbol) {
        for (InfixOperator op : infix) {
            if (op.getSymbols().contains(symbol)) {
                return op;
            }
        }
        return null;
    }

    public PrefixOperator prefixFor(String symbol) {
        for (PrefixOperator op : prefix) {
            if (op.getSymbols().contains(symbol)) {
                return op;
            }
        }
        return null;
    }

    public PostfixOperator postfixFor(String symbol) {
        for (PostfixOperator op : postfix) {
            if (op.getSymbols().contains(symbol)) {
                return op;
            }
        }
        return null;
    }

    public MathFunction functionFor(String name) {
        for (MathFunction fn : functions) {
            if (fn.getName().equalsIgnoreCase(name)) {
                return fn;
            }
        }
        return null;
    }

    public Double constantFor(String name) {
        return constants.get(name.toLowerCase());
    }

    public List<InfixOperator> getInfix() {
        return infix;
    }

    public List<PrefixOperator> getPrefix() {
        return prefix;
    }

    public List<PostfixOperator> getPostfix() {
        return postfix;
    }

    public List<MathFunction> getFunctions() {
        return functions;
    }

    public Map<String, Double> getConstants() {
        return constants;
    }

    public Set<Character> getSymbolChars() {
        return symbolChars;
    }

    public String matchSymbol(CharSequence text, int at) {
        if (at >= text.length()) {
            return null;
        }
        char c = text.charAt(at);
        String single = String.valueOf(c);
        if (symbolChars.contains(c)) {
            return single;
        }
        return null;
    }
}
