package com.bw.fsm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class State {
    /// The internal Id (not W3C). Used to refence the state.
    public int id;

    /// The unique id, counting in document order.
    /// "id" is increasing on references to states, not declaration and may not result in correct order.
    public int doc_id;

    /// The SCXML id.
    public String name;

    /// The initial transition id (if the state has sub-states).
    public Transition initial;

    /// The sub-states of this state.
    public final java.util.List<State> states = new java.util.ArrayList<>();

    /// True for "parallel" states
    public boolean is_parallel = false;

    /// True for "final" states
    public boolean is_final = false;

    public HistoryType history_type = HistoryType.None;

    /// The script that is executed if the state is entered. See W3c comments for \<onentry\> above.
    public final java.util.List<ExecutableContent> onentry = new ArrayList<>();

    /// The script that is executed if the state is left. See W3c comments for \<onexit\> above.
    public final java.util.List<ExecutableContent> onexit = new ArrayList<>();

    /// All transitions between sub-states.
    public final List<Transition> transitions = new List<>();

    public final List<Invoke> invoke = new List<>();
    public final List<State> history = new List<>();

    /// The initial data values on this state.
    public final Map<String, Data> data = new HashMap<>();

    /// True if the state was never entered before.
    public boolean isFirstEntry = true;

    public State parent;
    public DoneData donedata;

    public State(String name) {
        id = 0;
        doc_id = 0;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

}
