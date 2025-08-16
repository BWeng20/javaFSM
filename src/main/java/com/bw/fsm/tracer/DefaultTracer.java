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
    public void trace(String msg) {
        if (StaticOptions.trace) {
            Log.info("Trace>%" + get_indent() + "s%s", "", msg);
        }
    }

    @Override
    public void enter() {
        if (StaticOptions.trace) {
            DefaultTracer.increase_indent();
        }
    }

    @Override
    public void enter_method_with_arguments(String what, Argument... arguments) {
        if (this.is_trace(TraceMode.METHODS)) {
            this.trace(String.format(">>> %s", what));
            this.enter();
            if (arguments.length > 0) {
                this.trace("Arguments: {");
                DefaultTracer.increase_indent();
                for (Argument a : arguments) {
                    this.trace(String.format("%s = %s", a.name, value_to_string(a.value)));
                }
                DefaultTracer.decrease_indent();
                this.trace("}");
            }
        }
    }

    @Override
    public void leave() {
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