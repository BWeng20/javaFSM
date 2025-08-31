package com.bw.fsm.expressionEngine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.DataType;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expressionEngine.Expression;
import com.bw.fsm.expressionEngine.ExpressionException;

public class Not implements Expression {
    public Expression right;

    public Not(Expression right) {
        this.right = right;
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data r = right.execute(context, allow_undefined);

        if (r.type == DataType.Boolean) {
            return Data.Boolean.fromBoolean(!((Data.Boolean) r).value);
        } else {
            throw new ExpressionException("'!' can only be applied on boolean expressions.");
        }
    }

    @Override
    public String toString() {
        return "!(" + right + ")";
    }
}
