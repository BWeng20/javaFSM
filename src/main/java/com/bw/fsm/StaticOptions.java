package com.bw.fsm;

public interface StaticOptions {

    boolean debug = true;
    boolean debug_reader = false;

    boolean trace = true;
    boolean trace_method = trace;
    boolean trace_event = trace;
    boolean trace_state = trace;
    boolean trace_script = trace;

    boolean ecma_script_model = true;
    boolean rfsm_expression_model = true;

}
