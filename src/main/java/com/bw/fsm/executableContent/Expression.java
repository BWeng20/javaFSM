package com.bw.fsm.executableContent;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

import java.util.Map;

public class Expression implements ExecutableContent {
    public Data content = new Data.Source("");

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        datamodel.execute(this.content);
        return true;
    }

    @Override
    public int get_type() {
        return TYPE_EXPRESSION;
    }


    @Override
    public Map<String, Object> get_trace() {
        return ExecutableContent.toMap("content", content);
    }

    @Override
    public String toString() {
        return "Expression {" + content + "}";
    }
}
