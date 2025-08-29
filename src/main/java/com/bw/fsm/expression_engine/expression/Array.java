package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;

import java.util.ArrayList;
import java.util.List;

public class Array implements Expression {

    public final List<Expression> array;

    public Array() {
        array = new ArrayList<>();
    }

    public Array(List<Expression> list) {
        array = list;
    }

    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        List<Data> v = new ArrayList<>(array.size());
        for (var item : this.array) {
            Data val = item.execute(context, allow_undefined);
            v.add(val);
        }
        return new Data.Array(v);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        boolean first = true;
        sb.append('[');
        for (Expression e : array) {
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
