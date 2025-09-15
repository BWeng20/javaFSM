package com.bw.fsm.tracer;

import com.bw.fsm.*;

import java.util.stream.Collectors;

/**
 * Interface used to trace methods and
 * states inside the FSM. What is traced can be controlled by
 * {@link #enable_trace} and {@link #disable_trace}, see {@link TraceMode}.
 */
public abstract class Tracer {

    /**
     * Needs to be called before session is traces..
     */
    public abstract void registerSession(int session, String fsmName);

    public abstract void removeSession(int session);

    /**
     * Needed by a minimalistic implementation. Default methods below call this
     * Method with a textual representation of the trace-event.
     */
    public abstract void trace(int sessionId, String msg);

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
    public abstract void enter_method(int sessionId, String what, TraceArgument... arguments);


    /**
     * Called by FSM if a method is exited
     */
    public abstract void exit_method(int sessionId, String what, TraceArgument... arguments);

    /**
     * Called by FSM if an internal event is sent
     */
    public void event_internal_send(int sessionId, Event what) {
        if (StaticOptions.trace_event && this.is_trace(TraceMode.EVENTS)) {
            this.trace(sessionId, String.format("Send Internal Event: %s #%s", what.name, what.invoke_id));
        }
    }

    /**
     * Called by FSM if an internal event is received
     */
    public void event_internal_received(int sessionId, Event what) {
        if (StaticOptions.trace_event && this.is_trace(TraceMode.EVENTS)) {
            this.trace(sessionId, String.format(
                            "Received Internal Event: %s, invokeId %s, content %s, param %s",
                            what.name, what.invoke_id, what.content, what.param_values
                    )
            );
        }
    }

    /**
     * Called by FSM if an external event is sent
     */
    public void event_external_sent(int fromSessionId, int toSessionId, Event what) {
        if (StaticOptions.trace_event && this.is_trace(TraceMode.EVENTS)) {
            this.trace(fromSessionId, String.format("Send External Event: %s #%s to session #%d", what.name, what.invoke_id, toSessionId));
        }
    }

    /**
     * Called by FSM if an external event is received
     */
    public void event_external_received(int sessionId, Event what) {
        if (what.name.startsWith("trace.")) {
            var p = what.name.split("\\.");
            if (p.length == 3) {
                TraceMode t = TraceMode.fromString(p[1]);
                switch (p[2]) {
                    case "on", "ON", "On" -> this.enable_trace(t);
                    case "off", "OFF", "Off" -> this.disable_trace(t);
                    default ->
                            this.trace(sessionId, String.format("Trace event '%s' with illegal flag '%s'. Use 'On' or 'Off'.",
                                    what.name, p[2]));

                }
            }
        }
        if (StaticOptions.trace_event && this.is_trace(TraceMode.EVENTS)) {
            this.trace(sessionId, String.format("Received External Event: %s #%s", what.name, what.invoke_id));
        }
    }

    /**
     * Helper for default implementations.
     */
    protected void trace_state(int sessionId, String what, State s) {
        if (StaticOptions.trace_state && this.is_trace(TraceMode.STATES)) {
            if (s.name.isEmpty()) {
                this.trace(sessionId, String.format("%s #%d", what, s.id));
            } else {
                this.trace(sessionId, String.format("%s <%s> #%d", what, s.name, s.id));
            }
        }
    }

    /**
     * Called by FSM if a state is entered. Default implementation calls {@link Tracer#trace_state(int, String, State)}.
     */
    public void trace_enter_state(int sessionId, State s) {
        if (StaticOptions.trace_state)
            this.trace_state(sessionId, "Enter", s);
    }

    /**
     * Called by FSM if a state is left. Calls [Tracer::trace_state].
     */
    public void trace_exit_state(int sessionId, State s) {
        if (StaticOptions.trace_state)
            this.trace_state(sessionId, "Exit", s);
    }

    /**
     * Get trace mode
     */
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

    /**
     * Tracer should stop all activities and release all sessions.
     */
    public void stop() {
        // Default has nothing to do.
    }

    private static TracerFactory tracer_factory = new DefaultTracerFactory();
    private static Tracer tracer;

    public static synchronized void set_tracer_factory(TracerFactory tracer_factory) {
        if (tracer_factory != Tracer.tracer_factory
                && !(tracer_factory == null && Tracer.tracer_factory instanceof DefaultTracerFactory)) {
            if (tracer_factory == null) {
                tracer_factory = new DefaultTracerFactory();
            }
            Tracer.tracer_factory = tracer_factory;
            if (tracer != null) {
                tracer.stop();
                tracer = tracer_factory.create();
            }
        }
    }

    public static synchronized Tracer getInstance() {
        if (tracer == null)
            tracer = tracer_factory.create();
        return tracer;
    }

    protected String value_to_string(Object d) {
        if (d instanceof OrderedSet<?> os) {
            return "[" + os.data.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
        } else if (d instanceof List<?> os) {
            return "{" + os.data.stream().map(String::valueOf).collect(Collectors.joining(",")) + "}";
        } else if (d instanceof Data data) {
            return data.toString();
        } else if (d instanceof ExecutableContent ec) {
            return executable_content_to_string(ec);
        }
        return String.valueOf(d);
    }

    protected String executable_content_to_string(ExecutableContent ec) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("{EC type:").append(ec.get_type());
        for (var a : ec.get_trace().entrySet()) {
            sb
                    .append(',')
                    .append(a.getKey())
                    .append(':')
                    .append(value_to_string(a.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }
}
