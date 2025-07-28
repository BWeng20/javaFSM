package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;

import java.util.Objects;

public abstract class Data {

    public abstract boolean is_empty();

    public static class Integer extends Data {
        int value;

        public Integer(int value) {
            this.value = value;
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
        public java.lang.String toString() {
            return "null";
        }

        @Override
        public boolean is_empty() {
            return true;
        }

        public static final Data NULL = new Null();

    }

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

    public static class Source extends Data {
        SourceCode source;

        public Source(SourceCode source) {
            this.source = source;
        }

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

        public static final Data NONE = new None();
    }


}
