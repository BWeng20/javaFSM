package com.bw.fsm.expressionEngine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.DataType;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expressionEngine.Expression;
import com.bw.fsm.expressionEngine.ExpressionException;
import org.jetbrains.annotations.NotNull;

/**
 * Sub expression to access an element of a Map- or Array-data-instance.
 */
public class Index implements Expression {

    public final @NotNull Expression left;
    public final @NotNull Expression index;

    public Index(@NotNull Expression left, @NotNull Expression index) {
        this.left = left;
        this.index = index;
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data left_result = this.left.execute(context, allow_undefined);
        Data index_result = this.index.execute(context, allow_undefined);

        if (left_result.type == DataType.Error || index_result.type == DataType.Error) {
            throw new ExpressionException("Error result");
        }
        switch (left_result.type) {
            case Integer, Double, Boolean, Source, Null, None ->
                    throw new ExpressionException(String.format("Can't apply index on '%s'", left_result));
            case String -> {
                Data.String string = (Data.String) left_result;
                if (index_result.is_numeric()) {
                    final int idx = index_result.as_number().intValue();
                    if (idx < 0 || idx >= string.value.length())
                        throw new ExpressionException(String.format("Index not found: %s (len=%d)", index, string.value.length()));
                    return new Data.String("" + string.value.charAt(idx));
                } else {
                    throw new ExpressionException(String.format("Illegal index type '%s' for strings", index_result));
                }
            }
            case Map -> {
                String key = index_result.toString();
                Data.Map m = (Data.Map) left_result;
                Data member = m.values.get(key);
                if (member == null) {
                    if (allow_undefined) {
                        m.values.put(key, Data.None.NONE);
                        return Data.None.NONE;
                    } else {
                        throw new ExpressionException(String.format("Index '%s' not found", key));
                    }
                } else {
                    return member;
                }
            }
            case Array -> {
                Data.Array array = (Data.Array) left_result;
                if (index_result.is_numeric()) {
                    final int idx = index_result.as_number().intValue();
                    if (idx < 0 || idx >= array.values.size())
                        throw new ExpressionException(String.format("Index not found: %s (len=%d)", index, array.values.size()));
                    return array.values.get(idx);
                } else {
                    throw new ExpressionException(String.format("Illegal index type '%s'", index_result));
                }
            }
            default ->
                    throw new ExpressionException(String.format("Internal Error. Unexpected Data type %s in index expression", left_result.type));
        }
    }

    @Override
    public void assign(GlobalData context, Data data) throws ExpressionException {
        Data left_result = this.left.execute(context, true);
        Data index_result = this.index.execute(context, true);

        switch (left_result.type) {
            case Integer, Double, String, Boolean, Source, Null, None ->
                    throw new ExpressionException(String.format("Can't apply index on '%s'", left_result));
            case Map -> {
                if (left_result.is_readonly())
                    throw new ExpressionException(String.format("Can't set member of read-only %s", left_result));
                String key = index_result.toString();
                Data.Map m = (Data.Map) left_result;
                Data r = m.values.get(key);
                if (r != null && r.is_readonly())
                    throw new ExpressionException(String.format("Can't set read-only %s", r));
                m.values.put(key, data);
            }
            case Array -> {
                Data.Array array = (Data.Array) left_result;
                if (array.is_readonly())
                    throw new ExpressionException(String.format("Can't set item of read-only %s", left_result));
                if (index_result.is_numeric()) {
                    final int idx = index_result.as_number().intValue();
                    if (idx < 0 || idx > array.values.size())
                        throw new ExpressionException(String.format("Index not found: %s (len=%d)", index, array.values.size()));
                    if (idx < array.values.size()) {
                        array.values.set(idx, data);
                    } else
                        array.values.add(idx, data);
                } else {
                    throw new ExpressionException(String.format("Illegal index type '%s'", index_result));
                }
            }
            default ->
                    throw new ExpressionException(String.format("Internal Error. Unexpected Data type %s in index expression", left_result.type));
        }

    }

    @Override
    public String toString() {
        return left + " index " + index;
    }

}
