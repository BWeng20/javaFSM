package com.bw.fsm.tracer;


import com.bw.ruFSM.thrift.EventProcessor;

public class ThriftTracer extends Tracer {

    EventProcessor e;

    @Override
    public void trace(String msg) {

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
    public void enter_method(int sessionId, String what) {

    }

    @Override
    public void enter_method_with_arguments(int sessionId, String what, Argument... arguments) {

    }

    @Override
    public void exit_method(int sessionId, String what) {

    }
}
