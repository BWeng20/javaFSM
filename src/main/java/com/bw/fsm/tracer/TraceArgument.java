package com.bw.fsm.tracer;

/**
 * Lightweight container for argument descriptions
 */
public class TraceArgument {

    public final String name;
    public final Object value;

    public TraceArgument(String name, Object o) {
        this.name = name;
        this.value = o;
    }
}
