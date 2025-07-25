package com.bw.fsm;

import com.bw.fsm.datamodel.SourceCode;

public abstract class Data {

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
}
