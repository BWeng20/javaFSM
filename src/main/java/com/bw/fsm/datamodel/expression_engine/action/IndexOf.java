package com.bw.fsm.datamodel.expression_engine.action;

import com.bw.fsm.Data;
import com.bw.fsm.DataType;
import com.bw.fsm.Log;
import com.bw.fsm.StaticOptions;
import com.bw.fsm.actions.Action;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expressionEngine.ExpressionException;

import java.util.List;

public class IndexOf implements Action {
    @Override
    public Data execute(List<Data> arguments, GlobalData global) throws ExpressionException {
        if (arguments.size() == 2) {
            Data a1 = arguments.get(0);
            Data a2 = arguments.get(1);
            if (a1.type == DataType.String && a2.type == DataType.String) {
                int r = a1.toString().indexOf(a2.toString());
                if (StaticOptions.debug)
                    Log.debug("indexOf(%s,%s) -> %s", a1, a2, r);
                return new Data.Integer(r);
            } else {
                throw new ExpressionException("Illegal argument types for 'indexOf'");
            }
        } else {
            throw new ExpressionException("Wrong number of arguments for 'indexOf'.");
        }
    }
}
