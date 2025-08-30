package com.bw.fsm.datamodel.expression_engine.action;

import com.bw.fsm.Data;
import com.bw.fsm.actions.Action;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.ExpressionException;

import java.util.List;

public class Length implements Action {

    @Override
    public Data execute(List<Data> arguments, GlobalData global) throws Exception {
        if (arguments.size() == 1) {
            Data a1 = arguments.get(0);
            int r = switch (a1.type) {
                case Source, String -> a1.toString().length();
                case Array -> ((Data.Array) a1).values.size();
                case Map -> ((Data.Map) a1).values.size();
                default -> throw new ExpressionException("Wrong argument type for 'length'.");
            };
            return new Data.Integer(r);
        } else {
            throw new ExpressionException("Wrong number of arguments for 'length'.");
        }
    }
}
