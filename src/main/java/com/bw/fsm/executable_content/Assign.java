package com.bw.fsm.executable_content;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

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
    public void trace(ExecutableContentTracer tracer, Fsm fsm) {
        tracer.print_name_and_attributes(
                this,
                new String[][]{
                        {"location", this.location.toString()},
                        {"expr", this.expr.toString()}});
    }
}
