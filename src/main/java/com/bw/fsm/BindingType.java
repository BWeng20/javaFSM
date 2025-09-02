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

    /**
     * Serializer Ordinal handling (other values than Enum.Ordinal).
     */
    public static BindingType from_ordinal(int ordinal) {
        return switch (ordinal) {
            case 1 -> Early;
            case 2 -> Late;
            default -> {
                Log.panic("Unknown ordinal %d for BindingType", ordinal);
                yield Early;
            }
        };
    }

    /**
     * Serializer Ordinal handling (other values than Enum.Ordinal).
     */
    public int get_ordinal() {
        return switch (this) {
            case Early -> 1;
            case Late -> 2;
        };
    }

}
