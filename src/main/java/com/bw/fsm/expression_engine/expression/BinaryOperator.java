package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.DataType;
import com.bw.fsm.Log;
import com.bw.fsm.StaticOptions;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binary Operator expressions.
 */
public class BinaryOperator implements Expression {

    public com.bw.fsm.expression_engine.Operator operator;
    public Expression left;
    public Expression right;

    public BinaryOperator(
            com.bw.fsm.expression_engine.Operator op,
            Expression left,
            Expression right) {
        this.operator = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data left_result = left.execute(context, allow_undefined);
        Data right_result = right.execute(context, allow_undefined);
        if (StaticOptions.debug)
            Log.debug(
                    "ExpressionOperator::execute: <%s=%s> %s <%s=%s>",
                    left, left_result, operator, right, right_result);

        return operation(left_result, this.operator, right_result);
    }

    @Override
    public String toString() {
        return  '(' + left.toString() +
                ' ' + operator + ' ' + right.toString() +
                ')';
    }


    public static Data operation(Data left, com.bw.fsm.expression_engine.Operator op, Data right) throws ExpressionException {
        return switch (op) {
            case Multiply -> operation_multiply(left, right);
            case Divide -> operation_divide(left, right);
            case Plus -> operation_plus(left, right);
            case Minus -> operation_minus(left, right);
            case Less -> operation_less(left, right);
            case LessEqual -> operation_less_equal(left, right);
            case Greater -> operation_greater(left, right);
            case GreaterEqual -> operation_greater_equal(left, right);
            case And -> operation_and(left, right);
            case Or -> operation_or(left, right);
            case Equal -> operation_equal(left, right);
            case NotEqual -> operation_not_equal(left, right);
            case Modulus -> operation_modulus(left, right);
            case Assign, AssignUndefined, Not ->
                // These "operation" are handled by explicit Expression-implementations
                // and this line should never be reached.
                    throw new ExpressionException("Internal Error");
        };
    }

    /// Implements a "+" operation on Data items.
    public static Data operation_plus(@NotNull Data left, @NotNull Data right) {
        if (left.is_numeric() && right.is_numeric()) {
            if (left.type == DataType.Double || right.type == DataType.Double) {
                return new Data.Double(left.as_number().doubleValue() + right.as_number().doubleValue());
            } else {
                return new Data.Integer(left.as_number().intValue() + right.as_number().intValue());
            }
        } else if (left.type == DataType.String) {
            return new Data.String(((Data.String) left).value + right);
        } else if (left.type == DataType.Source) {
            return new Data.Source(left.toString() + right);
        } else if (left.type == DataType.Boolean && right.type == DataType.Boolean) {
            return Data.Boolean.fromBoolean(((Data.Boolean) left).value && ((Data.Boolean) right).value);
        } else if (left.type == DataType.Array) {
            Data.Array leftArray = (Data.Array) left;
            if (right.type == DataType.Array) {
                Data.Array rightArray = (Data.Array) right;
                List<Data> v = new ArrayList<>(leftArray.values.size() + rightArray.values.size());
                v.addAll(leftArray.values);
                v.addAll(rightArray.values);
                return new Data.Array(v);

            } else {
                List<Data> v = new ArrayList<>(leftArray.values.size() + 1);
                v.addAll(leftArray.values);
                v.add(right);
                return new Data.Array(v);
            }
        } else if (left.type == DataType.Map && right.type == DataType.Map) {
            Data.Map leftMap = (Data.Map) left;
            Data.Map rightMap = (Data.Map) right;
            Map<String, Data> m = new HashMap<>(leftMap.values.size() + rightMap.values.size());
            m.putAll(leftMap.values);
            m.putAll(rightMap.values);
            return new Data.Map(m);

        } else if (right.type == DataType.String) {
            // Fallback. Force String if right argument is string.
            return new Data.String(left.toString() + right);

        } else if (right.type == DataType.Source) {
            // Fallback. Force Source if right argument is Source .
            return new Data.Source(left.toString() + right);
        } else {
            return new Data.Error("Incompatible argument types for '+'");
        }
    }

    /// Implements a "&" operation on Data items.
    public static Data operation_and(@NotNull Data left, @NotNull Data right) {
        if (left.type == DataType.Error || right.type == DataType.Error) {
            return left.type == DataType.Error ? left : right;
        }
        if (left.type == DataType.Boolean && right.type == DataType.Boolean) {
            return Data.Boolean.fromBoolean(((Data.Boolean) left).value && ((Data.Boolean) right).value);
        }

        return new Data.Error("Wrong argument types for '&'");

    }

    /// Implements a "|" operation on Data items.
    public static Data operation_or(@NotNull Data left, @NotNull Data right) {
        if (left.type == DataType.Error || right.type == DataType.Error) {
            return left.type == DataType.Error ? left : right;
        }
        if (left.type == DataType.Boolean && right.type == DataType.Boolean) {
            return Data.Boolean.fromBoolean(((Data.Boolean) left).value || ((Data.Boolean) right).value);
        }

        return new Data.Error("Wrong argument types for '|'");

    }

    /// Implements a "-" operation on Data items.
    public static Data operation_minus(@NotNull Data left, @NotNull Data right) {

        if (left.is_numeric() && right.is_numeric()) {
            if (left.type == DataType.Double || right.type == DataType.Double) {
                return new Data.Double(left.as_number().doubleValue() - right.as_number().doubleValue());
            } else {
                return new Data.Integer(left.as_number().intValue() - right.as_number().intValue());
            }
        } else if (left.type == DataType.Error || right.type == DataType.Error) {
            return left.type == DataType.Error ? left : right;
        } else {
            return new Data.Error("Incompatible argument types for '-'");
        }
    }

    /// Implements a "*" operation on Data items.
    public static Data operation_multiply(@NotNull Data left, @NotNull Data right) throws ExpressionException {
        if (left.is_numeric() && right.is_numeric()) {
            if (left.type == DataType.Double || right.type == DataType.Double) {
                return new Data.Double(left.as_number().doubleValue() * right.as_number().doubleValue());
            } else {
                return new Data.Integer(left.as_number().intValue() * right.as_number().intValue());
            }
        } else {
            throw new ExpressionException("Wrong argument types for '*'");
        }
    }

    /// Implements a ":" operation on Data items.
    public static Data operation_divide(@NotNull Data left, @NotNull Data right) throws ExpressionException {
        if (left.is_numeric() && right.is_numeric()) {
            if (left.type == DataType.Double || right.type == DataType.Double) {
                double r = left.as_number().doubleValue() / right.as_number().doubleValue();
                if (Double.isNaN(r))
                    throw new ExpressionException("Result of '/' is NaN\"");
                return new Data.Double(r);
            } else {
                return new Data.Integer(left.as_number().intValue() / right.as_number().intValue());
            }
        } else {
            throw new ExpressionException("Wrong argument types for '/'");
        }
    }

    /// Implements a "%" modulus (remainder) operation on Data items.
    public static Data operation_modulus(@NotNull Data left, @NotNull Data right) throws ExpressionException {
        if (left.is_numeric() && right.is_numeric()) {
            if (left.type == DataType.Double || right.type == DataType.Double) {
                return new Data.Double(left.as_number().doubleValue() % right.as_number().doubleValue());
            }
            return new Data.Integer(left.as_number().intValue() % right.as_number().intValue());
        } else {
            throw new ExpressionException("Wrong argument types for '%'");
        }
    }


    /// Implements a "<" (less) operation on Data items.
    public static Data operation_less(@NotNull Data left, @NotNull Data right) {
        if (left.is_numeric() && right.is_numeric()) {
            return Data.Boolean.fromBoolean(left.as_number().doubleValue() < right.as_number().doubleValue());
        } else {
            if (left.type == DataType.String || left.type == DataType.Source) {
                String leftValue = left.toString();
                if (right.type == DataType.String || right.type == DataType.Source) {
                    String rightValue = right.toString();
                    return Data.Boolean.fromBoolean(leftValue.compareTo(rightValue) < 0);
                }
            }
            if (StaticOptions.debug)
                Log.warn("'<' supports only numeric or string types");
            return Data.Boolean.FALSE;
        }
    }

    /// Implements a "<=" (less or equal) operation on Data items.
    public static Data operation_less_equal(@NotNull Data left, @NotNull Data right) {
        if (left.is_numeric() && right.is_numeric()) {
            return Data.Boolean.fromBoolean(left.as_number().doubleValue() <= right.as_number().doubleValue());
        } else {
            if (left.type == DataType.String || left.type == DataType.Source) {
                String leftValue = left.toString();
                if (right.type == DataType.String || right.type == DataType.Source) {
                    String rightValue = right.toString();
                    return Data.Boolean.fromBoolean(leftValue.compareTo(rightValue) <= 0);
                }
            }
            if (StaticOptions.debug)
                Log.warn("'<=' supports only numeric or string types");
            return Data.Boolean.FALSE;
        }
    }

    /// Implements a ">" (greater) operation on Data items.
    public static Data operation_greater(@NotNull Data left, @NotNull Data right) {
        if (left.is_numeric() && right.is_numeric()) {
            return Data.Boolean.fromBoolean(left.as_number().doubleValue() > right.as_number().doubleValue());
        } else {
            if (left.type == DataType.String || left.type == DataType.Source) {
                String leftValue = left.toString();
                if (right.type == DataType.String || right.type == DataType.Source) {
                    String rightValue = right.toString();
                    return Data.Boolean.fromBoolean(leftValue.compareTo(rightValue) > 0);
                }
            }
            if (StaticOptions.debug)
                Log.warn("'>' supports only numeric or string types");
            return Data.Boolean.FALSE;
        }
    }

    /// Implements a ">=" (greater or equal) operation on Data items.
    public static Data operation_greater_equal(@NotNull Data left, @NotNull Data right) {
        if (left.is_numeric() && right.is_numeric()) {
            return Data.Boolean.fromBoolean(left.as_number().doubleValue() >= right.as_number().doubleValue());
        } else {
            if (left.type == DataType.String || left.type == DataType.Source) {
                String leftValue = left.toString();
                if (right.type == DataType.String || right.type == DataType.Source) {
                    String rightValue = right.toString();
                    return Data.Boolean.fromBoolean(leftValue.compareTo(rightValue) >= 0);
                }
            }
            if (StaticOptions.debug)
                Log.warn("'>=' supports only numeric or string types");
            return Data.Boolean.FALSE;
        }
    }

    /// Implements a "==" (equal) operation on Data items.
    public static Data operation_equal(@NotNull Data left, Data right) {
        return Data.Boolean.fromBoolean(left.equals(right));
    }

    /// Implements a "!=" (not equal) operation on Data items.
    public static Data operation_not_equal(@NotNull Data left, Data right) {
        return Data.Boolean.fromBoolean(!left.equals(right));
    }

}
