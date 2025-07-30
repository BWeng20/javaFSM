package com.bw.fsm;

public enum TransitionType {
    Internal,
    External;

    public static TransitionType map_transition_type(String ts) {
        return switch (ts.toLowerCase()) {
            case "internal" -> TransitionType.Internal;
            case "external" -> TransitionType.External;
            case "" -> TransitionType.External;
            default -> {
                StaticOptions.panic("Unknown transition type '%s'", ts);
                yield null;
            }
        };
    }
}
