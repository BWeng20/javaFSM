package com.bw.fsm.tracer;

/**
 * Lightweight container for argument descriptions
 */
public class Argument {

    public final String name;
    public final Object value;

    public Argument(String name, Object o) {
        this.name = name;
        this.value = o;
    }
}
