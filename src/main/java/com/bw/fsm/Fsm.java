package com.bw.fsm;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

public class Fsm {
    public static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger SESSION_ID_COUNTER = new AtomicInteger(1);

    // public Tracer tracer;
    public String datamodel;
    public BindingType binding = BindingType.Early;
    public String version;
    public final Map<String, State> statesNames = new HashMap<>();

    public String name;
    public String file;

    /// An FSM can have actual multiple initial-target-states, so this state may be artificial.
    /// Reader has to generate a parent state if needed.
    /// This state also serve as the "scxml" state element were mentioned.
    public State pseudo_root;

    public ExecutableContentRegion script;

    /// Set if this FSM was created as result of some invoke.
    /// See also Global.caller_invoke_id
    public Integer caller_invoke_id;
    public Integer parent_session_id;

    public Timer timer;

    public int generate_id_count = 0;

    public State get_state_by_id(int id) {
        return null;
    }
}
