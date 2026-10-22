package org.telegram.messenger.math;

import java.util.ArrayList;
import java.util.Locale;

public final class MathExpression {

    public static final MathExpression INSTANCE = new MathExpression();
    public static final MathParseError error = new MathParseError();

    public static final class MathParseError extends RuntimeException {
        public MathParseError() {
            super(null, null, false, false);
        }
    }

    public static final class Token {
        public static final int TYPE_NUMBER = 0;
        public static final int TYPE_SYMBOL = 1;
        public static final int TYPE_LPAREN = 2;
        public static final int TYPE_RPAREN = 3;
        public static final int TYPE_IDENTIFIER = 4;
        public static final int TYPE_SEMICOLON = 5;
        public static final int TYPE_EOF = 6;

        private final int type;
        private final double number;
        private final String text;

        public Token(int type, double number, String text) {
            this.type = type;
            this.number = number;
            this.text = text != null ? text : "";
        }

        public int getType() {
            return type;
        }

        public double getNumber() {
            return number;
        }

        public String getText() {
            return text;
        }
    }

    public static final class Operand {
        private final double value;
        private final boolean isPercent;

        public Operand(double value, boolean isPercent) {
            this.value = value;
            this.isPercent = isPercent;
        }

        public double getValue() {
            return value;
        }

        public boolean isPercent() {
            return isPercent;
        }
    }

    private MathExpression() {
    }

    public boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == 160;
    }

    public MathResult evaluate(CharSequence expression, MathOptions options) {
        if (expression == null || options == null) {
            return null;
        }
        try {
            return new Parser(expression, options).parse();
        } catch (MathParseError e) {
            return null;
        }
    }

    public MathSuggestion suggestionAt(CharSequence text, int caret, MathOptions options) {
        if (text == null || options == null || caret <= 0 || caret > text.length()) {
            return null;
        }
        int equalsIndex = caret - 1;
        if (text.charAt(equalsIndex) != '=') {
            return null;
        }
        int afterEquals = caret;
        while (afterEquals < text.length() && isBlank(text.charAt(afterEquals))) {
            afterEquals++;
        }
        if (afterEquals < text.length()) {
            char nextChar = text.charAt(afterEquals);
            if (Character.isDigit(nextChar) || nextChar == '.' || nextChar == ',') {
                return null;
            }
        }
        Integer start = expressionStart(text, equalsIndex);
        if (start == null) {
            return null;
        }
        int candidateStart = start;
        while (true) {
            if (!looksLikeDate(text, candidateStart, equalsIndex)) {
                StringBuilder sb = new StringBuilder(equalsIndex - candidateStart);
                sb.append(text, candidateStart, equalsIndex);
                MathResult result = evaluate(sb, options);
                if (result != null) {
                    if (result.getHasOperation()) {
                        String formatted = MathFormat.INSTANCE.format(result.getValue(), result.getDecimalSeparator());
                        if (formatted != null) {
                            boolean pad = equalsIndex > 0 && isBlank(text.charAt(equalsIndex - 1));
                            String insert = pad ? " " + formatted : formatted;
                            return new MathSuggestion(caret, insert, formatted);
                        }
                    }
                    return null;
                }
            }
            Integer next = nextCandidate(text, candidateStart, equalsIndex);
            if (next == null) {
                break;
            }
            candidateStart = next;
        }
        return null;
    }

    private Integer nextCandidate(CharSequence text, int from, int to) {
        int i = from;
        while (i < to) {
            char c = text.charAt(i);
            i++;
            if (c == '(') {
                return i;
            }
            if (isBlank(c)) {
                while (i < to && isBlank(text.charAt(i))) {
                    i++;
                }
                if (i < to) {
                    return i;
                }
                return null;
            }
        }
        return null;
    }

    private Integer expressionStart(CharSequence text, int equalsIndex) {
        int max = Math.max(0, equalsIndex - 64);
        int i = equalsIndex - 1;
        while (i >= max && isExpressionChar(text.charAt(i))) {
            i--;
        }
        do {
            i++;
            if (i >= equalsIndex) {
                break;
            }
        } while (isBlank(text.charAt(i)));

        if (i >= equalsIndex) {
            return null;
        }
        if (i > 0) {
            char prev = text.charAt(i - 1);
            if (Character.isLetterOrDigit(prev) || prev == '_') {
                return null;
            }
        }
        return i;
    }

    private boolean isExpressionChar(char c) {
        return Character.isDigit(c) || Character.isLetter(c) || c == '.' || c == ',' || isBlank(c) || MathOperators.INSTANCE.getSymbolChars().contains(c);
    }

    private boolean looksLikeDate(CharSequence text, int from, int to) {
        while (from < to && isBlank(text.charAt(from))) {
            from++;
        }
        while (to > from && isBlank(text.charAt(to - 1))) {
            to--;
        }
        if (to - from < 6) {
            return false;
        }
        ArrayList<Integer> segments = new ArrayList<>(4);
        int count = 0;
        char separator = ' ';
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                count++;
            } else {
                if (c != '/' && c != '.' && c != '-') {
                    return false;
                }
                if (separator == ' ') {
                    separator = c;
                } else if (separator != c) {
                    return false;
                }
                if (count == 0 || segments.size() == 2) {
                    return false;
                }
                segments.add(count);
                count = 0;
            }
        }
        if (count != 0 && segments.size() == 2) {
            segments.add(count);
            switch (separator) {
                case '-':
                    if (segments.get(0) == 4 && segments.get(1) <= 2 && segments.get(2) <= 2) {
                        return true;
                    }
                    break;
                case '.':
                case '/':
                    return segments.get(0) <= 2 && segments.get(1) <= 2 && (segments.get(2) == 2 || segments.get(2) == 4);
                default:
                    return false;
            }
        }
        return false;
    }

    public static final class Parser {
        private final CharSequence source;
        private final MathOptions options;
        private final ArrayList<Token> tokens = new ArrayList<>();
        private int position;
        private int depth;
        private boolean hasOperation;
        private Character separator;

        public Parser(CharSequence source, MathOptions options) {
            this.source = source;
            this.options = options;
        }

        public MathResult parse() {
            tokenize();
            Operand expr = parseExpression(0);
            expect(Token.TYPE_EOF);
            double val = expr.getValue();
            boolean op = this.hasOperation;
            char sep = this.separator != null ? this.separator : this.options.getDecimalSeparator();
            return new MathResult(val, op, sep);
        }

        private void tokenize() {
            int len = source.length();
            int i = 0;
            while (i < len) {
                char c = source.charAt(i);
                if (!MathExpression.INSTANCE.isBlank(c)) {
                    if (Character.isDigit(c) || ((c == '.' || c == ',') && i + 1 < len && Character.isDigit(source.charAt(i + 1)))) {
                        i = readNumber(i);
                        continue;
                    } else if (c == '(') {
                        tokens.add(new Token(Token.TYPE_LPAREN, 0, "("));
                    } else if (c == ')') {
                        tokens.add(new Token(Token.TYPE_RPAREN, 0, ")"));
                    } else if (c == ';') {
                        tokens.add(new Token(Token.TYPE_SEMICOLON, 0, ";"));
                    } else if (Character.isLetter(c)) {
                        int start = i;
                        while (i < len && (Character.isLetter(source.charAt(i)) || Character.isDigit(source.charAt(i)))) {
                            i++;
                        }
                        tokens.add(new Token(Token.TYPE_IDENTIFIER, 0, source.subSequence(start, i).toString().toLowerCase(Locale.ROOT)));
                        continue;
                    } else {
                        String symbol = MathOperators.INSTANCE.matchSymbol(source, i);
                        if (symbol == null) {
                            throw MathExpression.error;
                        }
                        tokens.add(new Token(Token.TYPE_SYMBOL, 0, symbol));
                        i += symbol.length();
                        continue;
                    }
                }
                i++;
            }
            tokens.add(new Token(Token.TYPE_EOF, 0, ""));
        }

        private int readNumber(int from) {
            int len = source.length();
            int i = from;
            while (i < len && Character.isDigit(source.charAt(i))) {
                i++;
            }
            int intPartLen = i - from;
            char dot = i < len ? source.charAt(i) : ' ';
            int fracPartLen = 0;
            if ((dot == '.' || dot == ',') && i + 1 < len && Character.isDigit(source.charAt(i + 1))) {
                i++;
                int fracStart = i;
                while (i < len && Character.isDigit(source.charAt(i))) {
                    i++;
                }
                fracPartLen = i - fracStart;
                if (dot == ',' && fracPartLen == 3 && intPartLen >= 1 && intPartLen < 4 && source.charAt(from) != '0') {
                    throw MathExpression.error;
                }
                if (this.separator == null) {
                    this.separator = dot;
                }
            }
            if (intPartLen == 0 && fracPartLen == 0) {
                throw MathExpression.error;
            }
            String raw = source.subSequence(from, i).toString().replace(',', '.');
            try {
                double val = Double.parseDouble(raw);
                tokens.add(new Token(Token.TYPE_NUMBER, val, null));
                return i;
            } catch (Exception e) {
                throw MathExpression.error;
            }
        }

        private Token peek() {
            return tokens.get(position);
        }

        private Token next() {
            return tokens.get(position++);
        }

        private Token expect(int type) {
            Token t = next();
            if (t.getType() == type) {
                return t;
            }
            throw MathExpression.error;
        }

        private Operand parseExpression(int minPrecedence) {
            depth++;
            if (depth > 32) {
                throw MathExpression.error;
            }
            try {
                Operand left = parseUnary();
                while (true) {
                    Token token = peek();
                    if (token.getType() != Token.TYPE_SYMBOL) {
                        break;
                    }
                    InfixOperator op = MathOperators.INSTANCE.infixFor(token.getText());
                    if (op == null || op.getPrecedence() < minPrecedence) {
                        break;
                    }
                    next();
                    Operand right = parseExpression(op.getRightAssociative() ? op.getPrecedence() : op.getPrecedence() + 1);
                    this.hasOperation = true;
                    left = new Operand(op.apply(left.getValue(), right.getValue(), right.isPercent()), false);
                }
                return left;
            } finally {
                depth--;
            }
        }

        private Operand parseUnary() {
            Token token = peek();
            if (token.getType() == Token.TYPE_SYMBOL) {
                PrefixOperator op = MathOperators.INSTANCE.prefixFor(token.getText());
                if (op != null) {
                    next();
                    Operand right = parseExpression(op.getPrecedence());
                    if (op.isOperation()) {
                        this.hasOperation = true;
                    }
                    return new Operand(op.apply(right.getValue()), right.isPercent());
                }
            }
            return parsePostfix();
        }

        private Operand parsePostfix() {
            double val = parsePrimary();
            boolean isPercent = false;
            while (true) {
                Token token = peek();
                if (token.getType() == Token.TYPE_SYMBOL) {
                    PostfixOperator op = MathOperators.INSTANCE.postfixFor(token.getText());
                    if (op != null) {
                        next();
                        val = op.apply(val);
                        this.hasOperation = true;
                        isPercent = op.isPercent();
                        continue;
                    }
                }
                break;
            }
            return new Operand(val, isPercent);
        }

        private double parsePrimary() {
            Token token = next();
            int type = token.getType();
            if (type == Token.TYPE_NUMBER) {
                return token.getNumber();
            }
            if (type == Token.TYPE_LPAREN) {
                Operand inner = parseExpression(0);
                expect(Token.TYPE_RPAREN);
                return inner.getValue();
            }
            if (type == Token.TYPE_IDENTIFIER) {
                return parseIdentifier(token.getText());
            }
            throw MathExpression.error;
        }

        private double parseIdentifier(String name) {
            Double constant = MathOperators.INSTANCE.constantFor(name);
            if (constant != null) {
                return constant;
            }
            MathFunction fn = MathOperators.INSTANCE.functionFor(name);
            if (fn == null) {
                throw MathExpression.error;
            }
            expect(Token.TYPE_LPAREN);
            ArrayList<Double> args = new ArrayList<>(2);
            if (peek().getType() != Token.TYPE_RPAREN) {
                args.add(parseExpression(0).getValue());
                while (peek().getType() == Token.TYPE_SEMICOLON) {
                    next();
                    args.add(parseExpression(0).getValue());
                }
            }
            expect(Token.TYPE_RPAREN);
            if (args.size() < fn.getMinArity() || args.size() > fn.getMaxArity()) {
                throw MathExpression.error;
            }
            double[] arr = new double[args.size()];
            for (int i = 0; i < args.size(); i++) {
                arr[i] = args.get(i);
            }
            this.hasOperation = true;
            return fn.apply(arr);
        }
    }
}
