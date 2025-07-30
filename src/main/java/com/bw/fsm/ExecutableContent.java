package com.bw.fsm;

import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.executable_content.ExecutableContentTracer;

public interface ExecutableContent {
    boolean execute(Datamodel datamodel, Fsm fsm);

    /**
     * Returns the matching "TYPE_XXX".
     */
    int get_type();

    void trace(ExecutableContentTracer tracer, Fsm fsm);

    String TARGET_SCXML_EVENT_PROCESSOR = "http://www.w3.org/TR/scxml/#SCXMLEventProcessor";

    int TYPE_IF = 0;
    int TYPE_EXPRESSION = 1;
    int TYPE_SCRIPT = 2;
    int TYPE_LOG = 3;
    int TYPE_FOREACH = 4;
    int TYPE_SEND = 5;
    int TYPE_RAISE = 6;
    int TYPE_CANCEL = 7;
    int TYPE_ASSIGN = 8;

    String[] TYPE_NAMES = {
            "if",
            "expression",
            "script",
            "log",
            "foreach",
            "send",
            "raise",
            "cancel",
            "assign"
    };

}
