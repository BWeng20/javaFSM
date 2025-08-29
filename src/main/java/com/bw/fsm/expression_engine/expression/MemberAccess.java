package com.bw.fsm.expression_engine.expression;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;

import java.util.Map;

public class MemberAccess implements Expression {
    public Expression left;
    public String member_name;

    public MemberAccess(Expression left, String member_name) {
        this.left = left;
        this.member_name = member_name;
    }

    @Override
    public Data execute(GlobalData context, boolean allow_undefined) throws ExpressionException {
        Data data = left.execute(context, allow_undefined);
        switch (data.type) {
            case Fsm, Integer, Double, String, Boolean, Array, Source, Null, None ->
                    throw new ExpressionException(String.format("Value '%s' has no members", data));
            case Map -> {
                Map<String, Data> m = ((Data.Map) data).values;
                Data r = m.get(this.member_name);
                if (r == null) {
                    if (allow_undefined) {
                        m.put(member_name, Data.None.NONE);
                        return Data.None.NONE;
                    } else {
                        throw new ExpressionException(String.format("Member %s not found", this.member_name));
                    }
                }
                return r;
            }
            case Error -> throw new ExpressionException(data.toString());
            default -> throw new ExpressionException("Internal Error");
        }
    }

    @Override
    public void assign(GlobalData context, Data data) throws ExpressionException {
        Data leftResult = left.execute(context, true);
        switch (leftResult.type) {
            case Fsm, Integer, Double, String, Boolean, Array, Source, Null, None ->
                    throw new ExpressionException(String.format("Value '%s' has no members", data));
            case Map -> {
                if (data.is_readonly())
                    throw new ExpressionException(String.format("Can't set member of read-only %s", data));
                Map<String, Data> m = ((Data.Map) leftResult).values;
                Data r = m.get(this.member_name);
                if (r != null && r.is_readonly())
                    throw new ExpressionException(String.format("Can't set read-only %s", r));
                m.put(this.member_name, data);
            }
            case Error -> throw new ExpressionException(data.toString());
            default -> throw new ExpressionException("Internal Error");
        }
    }

    @Override
    public String toString() {
        return left + "." + member_name;
    }

}
