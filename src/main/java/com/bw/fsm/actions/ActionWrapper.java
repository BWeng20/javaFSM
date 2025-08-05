package com.bw.fsm.actions;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a map of actions.
 */
public class ActionWrapper {

    public Map<String, Action> actions = new HashMap<>();

    public void add_action(String name, Action action) {
        this.actions.put(name, action);
    }

    public Data execute(String action_name, List<Data> arguments, GlobalData global) {
        Action action = this.actions.get(action_name);
        if (action != null) {
            return action.execute(arguments, global);
        } else {
            throw new IllegalArgumentException(String.format("Action '%s' not found", action_name));
        }
    }
}
