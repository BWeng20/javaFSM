package com.bw.fsm;

import java.util.Locale;

/**
 * Datamodel binding type.
 *
 * @see <a href="https://www.w3.org/TR/scxml/#DataBinding">DataBinding</a>
 */
public enum BindingType {

    /**
     * Default
     */
    Early,
    Late;


    /**
     * Case invariant converter. Returns "Early" for unknown values.
     */
    public static BindingType fromString(String e) {
        return switch (e.toUpperCase(Locale.CANADA)) {
            case "EARLY" -> Early;
            case "LATE" -> Late;
            default -> Early;
        };
    }
}
