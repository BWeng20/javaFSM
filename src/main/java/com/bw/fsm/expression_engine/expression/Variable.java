package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;

public class Variable implements Expression {

    public final String name;

    public Variable(String name) {
        this.name = name;
    }

    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data value = context.data.get(this.name);
        if (value == null) {
            if (allow_undefined) {
                context.data.put(this.name, Data.None.NONE);
                return Data.None.NONE;
            } else {
                throw new ExpressionException(String.format("Variable '%s' not found", this.name));
            }
        } else {
            return value;
        }
    }

    @Override
    public void assign(GlobalData context, Data data) throws ExpressionException {
        Data r = context.data.get(this.name);
        if (r != null && r.is_readonly())
            throw new ExpressionException(String.format("Can't set read-only %s", r));

        context.data.put(this.name, data);
    }

    @Override
    public String toString() {
        return name;
    }


}
