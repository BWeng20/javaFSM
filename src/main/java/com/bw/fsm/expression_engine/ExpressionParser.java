package com.bw.fsm.expression_engine;

import com.bw.fsm.Data;
import com.bw.fsm.Log;
import com.bw.fsm.StaticOptions;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.expression.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;

/**
 * Static tool class to process expressions.
 */
public final class ExpressionParser {

    /*/ Internal item for the parser stack. */
    protected static class ExpressionParserItem {
        Token<?> token;
        Expression expression;

        ExpressionParserItem(Token<?> token, Expression expression) {
            this.token = token;
            this.expression = expression;
        }

        ExpressionParserItem(Expression expression) {
            this.token = null;
            this.expression = expression;
        }

        ExpressionParserItem(Token<?> token) {
            this.token = token;
            this.expression = null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(20);
            sb.append('{');
            if (token != null) {
                sb.append("token:").append(token);
            } else if (expression == null)
                return "<empty>";
            if (expression != null) {
                if (token != null)
                    sb.append(' ');
                sb.append("expression:").append(expression);
            }
            return sb.append('}').toString();
        }
    }

    private static class SubExpression {
        char stop;
        @Nullable Expression expression;

        public SubExpression(char stop, @Nullable Expression expression) {
            this.stop = stop;
            this.expression = expression;
        }
    }

    /**
     * Parse a member list, stops at the matching stop char
     */
    private static List<Pair> parse_member_list(ExpressionLexer lexer, char stop) throws ExpressionException {
        List<Pair> r = new ArrayList<>();
        char stop_c;
        while (true) {
            SubExpression subExpression = parse_sub_expression(lexer, new char[]{':', stop});
            if (subExpression.expression == null) {
                if (r.isEmpty()) {
                    // Special case: empty member list
                    break;
                } else {
                    throw new ExpressionException("Error in member list");
                }
            } else {
                var valueExp = parse_sub_expression(lexer, new char[]{',', stop});
                stop_c = valueExp.stop;
                if (valueExp.expression == null) {
                    throw new ExpressionException("Missing value expression in member list");
                } else {
                    r.add(new Pair(subExpression.expression, valueExp.expression));
                }
            }
            if (stop_c == stop) {
                break;
            }
            if (stop_c == '\0') {
                throw new ExpressionException(String.format("Missing '%c'", stop));
            }
        }
        return r;
    }

    /// Parse an argument list, stops at the matching stop char
    public static List<Expression> parse_argument_list(ExpressionLexer lexer, char stop) throws ExpressionException {
        List<Expression> r = new ArrayList<>();
        while (true) {
            SubExpression subExpression = parse_sub_expression(lexer, new char[]{',', stop});
            if (subExpression.expression == null) {
                if (r.isEmpty()) {
                    // Special case: empty argument list
                    break;
                } else {
                    throw new ExpressionException("Error in argument list");
                }
            } else {
                r.add(subExpression.expression);
            }
            if (subExpression.stop == stop) {
                break;
            }
            if (subExpression.stop == '\0') {
                throw new ExpressionException(String.format("Missing '%c'", stop));
            }
        }
        return r;
    }

    /**
     * Parse an expression, returning a re-usable expression.
     */
    public static @NotNull Expression parse(String text) throws ExpressionException {
        ExpressionLexer lexer = new ExpressionLexer(text);
        SubExpression expression = parse_sub_expression(lexer, new char[]{'\0'});
        if (expression.expression == null) {
            throw new ExpressionException("Failed to parse");
        }
        return expression.expression;
    }

    /**
     * Parses and executes an expression.<br>
     * If possible, please use "parse" and re-use the parsed expressions.
     */
    public static Data execute(String source, GlobalData context) throws ExpressionException {
        if (StaticOptions.debug)
            Log.debug("execute: %s", source);
        Expression parser_result = parse(source);
        Data r = parser_result.execute(context, false);
        if (StaticOptions.debug)
            Log.debug("result: %s", r);
        return r;
    }

    private static boolean stops_contains(char[] stops, char stop) {
        for (char x : stops)
            if (x == stop) return true;
        return false;
    }

    // Translate the lexer tokens and put them to the stack. Resolve method calls and sub-expressions.
    // The result will be a stack sequence of identifier / operators / expressions.
    // All remaining "Identifier" are variables.
    private static SubExpression parse_sub_expression(ExpressionLexer lexer, char[] stops) throws ExpressionException {
        List<Expression> expressions = new ArrayList<>();
        Stack<ExpressionParserItem> stack = new Stack<>();
        char stop = '\0';
        Loop:
        while (true) {
            Token<?> t = lexer.next_token_with_stop(stops);
            switch (t.type) {
                case EOE -> {
                    break Loop;
                }
                case Null, TString, Boolean, Number -> stack.push(new ExpressionParserItem(new Constant(t.as_data())));
                case Identifier, Operator -> stack.push(new ExpressionParserItem(t));
                case Bracket -> {
                    final char br = (Character) t.value;
                    switch (br) {
                        case '(' -> {
                            if (stack.isEmpty()) {
                                SubExpression sev = parse_sub_expression(lexer, new char[]{')'});
                                if (sev.expression != null) {
                                    stack.push(new ExpressionParserItem(sev.expression));
                                }
                            } else {
                                var si = stack.pop();
                                if (si.token != null) {
                                    switch (si.token.type) {
                                        case Null, Separator, Bracket, Boolean, TString, Number ->
                                                throw new ExpressionException("Unexpected '('");
                                        case Identifier -> {
                                            Token.Identifier id = (Token.Identifier) si.token;
                                            var v = parse_argument_list(lexer, ')');
                                            var x = new Method(id.value, v);
                                            stack.push(new ExpressionParserItem(x));
                                        }
                                        case Operator -> {
                                            stack.push(new ExpressionParserItem(si.token));
                                            var se = parse_sub_expression(lexer, new char[]{')'});
                                            if (se.expression != null) {
                                                stack.push(new ExpressionParserItem(se.expression));
                                            }
                                        }
                                        case Error, EOE, ExpressionSeparator -> {
                                        }
                                    }
                                } else {
                                    throw new ExpressionException("Unexpected '('");
                                }
                            }
                        }
                        case '[' -> {
                            Expression new_stack_item;
                            if (stack.isEmpty()) {
                                var v = parse_argument_list(lexer, ']');
                                new_stack_item = new Array(v);
                            } else {
                                var si = stack.pop();
                                if (si.token != null) {
                                    switch (si.token.type) {
                                        case Null, Separator, Bracket, Boolean, TString, Number ->
                                                throw new ExpressionException("Unexpected '['");
                                        case Identifier -> {
                                            var v = parse_argument_list(lexer, ']');
                                            if (v.size() != 1) {
                                                throw new ExpressionException("index operator '[]' allows only one argument");
                                            }
                                            new_stack_item = new Index(
                                                    new Variable(si.token.value.toString()),
                                                    v.remove(0));
                                        }
                                        case Operator -> {
                                            // Put token back on stack.
                                            stack.push(new ExpressionParserItem(si.token));
                                            var v = parse_argument_list(lexer, ']');
                                            new_stack_item = new Array(v);
                                        }
                                        default -> throw new ExpressionException("Internal Error at '['");
                                    }
                                } else if (si.expression != null) {
                                    var v = parse_argument_list(lexer, ']');
                                    if (v.size() != 1) {
                                        throw new ExpressionException("index operator '[]' allows only one argument");
                                    }
                                    new_stack_item = new Index(si.expression, v.remove(0));
                                } else {
                                    throw new ExpressionException("internal error");
                                }
                            }
                            stack.push(new ExpressionParserItem(new_stack_item));
                        }
                        case '{' -> {
                            var v = parse_member_list(lexer, '}');
                            stack.push(new ExpressionParserItem(new Map(v)));
                        }
                        default -> {
                            if (stops_contains(stops, br)) {
                                stop = br;
                                break Loop;
                            }
                            throw new ExpressionException(String.format("Unexpected '%c'", br));
                        }
                    }
                }
                case Separator -> {
                    char sep = (Character) t.value;
                    if (stops_contains(stops, sep)) {
                        stop = sep;
                        break Loop;
                    } else if (sep == '.') {
                        stack.push(new ExpressionParserItem(new Token.Separator('.')));
                    }
                }
                case ExpressionSeparator -> {
                    Expression expression = stack_to_expression(stack);
                    if (!stack.isEmpty()) {
                        throw new ExpressionException("Failed to evaluate expression");
                    }
                    if (expression != null) {
                        expressions.add(expression);
                    }
                }
                case Error -> throw new ExpressionException("");
            }
        }
        expressions.add(stack_to_expression(stack));
        if (!stack.isEmpty()) {
            throw new ExpressionException("Failed to evaluate expression");
        } else if (expressions.isEmpty()) {
            return new SubExpression(stop, null);
        } else if (expressions.size() == 1) {
            return new SubExpression(stop, expressions.remove(expressions.size() - 1));
        } else {
            return new SubExpression(stop, new Sequence(expressions));
        }
    }

    /**
     * Removes both neighbours of the item at the index, then call the function with the
     * neighbours and replace the item at the index with the result.<br>
     * If the operation fails, all items (at index and neighbours) are removed.<br>
     * Neighbours must be ExpressionParserItem with an Expression.
     */
    private static boolean fold_stack_at(List<ExpressionParserItem> stack, int idx, BiFunction<Expression, Expression, Expression> f) {
        if (idx > 0 && (idx + 1) < stack.size()) {
            var right = stack.remove(idx + 1);
            stack.remove(idx);
            var left = stack.remove(idx - 1);

            if (right.expression != null) {
                if (left.expression != null) {
                    Expression expression = f.apply(left.expression, right.expression);
                    stack.add(idx - 1, new ExpressionParserItem(expression));
                    return true;
                }
            }
        }
        return false;
    }


    /// Tries to create an expression from the current contents of the parser-stack.
    private static Expression stack_to_expression(List<ExpressionParserItem> stack) throws ExpressionException {
        if (StaticOptions.debug)
            Log.debug("stack=%s", toString(stack));
        if (stack.isEmpty()) {
            return null;
        }
        // Handle operators and identifier
        int best_idx = 0;
        int best_idx_prio = 0x00ff;
        // Fold Methods on variables. Currently, this will not work with the logic below.
        int sidx = 0;
        while (sidx < stack.size()) {
            ExpressionParserItem item = stack.get(sidx);
            if (item.token != null) {
                switch (item.token.type) {
                    case Identifier -> {
                        Expression ex = new Variable((String) item.token.value);
                        stack.set(sidx, new ExpressionParserItem(ex));
                    }
                    case Separator -> {
                        if (((Character) item.token.value) == '.') {
                            if (2 < best_idx_prio) {
                                best_idx = sidx;
                                best_idx_prio = 2;
                            }
                        }
                    }
                    case Operator -> {
                        Operator op = (Operator) item.token.value;
                        int prio = switch (op) {
                            case Not -> 3;
                            case And, Multiply, Divide, Modulus -> 5;
                            case Or, Plus, Minus -> 6;
                            case Less, LessEqual, Greater, GreaterEqual -> 9;
                            case Equal, NotEqual -> 10;
                            case Assign, AssignUndefined -> 16;
                        };
                        if (prio <= best_idx_prio) {
                            best_idx = sidx;
                            best_idx_prio = prio;
                        }
                    }
                    default -> Log.panic("Internal error");
                }
            }
            ++sidx;
        }
        if (best_idx_prio < 0x00ff) {
            final Operator op;
            var si = stack.get(best_idx);
            if (si.token != null && si.token.type == TokenType.Operator) {
                op = (Operator) si.token.value;
            } else {
                op = null;
            }
            if (op != null) {
                switch (op) {
                    case Divide
                    , And
                    , Or
                    , Plus
                    , Minus
                    , Less
                    , LessEqual
                    , Greater
                    , GreaterEqual
                    , Equal
                    , NotEqual
                    , Modulus
                    , Multiply -> {
                        if (fold_stack_at(
                                stack,
                                best_idx,
                                (le, re) -> new BinaryOperator(op, le, re)
                        )) {
                            return stack_to_expression(stack);
                        }
                    }
                    case AssignUndefined -> {
                        if (fold_stack_at(stack, best_idx, AssignUndefined::new)) {
                            return stack_to_expression(stack);
                        }
                    }
                    case Assign -> {
                        if (fold_stack_at(stack, best_idx, Assign::new)) {
                            return stack_to_expression(stack);
                        }
                    }
                    case Not -> {
                        if ((best_idx + 1) < stack.size()) {
                            stack.remove(best_idx);
                            ExpressionParserItem right = stack.remove(best_idx);
                            if (right.expression != null) {
                                stack.add(best_idx, new ExpressionParserItem(new Not(right.expression)));
                                return stack_to_expression(stack);
                            }
                        }
                    }
                }
                throw new ExpressionException(String.format("Failed to parse at operator '%s'", op));
            } else if (si.token != null && si.token.type == TokenType.Separator) {
                char sep_char = (Character) si.token.value;
                if (best_idx > 0 && (best_idx + 1) < stack.size()) {
                    try {
                        fold_stack_at(stack, best_idx,
                                (le, re) -> {
                                    if (re instanceof Variable variable) {
                                        Expression r = new Index(le, new Constant(new Data.String(variable.name)));
                                        if (StaticOptions.debug) {
                                            Log.debug("Resulting expression: %s", r);
                                            if (!stack.isEmpty())
                                                Log.debug("Remaining stack: %s", stack);
                                        }
                                        return r;
                                    }
                                    if (re instanceof Method method) {
                                        Method method_copy = new Method(method.method, new ArrayList<>(method.arguments));
                                        method_copy.arguments.add(0, le);
                                        if (StaticOptions.debug) {
                                            Log.debug("Resulting expression: %s", method_copy);
                                            if (!stack.isEmpty())
                                                Log.debug("Remaining stack: %s", stack);
                                        }
                                        return method_copy;
                                    } else {
                                        throw new RuntimeException("No Field/Method on right side of '.'");
                                    }
                                });
                    } catch (RuntimeException re) {
                        throw new ExpressionException(re.getMessage());
                    }
                    return stack_to_expression(stack);
                } else {
                    throw new ExpressionException(String.format("Failed to parse at '%s'", sep_char));
                }
            }
        } else {
            var x = stack.remove(0);
            if (x.expression != null) {
                if (StaticOptions.debug) {
                    Log.debug("Resulting expression: %s", x.expression);
                    if (!stack.isEmpty())
                        Log.debug("Remaining stack: %s", stack);
                }

                // No operator? Return first one.
                return x.expression;
            } else {
                throw new ExpressionException(String.format("Failed to parse at '%s'", x));
            }
        }
        throw new ExpressionException("Failed to parse");
    }

    public static String toString(List<?> list) {
        StringBuilder sb = new StringBuilder(100);
        boolean first = true;
        sb.append('[');
        for (Object o : list) {
            if (first)
                first = false;
            else
                sb.append(',');
            sb.append(o);
        }
        sb.append(']');
        return sb.toString();
    }
}