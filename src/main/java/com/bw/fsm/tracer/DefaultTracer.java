package com.bw.fsm.tracer;

import com.bw.fsm.Log;
import com.bw.fsm.StaticOptions;

import java.util.HashSet;
import java.util.Set;

public class DefaultTracer extends Tracer {

    protected Set<TraceMode> trace_flags = new HashSet<>();

    private static ThreadLocal<Integer> trace_indent = ThreadLocal.withInitial(() -> 1);

    public static int get_indent() {
        return trace_indent.get();
    }

    public static void increase_indent() {
        trace_indent.set(trace_indent.get() + 1);
    }

    public static void decrease_indent() {
        trace_indent.set(trace_indent.get() - 1);
    }

    @Override
    public void trace(int sessionId, String msg) {
        if (StaticOptions.trace) {
            Log.info("Trace #%d >%" + get_indent() + "s%s", sessionId, "", msg);
        }
    }

    @Override
    public void enter_method(int sessionId, String what, TraceArgument... arguments) {
        if (this.is_trace(TraceMode.METHODS)) {
            final StringBuilder sb = new StringBuilder(30);
            sb.append(">>> ").append(what).append('(');
            if (arguments.length > 0) {
                boolean first = true;
                for (TraceArgument a : arguments) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(a.name).append(':').append(value_to_string(a.value));
                }
            }
            sb.append(')');
            this.trace(sessionId, sb.toString());
            DefaultTracer.increase_indent();
        }
    }

    @Override
    public void exit_method(int sessionId, String what, TraceArgument... results) {
        if (this.is_trace(TraceMode.METHODS)) {
            this.leave();
            final StringBuilder sb = new StringBuilder(30);
            sb.append("<<< ").append(what);
            if (results.length > 0) {
                sb.append(" -> ");
                if (results.length > 1)
                    sb.append('[');
                boolean first = true;
                for (TraceArgument a : results) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(a.name).append(':').append(value_to_string(a.value));
                }
                if (results.length > 1)
                    sb.append(']');
            }
            this.trace(sessionId, sb.toString());
        }
    }

    protected void leave() {
        if (StaticOptions.trace) {
            int prefix = DefaultTracer.get_indent();
            if (prefix > 2) {
                DefaultTracer.decrease_indent();
            }
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
        return StaticOptions.trace && (this.trace_flags.contains(flag) || this.trace_flags.contains(TraceMode.ALL));
    }

}