package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;

public class AssignUndefined implements Expression {

    public Expression left;
    public Expression right;

    public AssignUndefined(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data right_result = right.execute(context, allow_undefined);
        left.assign(context, right_result);
        return right_result;
    }

    @Override
    public String toString() {
        return left + " ?= " + right;
    }


}
