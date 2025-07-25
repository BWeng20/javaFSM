package com.bw.fsm.executable_content;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

public class Expression implements ExecutableContent {
    public Data content = new Data.Source("");

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        Data r = datamodel.execute(this.content);
        return r != null;
    }

    @Override
    public int get_type() {
        return TYPE_EXPRESSION;
    }

    @Override
    public void trace(ExecutableContentTracer tracer, Fsm fsm) {
        tracer.print_name_and_attributes(this, new String[][]
                {{"content", content.toString()}});
    }
}
