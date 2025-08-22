package com.bw.fsm.executable_content;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.ExecutableContentBlock;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

import java.util.Map;

public class If implements ExecutableContent {
    public Data condition;
    public ExecutableContentBlock content;
    public ExecutableContentBlock else_content;

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
    public Map<String, Object> get_trace() {
        return ExecutableContent.toMap("condition", this.condition,
                "then", this.content,
                "else", this.else_content);
    }

    @Override
    public String toString() {
        return "If [" + condition + "] then [" + content + "]";
    }
}
