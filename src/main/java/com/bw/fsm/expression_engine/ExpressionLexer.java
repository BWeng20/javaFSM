package com.bw.fsm.expression_engine;

import java.util.ArrayList;

/**
 * Lexer for Expressions. <br>
 * Generates tokens from text.
 */
public class ExpressionLexer {

    final java.util.List<Character> chars;
    int pos = 0;
    final StringBuilder buffer = new StringBuilder(10);


    public ExpressionLexer(CharSequence text) {
        chars = new ArrayList<>(text.length());
        text.chars().forEach(value -> chars.add((char) value));
    }

    public boolean is_stop(char c) {
        return is_whitespace(c)
                || switch (c) {
            case '\0', '.', '!', ',', '\\',
                 // Operators
                 '-', '+', '/', ':', '*', '&', '|',
                 '<', '>', '=', '%', '?',
                 // Brackets
                 '[', ']', '(', ')', '{', '}',
                 // String
                 '"', '\'',
                 // Expressions Separator
                 ';' -> true;
            default -> false;
        };
    }

    public boolean is_string_delimiter(char c) {
        return (c == '\'' || c == '"');
    }

    public char next_char() {
        if (pos < chars.size()) {
            char c = chars.get(pos);
            ++pos;
            return c;
        } else {
            return '\0';
        }
    }

    public void push_back() {
        if (pos > 0) {
            --pos;
        }
    }

    /// Read a String.\
    /// delimiter - The delimiter\
    /// Escape sequences see String state-chart on JSON.org.
    public Token<?> read_string(char delimiter) throws ExpressionException {
        boolean escape = false;
        char c;
        while (true) {
            c = next_char();
            if (c == '\0') {
                throw new ExpressionException("Missing string delimiter");
            } else if (escape) {
                escape = false;
                switch (c) {
                    case '"', '\\', '/' -> {
                    }
                    case 'b' -> c = '\u0008';
                    case 'f' -> c = '\u000c';
                    case 'n' -> c = '\n';
                    case 'r' -> c = '\r';
                    case 't' -> c = '\t';
                    case 'u' -> {
                        // 4 hex digits
                        StringBuilder codepoint = new StringBuilder(4);
                        try {
                            for (int i = 0; i < 4; ++i) {
                                char cd = next_char();
                                if (is_digit(cd)) {
                                    codepoint.append(cd);
                                } else {
                                    throw new ExpressionException("Illegal \\u sequence in String");
                                }
                            }
                            buffer.append(Character.toChars(Integer.parseInt(codepoint.toString(), 16)));
                            continue;
                        } catch (Exception e) {
                            throw new ExpressionException(String.format("Illegal \\u sequence %s", codepoint));
                        }
                    }
                    default -> throw new ExpressionException("Illegal escape sequence in String");
                }
            } else if (c == '\\') {
                escape = true;
                continue;
            } else if (c == delimiter) {
                return new Token.TString(buffer.toString());
            }
            buffer.append(c);
        }
    }

    /// Read (possible combined) operators
    public Token<?> read_operator(char first) throws ExpressionException {
        Operator op =
                switch (first) {
                    case '-' -> Operator.Minus;
                    case '+' -> Operator.Plus;
                    case '*' -> Operator.Multiply;
                    case ':', '/' -> Operator.Divide;
                    case '&' -> Operator.And;
                    case '|' -> Operator.Or;
                    case '%' -> Operator.Modulus;
                    default -> {
                        var second = next_char();
                        if (second == '=') {
                            yield switch (first) {
                                case '?' -> Operator.AssignUndefined;
                                case '<' -> Operator.LessEqual;
                                case '>' -> Operator.GreaterEqual;
                                case '=' -> Operator.Equal;
                                case '!' -> Operator.NotEqual;
                                default -> // This method shall not be called with other chars.
                                        throw new ExpressionException("Internal Error");
                            }
                                    ;
                        } else {
                            push_back();
                            yield switch (first) {
                                case '<' -> Operator.Less;
                                case '>' -> Operator.Greater;
                                case '=' -> Operator.Assign;
                                case '!' -> Operator.Not;
                                default ->  // This method shall not be called with other chars.
                                        throw new ExpressionException("Internal Error");
                            };
                        }
                    }
                };
        return new Token.Operator(op);
    }

    /// Read a JSON Number (see state chart at JSON.org).
    /// c - The starting character.
    public Token<?> read_number(char c) throws ExpressionException {
        // States:
        // 0: Init
        // 1: In fix-point part
        // 2: In fraction part
        // 3: Just after "E"
        // 4: In exponent
        // 5: On starting "-"
        // 6: On "-" or "+" after "E"

        short state = 0;
        outer:
        while (true) {
            if (c == '.') {
                switch (state) {
                    case 0, 1, 5 -> state = 2;
                    default -> {
                        this.push_back();
                        break outer;
                    }
                }
            } else if (is_digit(c)) {
                switch (state) {
                    case 0, 5 -> state = 1;
                    case 3, 6 -> state = 4;
                    default -> {
                    }
                }
            } else if (c == '+') {
                // According to JSON only legal just after the "E".
                switch (state) {
                    case 0 -> {
                        return new Token.Operator(Operator.Plus);
                    }
                    case 5 -> {
                        this.push_back();
                        return new Token.Operator(Operator.Minus);
                    }
                    case 3 -> state = 6;
                    default -> {
                        this.push_back();
                        break outer;
                    }
                }
            } else if (c == '-') {
                // According to JSON only legal at start or just after the "E".
                switch (state) {
                    case 0 -> state = 5;
                    case 3 -> state = 6;
                    case 5 -> {
                        this.push_back();
                        return new Token.Operator(Operator.Minus);
                    }
                    default -> {
                        this.push_back();
                        break outer;
                    }
                }
            } else if (c == 'E' || c == 'e') {
                switch (state) {
                    case 1, 2 -> state = 3;
                    case 5 -> {
                        return new Token.Operator(Operator.Minus);
                    }
                    default -> {
                        this.push_back();
                        break outer;
                    }
                }
            } else {
                if (c != '\0') {
                    this.push_back();
                }
                break;
            }
            this.buffer.append(c);
            c = this.next_char();
        }
        return switch (state) {
            case 1 -> {
                try {
                    var v = Integer.parseInt(this.buffer.toString());
                    yield new Token.NumericToken.Integer(v);
                } catch (NumberFormatException ne) {
                    throw new ExpressionException(ne.getMessage());
                }
            }
            case 2, 4 -> {
                if (this.buffer.length() == 1) {
                    // Special case '.'
                    yield new Token.Separator('.');
                } else {
                    try {
                        var v = Double.parseDouble(this.buffer.toString());
                        yield new Token.NumericToken.Double(v);
                    } catch (NumberFormatException ne) {
                        throw new ExpressionException(ne.getMessage());
                    }
                }
            }
            case 3, 6 -> throw new ExpressionException("missing exponent in number");
            case 5 -> new Token.Operator(Operator.Minus);
            default -> throw new ExpressionException("internal error");
        };
    }

    /// A much, much simpler replacement for char.is_digit(10).
    public boolean is_digit(char c) {
        return (c >= '0' && c <= '9');
    }

    /// Check for a JSON whitespace.
    public boolean is_whitespace(char c) {
        return (c == ' ' || c == '\n' || c == '\r' || c == '\t');
    }

    private static final char[] no_stops = new char[0];

    /// Parse and return the next token.
    public Token<?> next_token() throws ExpressionException {
        return next_token_with_stop(no_stops);
    }

    public Token<?> next_token_with_stop(char[] hard_stops) throws ExpressionException {
        // at start of new symbol, eat all spaces
        eat_space();
        buffer.setLength(0);
        char c = next_char();

        // Start chars for a legal Number ('+' and "." NOT in JSON):
        if (is_digit(c) || c == '-' || c == '+' || c == '.') {
            return read_number(c);
        }
        while (true) {
            if (is_stop(c)) {
                if (buffer.isEmpty()) {
                    if (is_string_delimiter(c)) {
                        // At start of string
                        return read_string(c);
                    } else if (is_stop(c, hard_stops)) {
                        return new Token.Separator(c);
                    } else {
                        // return the current stop as symbol
                        switch (c) {
                            case '\0' -> {
                                return Token.EOE.INSTANCE;
                            }
                            case '?', '+', '-', '*', '<', '>', '=', '%', '/', ':', '!', '&', '|' -> {
                                return read_operator(c);
                            }
                            case '{', '}', '(', ')', '[', ']' -> {
                                return new Token.Bracket(c);
                            }
                            case ';' -> {
                                return Token.ExpressionSeparator.INSTANCE;
                            }
                            default -> {
                                return new Token.Separator(c);
                            }
                        }
                    }
                } else if (c != '\0') {
                    // handle this the next call
                    push_back();
                }
                return switch (buffer.toString()) {
                    case "true" -> new Token.Boolean(true);
                    case "false" -> new Token.Boolean(false);
                    case "null" -> Token.Null.INSTANCE;
                    default -> new Token.Identifier(buffer.toString());
                };
            }
            // append until stop is found.
            buffer.append(c);
            c = next_char();
        }
    }

    /// Return the next token as a number, otherwise throw.
    public Token.NumericToken<?> next_number() throws ExpressionException {
        var t = next_token();
        if (t instanceof Token.NumericToken<?> nb) {
            return nb;
        }
        throw new ExpressionException("Number expected");
    }

    /// Return the next token to an Identifier, otherwise return Error.
    public String next_name() throws ExpressionException {
        var t = next_token();
        if (t instanceof Token.Identifier id) {
            return id.value;
        } else {
            throw new ExpressionException(String.format("Unexpected token %s", t));
        }
    }

    /// Checks if the lexer has at least one token remaining.
    public boolean has_next() {
        return pos < chars.size();
    }

    /// Easts whitespaces.
    public void eat_space() {
        while (has_next() && is_whitespace(chars.get(pos))) {
            ++pos;
        }
    }

    private boolean is_stop(char c, char[] stops) {
        for (char x : stops)
            if (c == x)
                return true;
        return false;
    }

}
