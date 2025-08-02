package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Data Variant used to handle data in a type-safe but Datamodel-agnostic way.
 */
public abstract class Data {

    /**
     * Tries to convert the data to a number.
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

    private static Number parseNumber(java.lang.String s) {
        try {
            return nf.parse(s);
        } catch (ParseException e) {
            return NUL;
        }
    }

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
    }

    public static class Double extends Data {
        double value;

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
    }

    public static class String extends Data {
        java.lang.String value;

        public String(java.lang.String value) {
            this.value = value;
        }

        @Override
        public Number as_number() {
            return value == null ? NUL : parseNumber(value);
        }

        @Override
        public java.lang.String as_script() {
            // @TODO
            return "'" + value + "'";
        }

        @Override
        public boolean is_empty() {
            return value == null || value.isEmpty();
        }

        @Override
        public java.lang.String toString() {
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

    }

    public static class Boolean extends Data {
        boolean value;

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
    }

    public static class Array extends Data {

        java.util.List<Data> values;

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
    }

    /**
     * A map, can also be used to store "object"-like data-structures.
     */
    public static class Map extends Data {

        java.util.Map<java.lang.String, Data> values;

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
    }


}
