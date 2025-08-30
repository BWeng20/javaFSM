package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;

import java.util.List;

public class Sequence implements Expression {

    public final List<Expression> expressions;

    public Sequence(List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data r = Data.None.NONE;
        for (Expression exp : expressions) {
            if (exp != null)
                r = exp.execute(context, allow_undefined);
        }
        return r;
    }

}
