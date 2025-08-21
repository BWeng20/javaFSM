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
}
