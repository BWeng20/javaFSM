package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;
import com.bw.fsm.expression_engine.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Expression that represents a map or object like structure.<br>
 * Keys and Values are expressions.
 */
public class Map implements Expression {


    public final List<Pair> map;

    public Map() {
        map = new ArrayList<>();
    }

    /**
     * Creates an instance from the list.
     */
    public Map(List<Pair> data) {
        map = data;
    }


    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {

        java.util.Map<String, Data> v = new HashMap<>(map.size());
        for (Pair item : map) {
            Data keyValue = item.key.execute(context, allow_undefined);
            Data value = item.value.execute(context, allow_undefined);

            v.put(keyValue.toString(), value);
        }
        return new Data.Map(v);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        boolean first = true;
        sb.append('[');
        for (Pair e : map) {
            if (first)
                first = false;
            else
                sb.append(',');
            sb.append(e);
        }
        sb.append(']');
        return sb.toString();
    }
}
