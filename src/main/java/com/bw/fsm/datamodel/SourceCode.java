package com.bw.fsm.datamodel;

public class SourceCode {

    public SourceCode(String source, int id) {
        this.source = source;
        this.source_id = id;
    }

    public String source;

    /// The unique Id of the script. Unique only inside the current life-cycle.\
    /// Invalid if 0-
    public int source_id;

    public boolean is_empty() {
        return source == null || source.isEmpty();
    }
}
