package com.bw.fsm;

import java.util.Locale;

public enum HistoryType {
    Shallow,
    Deep,
    None;

    /**
     * Case invariant converter. Returns "None" for unknown values.
     */
    public static HistoryType fromString(String e) {
        return switch (e.toLowerCase(Locale.CANADA)) {
            case "deep" -> Deep;
            case "shallow" -> Shallow;
            case "" -> None;
            default -> {
                Log.panic("Unknown transition type '%s'", e);
                yield None;
            }
        };
    }


    /**
     * Serializer Ordinal handling (possibly other values than Enum.Ordinal).
     */
    public static HistoryType from_ordinal(int ordinal) {
        return switch (ordinal) {
            case 1 -> Shallow;
            case 2 -> Deep;
            default -> None;
        };
    }

    /**
     * Serializer Ordinal handling (possibly other values than Enum.Ordinal).
     */
    public int get_ordinal() {
        return switch (this) {
            case Shallow -> 1;
            case Deep -> 2;
            case None -> 0;
        };
    }
}
