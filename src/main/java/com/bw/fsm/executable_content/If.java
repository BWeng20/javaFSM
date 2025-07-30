package com.bw.fsm.executable_content;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.ExecutableContentRegion;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

public class If implements ExecutableContent {
    public Data condition;
    public ExecutableContentRegion content;
    public ExecutableContentRegion else_content;

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        if (datamodel.execute_condition(this.condition)) {
            if (this.content != null) {
                for (ExecutableContent e : this.content.content) {
                    if (!e.execute(datamodel, fsm)) {
                        return false;
                    }
                }
            }
        } else if (this.else_content != null) {
            for (ExecutableContent e : this.else_content.content) {
                if (!e.execute(datamodel, fsm)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int get_type() {
        return TYPE_IF;
    }

    public If(Data condition) {
        this.condition = condition;
    }

    @Override
    public void trace(ExecutableContentTracer tracer, Fsm fsm) {
        tracer.print_name_and_attributes(this, new String[][]
                {{"condition", this.condition.toString()}});
        tracer.print_sub_content("then", fsm, this.content);
        tracer.print_sub_content("else", fsm, this.else_content);
    }

    @Override
    public String toString() {
        return "If [" + condition + "] then [" + content + "]";
    }
}
