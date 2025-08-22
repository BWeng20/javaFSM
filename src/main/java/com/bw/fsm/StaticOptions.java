package com.bw.fsm;

public interface StaticOptions {

    boolean debug_option = false;


    boolean trace = true;
    boolean trace_method = trace;
    boolean trace_event = trace;
    boolean trace_state = trace;
    boolean trace_script = trace;

    boolean ecma_script_model = true;
}
