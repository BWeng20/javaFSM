package com.bw.fsm;

public interface StaticOptions {

    boolean debug = true;

    /**
     * Enables {@link com.bw.fsm.tracer.Tracer}
     */
    boolean trace = true;
    boolean trace_method = trace;
    boolean trace_event = trace;
    boolean trace_state = trace;
    boolean trace_script = trace;

    /**
     * Debug output for {@link ScxmlReader}
     */
    boolean debug_reader = false;

    /**
     * Debug output for {@link com.bw.fsm.serializer.FsmReader} and {@link com.bw.fsm.serializer.FsmWriter}.
     */
    boolean debug_serializer = false;
}
