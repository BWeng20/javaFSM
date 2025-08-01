package com.bw.fsm.datamodel;

import com.bw.fsm.*;
import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.event_io_processor.EventIOProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class GlobalData {

    public String source;
    public FsmExecutor executor;
    public ActionWrapper actions = new ActionWrapper();
    public OrderedSet<State> configuration = new OrderedSet<>();
    public OrderedSet<State> statesToInvoke = new OrderedSet<>();
    public HashTable<State, OrderedSet<State>> historyValue = new HashTable<>();
    public boolean running = false;

    Queue<Event> internalQueue = new Queue<>();

    public BlockingQueue<Event> externalQueue = new BlockingQueue();

    /// Invoked Sessions. Key: InvokeId.
    public HashMap<Integer, ScxmlSession> child_sessions = new HashMap<>();

    /// Set if this FSM was created as result of some invoke.
    public Integer caller_invoke_id;
    public Integer parent_session_id;

    /// Unique Id of the owning session.
    public Integer session_id = 0;

    /// Will contain after execution the final configuration, if set before.
    public java.util.List<String> final_configuration;
    public Map<String, Data> environment = new HashMap<>();

    /// Stores any delayed send (with a "sendid"), Key: sendid
    public Map<String, Future> delayed_send = new HashMap<>();
    public Map<String, EventIOProcessor> io_processors = new HashMap<>();

    public DataStore data = new DataStore();

    public GlobalData() {
    }

    public void enqueue_internal(Event event) {
        this.internalQueue.enqueue(event);
    }
}
