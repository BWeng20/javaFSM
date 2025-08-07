package com.bw.fsm.tracer;

import com.bw.fsm.Log;

import java.util.HashSet;
import java.util.Set;

public class DefaultTracer extends Tracer {

    protected Set<TraceMode> trace_flags = new HashSet<>();

    private static ThreadLocal<String> trace_prefix = ThreadLocal.withInitial(() -> "");

    public static String get_prefix() {
        return trace_prefix.get();
    }

    public static void set_prefix(String p) {
        trace_prefix.set(p);
    }

    @Override
    public void trace(String msg) {
        Log.info("%s%s", DefaultTracer.get_prefix(), msg);
    }

    @Override
    public void enter() {
        DefaultTracer.set_prefix(DefaultTracer.get_prefix() + " ");
    }

    @Override
    public void leave() {
        String prefix = DefaultTracer.get_prefix();
        if (!prefix.isEmpty()) {
            DefaultTracer.set_prefix(prefix.substring(1));
        }
    }

    @Override
    public void enable_trace(TraceMode flag) {
        this.trace_flags.add(flag);
    }

    @Override
    public void disable_trace(TraceMode flag) {
        this.trace_flags.remove(flag);
    }

    @Override
    public boolean is_trace(TraceMode flag) {
        return this.trace_flags.contains(flag) || this.trace_flags.contains(TraceMode.ALL);
    }

    @Override
    public TraceMode trace_mode() {
        if (is_trace(TraceMode.ALL)) {
            return TraceMode.ALL;
        } else if (is_trace(TraceMode.EVENTS)) {
            return TraceMode.EVENTS;
        } else if (this.is_trace(TraceMode.STATES)) {
            return TraceMode.STATES;
        } else if (is_trace(TraceMode.METHODS)) {
            return TraceMode.METHODS;
        } else {
            return TraceMode.NONE;
        }
    }
}
