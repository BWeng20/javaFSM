package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;

import java.util.Objects;

public abstract class Data {

    public static final Data NULL = new Null();

    public abstract boolean is_empty();

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
            if (o instanceof Source source) {
                return Objects.equals(this.source, source.source);
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
    }

    public static class Integer extends Data {
        int value;

        public Integer(int value) {
            this.value = value;
        }

        @Override
        public boolean is_empty() {
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

    }
}
