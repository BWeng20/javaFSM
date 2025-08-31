package com.bw.fsm;

import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.expressionEngine.ExpressionException;
import com.bw.fsm.expressionEngine.ExpressionLexer;

import java.util.Map;

/**
 * Describes one executable content element. E.g. "if", "log" etc.
 */
public interface ExecutableContent {
    boolean execute(Datamodel datamodel, Fsm fsm);

    /**
     * Returns the matching "TYPE_XXX".
     */
    int get_type();

    Map<String, Object> get_trace();

    String TARGET_SCXML_EVENT_PROCESSOR = "http://www.w3.org/TR/scxml/#SCXMLEventProcessor";

    int TYPE_IF = 0;
    int TYPE_EXPRESSION = 1;

    /// Unused:
    /// int TYPE_SCRIPT = 2;

    int TYPE_LOG = 3;
    int TYPE_FOREACH = 4;
    int TYPE_SEND = 5;
    int TYPE_RAISE = 6;
    int TYPE_CANCEL = 7;
    int TYPE_ASSIGN = 8;

    String[] TYPE_NAMES = {
            "if",
            "expression",
            "unused",
            "log",
            "foreach",
            "send",
            "raise",
            "cancel",
            "assign"
    };

    static int parse_duration_to_milliseconds(String ms) {
        if (ms == null || ms.isEmpty()) {
            return 0;
        } else {
            try {
                var exp = new ExpressionLexer(ms);
                var value_result = exp.next_number();
                String unit = exp.next_name();

                var v = value_result.as_double();
                switch (unit) {
                    case "D", "d" -> v *= 24.0 * 60.0 * 60.0 * 1000.0;
                    case "H", "h" -> v *= 60.0 * 60.0 * 1000.0;
                    case "M", "m" -> v *= 60000.0;
                    case "S", "s" -> v *= 1000.0;
                    case "MS", "ms" -> {
                    }
                    default -> {
                        return -1;
                    }
                }
                return (int) Math.round(v);
            } catch (ExpressionException e) {
                Log.exception("Failed to´parse duration.", e);
                return -1;
            }
        }
    }

    static Map<String, Object> toMap(String k, Object o) {
        return Map.of(k, o == null ? "null" : o);
    }

    static Map<String, Object> toMap(String k1, Object o1, String k2, Object o2) {
        return Map.of(k1, o1 == null ? "null" : o1,
                k2, o2 == null ? "null" : o2
        );
    }

    static Map<String, Object> toMap(String k1, Object o1, String k2, Object o2, String k3, Object o3) {
        return Map.of(k1, o1 == null ? "null" : o1,
                k2, o2 == null ? "null" : o2,
                k3, o3 == null ? "null" : o3
        );
    }

    static Map<String, Object> toMap(String k1, Object o1, String k2, Object o2, String k3, Object o3, String k4, Object o4) {
        return Map.of(k1, o1 == null ? "null" : o1,
                k2, o2 == null ? "null" : o2,
                k3, o3 == null ? "null" : o3,
                k4, o4 == null ? "null" : o4
        );
    }
}
