package com.bw.fsm.tracer;

import com.bw.fsm.Log;

import java.util.Locale;

/**
 * Trace mode for FSM Tracer.
 */
public enum TraceMode {
    METHODS,
    STATES,
    EVENTS,
    ARGUMENTS,
    RESULTS,
    ALL,
    NONE;

    public static TraceMode fromString(String trace_name) {
        if (trace_name == null)
            return TraceMode.STATES;
        return switch (trace_name.toLowerCase(Locale.CANADA)) {
            case "methods" -> TraceMode.METHODS;
            case "states" -> TraceMode.STATES;
            case "events" -> TraceMode.EVENTS;
            case "arguments" -> TraceMode.ARGUMENTS;
            case "results" -> TraceMode.RESULTS;
            case "all" -> TraceMode.ALL;
            default -> {
                Log.warn("Unknown trace mode %s", trace_name);
                yield TraceMode.STATES;
            }
        };
    }
}
