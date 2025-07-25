package com.bw.fsm;

import java.util.ArrayList;

public class Transition {

    public int id;
    public int doc_id;

    // TODO: Possibly we need some type to express event ids
    public java.util.List<String> events;
    public boolean wildcard;
    public Data cond;
    public State source;
    public final java.util.List<State> target = new ArrayList<>(1);
    public TransitionType transition_type;
    public ExecutableContentRegion content;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(20);
        stringBuilder.append("Transition ").append(source).append(" -> ");
        for (State t : target)
            stringBuilder.append(t);
        return stringBuilder.toString();
    }
}
