package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

/**
 * Data Variant used to handle data in a type-safe but Datamodel-agnostic way.
 */
@SuppressWarnings("unused")
public abstract class Data {

    /**
     * Tries to convert the data to a number.
     *
     * @return default returns 0.
     */
    public Number as_number() {
        return NUL;
    }

    public abstract java.lang.String as_script();

    public boolean is_numeric() {
        return false;
    }

    public abstract boolean is_empty();


    private static final java.lang.Integer NUL = 0;
    private static final java.lang.Integer ONE = 1;
    private static NumberFormat nf = NumberFormat.getInstance(Locale.UK);

    private static @NotNull Number parseNumber(java.lang.String s) {
        try {
            return nf.parse(s);
        } catch (ParseException e) {
            return NUL;
        }
    }

    /**
     * Gets a deep copy
     *
     * @return deep copy of the data element.
     */
    public abstract @NotNull Data getCopy();

    public static class Integer extends Data {
        int value;

        public Integer(int value) {
            this.value = value;
        }

        @Override
        public Number as_number() {
            return value;
        }

        @Override
        public java.lang.String as_script() {
            return nf.format(value);
        }


        @Override
        public boolean is_numeric() {
            return true;
        }

        @Override
        public boolean is_empty() {
            return false;
        }

        @Override
        public java.lang.String toString() {
            return java.lang.Integer.toString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Integer i) {
                return value == i.value;
            }
            if (o instanceof Double i) {
                return value == i.value;
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new Integer(this.value);
        }
    }

    public static class Double extends Data {
        private final double value;

        public Double(double value) {
            this.value = value;
        }

        @Override
        public Number as_number() {
            return value;
        }

        @Override
        public java.lang.String as_script() {
            return nf.format(value);
        }

        @Override
        public boolean is_numeric() {
            return true;
        }

        @Override
        public boolean is_empty() {
            return false;
        }

        @Override
        public java.lang.String toString() {
            return java.lang.Double.toString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Double i) {
                return value == i.value;
            }
            if (o instanceof Integer i) {
                return value == i.value;
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new Double(this.value);
        }
    }

    public static class String extends Data {
        final public @NotNull java.lang.String value;
        private @Nullable java.lang.String script_value;

        public String(@NotNull java.lang.String value) {
            this.value = value;
        }

        @Override
        public @NotNull Number as_number() {
            return parseNumber(value);
        }

        @Override
        public @NotNull java.lang.String as_script() {
            if (script_value == null) {
                script_value =
                        value.replace("\\", "\\\\")
                                .replace("'", "\\'");
            }
            return "'" + script_value + "'";
        }

        @Override
        public boolean is_empty() {
            return value.isEmpty();
        }

        @Override
        public @NotNull java.lang.String toString() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof String i) {
                return Objects.equals(value, i.value);
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new String(this.value);
        }
    }

    public static class Boolean extends Data {
        public boolean value;

        public Boolean(boolean value) {
            this.value = value;
        }

        @Override
        public Number as_number() {
            return value ? ONE : NUL;
        }

        @Override
        public java.lang.String as_script() {
            return value ? "true" : "false";
        }

        @Override
        public boolean is_empty() {
            return false;
        }

        @Override
        public java.lang.String toString() {
            return value ? "true" : "false";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Boolean i) {
                return value == i.value;
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new Boolean(this.value);
        }
    }

    public static class Array extends Data {

        public java.util.List<Data> values;

        public Array(java.util.List<Data> values) {
            this.values = values;
        }

        @Override
        public boolean is_empty() {
            return values.isEmpty();
        }

        public Number as_number() {
            return values.size();
        }

        @Override
        public java.lang.String as_script() {
            return values.toString();
        }

        @Override
        public java.lang.String toString() {
            return values.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Array i) {
                return values.equals(i.values);
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            Array a = new Array(new ArrayList<>(this.values.size()));
            for (Data d : values) {
                a.values.add(d.getCopy());
            }
            return a;
        }
    }

    /**
     * A map, can also be used to store "object"-like data-structures.
     */
    public static class Map extends Data {

        public java.util.Map<java.lang.String, Data> values;

        public Map(java.util.Map<java.lang.String, Data> values) {
            this.values = values;
        }

        @Override
        public boolean is_empty() {
            return values.isEmpty();
        }

        @Override
        public Number as_number() {
            return values.size();
        }

        @Override
        public java.lang.String as_script() {
            return values.toString();
        }

        @Override
        public java.lang.String toString() {
            return values.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Map i) {
                return values.equals(i.values);
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            HashMap<java.lang.String, Data> a = new HashMap<>();
            for (var d : values.entrySet()) {
                a.put(d.getKey(), d.getValue().getCopy());
            }
            return new Map(a);
        }
    }

    public static class Null extends Data {

        private Null() {
        }

        @Override
        public boolean is_numeric() {
            return true;
        }

        @Override
        public java.lang.String toString() {
            return "null";
        }

        @Override
        public java.lang.String as_script() {
            return "null";
        }

        @Override
        public boolean is_empty() {
            return true;
        }

        public static final Data NULL = new Null();

        @Override
        public @NotNull Data getCopy() {
            return NULL;
        }
    }

    /**
     * Special placeholder to indicate an error
     */
    public static class Error extends Data {
        java.lang.String message;

        public Error(java.lang.String message) {
            this.message = message;
        }

        @Override
        public java.lang.String toString() {
            return "Error: " + message;
        }

        @Override
        public java.lang.String as_script() {
            return "";
        }

        @Override
        public boolean is_empty() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Error err) {
                return Objects.equals(this.message, err.message);
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new Error(message);
        }

    }

    /**
     * Special placeholder to indicate a fsm definition indie &lt;content> elements.
     */
    public static class FsmDefinition extends Data {
        public Fsm fsm;
        public java.lang.String xml;

        public FsmDefinition(java.lang.String xml, Fsm fsm) {
            this.fsm = fsm;
            this.xml = xml;
        }

        @Override
        public java.lang.String as_script() {
            return xml;
        }

        @Override
        public boolean is_empty() {
            return fsm == null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof FsmDefinition s) {
                // No feasible way to compare FSM by content.
                return this.fsm == s.fsm;
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new FsmDefinition(xml, fsm);
        }

    }

    /**
     * Special placeholder to indicate script source (from FSM definition)
     * that needs to be evaluated by the datamodel.
     */
    public static class Source extends Data {
        SourceCode source;

        public Source(SourceCode source) {
            this.source = source;
        }

        @Override
        public Number as_number() {
            return source == null ? NUL : parseNumber(source.source);
        }

        @Override
        public java.lang.String as_script() {
            return source == null ? null : source.source;
        }

        /**
         * Create a Data.Source from a String with invalid id.
         * Should be used for calculated script source, that is not part of FSM definition.
         */
        public Source(java.lang.String source) {
            this.source = new SourceCode(source, 0);
        }

        public Source(String source) {
            this.source = new SourceCode(source.value, 0);
        }

        @Override
        public boolean is_empty() {
            return source == null || source.is_empty();
        }

        @Override
        public java.lang.String toString() {
            return source == null ? "null" : source.source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o instanceof Source s) {
                return Objects.equals(this.source, s.source);
            }
            return false;
        }

        @Override
        public @NotNull Data getCopy() {
            return new Source(source);
        }
    }

    /**
     * Special placeholder to indicate empty content.
     * There is only one instance.
     */
    public static class None extends Data {

        private None() {
        }

        @Override
        public java.lang.String toString() {
            return "";
        }

        @Override
        public boolean is_empty() {
            return true;
        }

        @Override
        public java.lang.String as_script() {
            return "";
        }

        public static final Data NONE = new None();

        @Override
        public @NotNull Data getCopy() {
            return NONE;
        }
    }

}
