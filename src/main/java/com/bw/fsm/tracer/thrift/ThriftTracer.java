package com.bw.fsm.tracer.thrift;


import com.bw.fsm.thrift.TraceServer;
import com.bw.fsm.tracer.TraceArgument;
import com.bw.fsm.tracer.TraceMode;
import com.bw.fsm.tracer.Tracer;

public class ThriftTracer extends Tracer {

    TraceServer server;

    @Override
    public void trace(int session, String msg) {

    }


    @Override
    public void enable_trace(TraceMode flag) {

    }

    @Override
    public void disable_trace(TraceMode flag) {

    }

    @Override
    public boolean is_trace(TraceMode flag) {
        return false;
    }

    @Override
    public void enter_method(int sessionId, String what, TraceArgument... arguments) {

    }


    @Override
    public void exit_method(int sessionId, String what, TraceArgument... results) {

    }
}
