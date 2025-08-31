package com.bw.fsm.executableContent;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

import java.util.Map;

public class Log implements ExecutableContent {

    public String label;
    public Data expression;

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        Data msg = datamodel.execute(this.expression);
        if (msg != null) {
            datamodel.log(msg.toString());
            return true;
        } else {
            return false;

        }
    }

    @Override
    public int get_type() {
        return TYPE_LOG;
    }

    @Override
    public Map<String, Object> get_trace() {
        return ExecutableContent.toMap("expression", this.expression);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(50);
        stringBuilder.append("Log {");
        if (label != null)
            stringBuilder.append('#').append(label).append(' ');
        if (expression != null)
            stringBuilder.append(expression);
        return stringBuilder.append('}').toString();
    }

    public Log(String label, Data expression) {
        this.label = label;
        this.expression = expression;
    }
}
