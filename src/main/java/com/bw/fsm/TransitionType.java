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
                Log.panic("Unknown transition type '%s'", ts);
                yield null;
            }
        };
    }


    /**
     * Serializer Ordinal handling (possibly other values than Enum.Ordinal).
     */
    public static TransitionType from_ordinal(int ordinal) {
        return switch (ordinal) {
            case 0 -> Internal;
            case 1 -> External;
            default -> {
                Log.panic("Unknown ordinal %d for TransitionType", ordinal);
                yield External;
            }
        };
    }

    /**
     * Serializer Ordinal handling (possibly other values than Enum.Ordinal).
     */
    public int get_ordinal() {
        return switch (this) {
            case Internal -> 0;
            case External -> 1;
        };
    }
}
