package com.bw.fsm.executableContent;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

import java.util.Map;

public class Assign implements ExecutableContent {
    public Data location = Data.None.NONE;
    public Data expr = Data.None.NONE;

    @Override
    public String toString() {
        return String.format("Assign {location %s, expr %s}", location, expr);
    }

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        return datamodel.assign(this.location, this.expr);
    }

    @Override
    public int get_type() {
        return TYPE_ASSIGN;
    }

    @Override
    public Map<String, Object> get_trace() {
        return ExecutableContent.toMap("location", String.valueOf(this.location),
                "expr", String.valueOf(this.expr));
    }
}
