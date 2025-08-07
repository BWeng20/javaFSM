package com.bw.fsm.tracer;

public class DefaultTracerFactory implements TracerFactory {

    @Override
    public Tracer create() {
        return new DefaultTracer();
    }
}
