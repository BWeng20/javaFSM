package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;

public class Constant implements Expression {
    public final Data data;

    public Constant(Data data) {
        this.data = data;
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) {
        return data.getCopy();
    }


    @Override
    public String toString() {
        return "const <" + data.toString() + ">";
    }
}
