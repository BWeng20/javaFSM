package com.bw.fsm.datamodel;

import java.util.Objects;

public class SourceCode {

    public SourceCode(String source, int id) {
        this.source = source;
        this.source_id = id;
    }

    public final String source;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof SourceCode sourceCode) {
            return Objects.equals(this.source, sourceCode.source);
        }
        return false;
    }

    @Override
    public java.lang.String toString() {
        return source;
    }


    /// The unique Id of the script. Unique only inside the current life-cycle.\
    /// Invalid if 0
    public final int source_id;

    public boolean is_empty() {
        return source == null || source.isEmpty();
    }
}
