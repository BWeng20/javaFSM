package com.bw.fsm.expression_engine;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;

/**
 * Interface for different kinds of (sub-)expressions.
 */
public interface Expression {

    Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException;

    default void assign(GlobalData context, Data data) throws ExpressionException {
        throw new ExpressionException("Can't assign to a " + getClass().getSimpleName());
    }

}
