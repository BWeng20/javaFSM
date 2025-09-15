package com.bw.fsm.tracer.thrift;

import com.bw.fsm.tracer.Tracer;
import com.bw.fsm.tracer.TracerFactory;

public class ThriftTracerFactory implements TracerFactory {

    public static String serverAddress = "tcp:localhost:4711";
    public static String clientAddress = "tcp:localhost:4712";

    @Override
    public Tracer create() {
        return new ThriftTracer(serverAddress, clientAddress);
    }
}
