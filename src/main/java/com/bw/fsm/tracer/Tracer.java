package com.bw.fsm.tracer;

import com.bw.fsm.Event;
import com.bw.fsm.List;
import com.bw.fsm.OrderedSet;
import com.bw.fsm.State;

import java.util.stream.Collectors;

/**
 * Interface used to trace methods and
 * states inside the FSM. What is traced can be controlled by
 * {@link #enable_trace} and {@link #disable_trace}, see {@link TraceMode}.
 */
public abstract class Tracer {

    /**
     * Needed by a minimalistic implementation. Default methods below call this
     * Method with a textual representation of the trace-event.
     */
    public abstract void trace(String msg);

    /**
     * Enter a sub-scope, e.g. by increase the log indentation.
     */
    public abstract void enter();

    /**
     * Leave the current sub-scope, e.g. by decrease the log indentation.
     */
    public abstract void leave();

    /**
     * Enable traces for the specified scope.
     */
    public abstract void enable_trace(TraceMode flag);

    /**
     * Disable traces for the specified scope.
     */
    public abstract void disable_trace(TraceMode flag);

    /**
     * Return true if the given scape is enabled.
     */
    public abstract boolean is_trace(TraceMode flag);

    /**
     * Called by FSM if a method is entered
     */
    public void enter_method(String what) {
        if (this.is_trace(TraceMode.METHODS)) {
            this.trace(String.format(">>> %s", what));
            this.enter();
        }
    }

    /**
     * Called by FSM if a method is exited
     */
    public void exit_method(String what) {
        if (this.is_trace(TraceMode.METHODS)) {
            this.leave();
            this.trace(String.format("<<< %s", what));
        }
    }

    /**
     * Called by FSM if an internal event is sent
     */
    public void event_internal_send(Event what) {
        if (this.is_trace(TraceMode.EVENTS)) {
            this.trace(String.format("Send Internal Event: %s #%s", what.name, what.invoke_id));
        }
    }

    /**
     * Called by FSM if an internal event is received
     */
    public void event_internal_received(Event what) {
        if (this.is_trace(TraceMode.EVENTS)) {
            this.trace(String.format(
                            "Received Internal Event: %s, invokeId %s, content %s, param %s",
                            what.name, what.invoke_id, what.content, what.param_values
                    )
            );
        }
    }

    /**
     * Called by FSM if an external event is send
     */
    public void event_external_send(Event what) {
        if (this.is_trace(TraceMode.EVENTS)) {
            this.trace(String.format("Send External Event: %s #%s", what.name, what.invoke_id));
        }
    }

    /**
     * Called by FSM if an external event is received
     */
    public void event_external_received(Event what) {
        if (what.name.startsWith("trace.")) {
            var p = what.name.split("\\.");
            if (p.length == 3) {
                TraceMode t = TraceMode.fromString(p[1]);
                switch (p[2]) {
                    case "on", "ON", "On" -> this.enable_trace(t);
                    case "off", "OFF", "Off" -> this.disable_trace(t);
                    default -> this.trace(String.format("Trace event '%s' with illegal flag '%s'. Use 'On' or 'Off'.",
                            what.name, p[2]));

                }
            }
        }
        if (this.is_trace(TraceMode.EVENTS)) {
            this.trace(String.format("Received External Event: %s #%s", what.name, what.invoke_id));
        }
    }

    /**
     * Called by FSM if a state is entered or left.
     */
    public void trace_state(String what, State s) {
        if (this.is_trace(TraceMode.STATES)) {
            if (s.name.isEmpty()) {
                this.trace(String.format("%s #%d", what, s.id));
            } else {
                this.trace(String.format("%s <%s> #%d", what, s.name, s.id));
            }
        }
    }

    /**
     * Called by FSM if a state is entered. Calls [Tracer::trace_state].
     */
    public void trace_enter_state(State s) {
        this.trace_state("Enter", s);
    }

    /**
     * Called by FSM if a state is left. Calls [Tracer::trace_state].
     */
    public void trace_exit_state(State s) {
        this.trace_state("Exit", s);
    }

    /**
     * Called by FSM for input arguments in methods.
     */
    public void trace_argument(String what, Object d) {
        if (this.is_trace(TraceMode.ARGUMENTS)) {
            this.trace(String.format("Argument:%s=%s", what, value_to_string(d)));
        }
    }

    /**
     * Called by FSM for results in methods.
     */
    public void trace_result(String what, Object d) {
        if (this.is_trace(TraceMode.RESULTS)) {
            this.trace(String.format("Result:%s=%s", what, value_to_string(d)));
        }
    }

    /**
     * Helper method to trace a vector of objects.
     */
    public void trace_list(String what, List<?> l) {
        // @TODO
        this.trace(String.format("%s=[%s]", what, l.data));
    }

    /**
     * Helper method to trace a OrderedSet of ids.
     */
    public void trace_set(String what, OrderedSet<?> l) {
        // @TODO
        this.trace(String.format("%s=(%s)", what, l.data));
    }

    /**
     * Get trace mode
     */
    public abstract TraceMode trace_mode();

    private static TracerFactory tracer_factory = new DefaultTracerFactory();

    public static void set_tracer_factory(TracerFactory tracer_factory) {
        if (tracer_factory == null)
            tracer_factory = new DefaultTracerFactory();
        Tracer.tracer_factory = tracer_factory;
    }

    public static Tracer create_tracer() {
        return tracer_factory.create();
    }

    protected String value_to_string(Object d) {
        if (d instanceof OrderedSet<?> os) {
            return "[" + os.data.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
        } else if (d instanceof List<?> os) {
            return "{" + os.data.stream().map(String::valueOf).collect(Collectors.joining(",")) + "}";
        }
        return String.valueOf(d);
    }
}
