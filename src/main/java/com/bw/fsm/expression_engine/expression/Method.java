package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;

import java.util.ArrayList;
import java.util.List;

public class Method implements Expression {

    public String method;
    public List<Expression> arguments;

    public Method(String method, List<Expression> arguments) {
        this.method = method;
        this.arguments = arguments;
    }

    public Data execute_with_arguments(
            List<Data> arguments, GlobalData context) {
        return context.actions.execute(method, arguments, context);
    }

    public void eval_arguments(List<Data> v, GlobalData context) throws ExpressionException {
        for (var arg : this.arguments) {
            v.add(arg.execute(context, false));
        }
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        final List<Data> v = new ArrayList<>(this.arguments.size());
        eval_arguments(v, context);
        return execute_with_arguments(v, context);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(method).append('(');
        boolean first = true;
        for (Expression e : arguments) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(e);
        }
        return sb.append(')').toString();
    }


}
