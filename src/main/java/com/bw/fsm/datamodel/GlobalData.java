package com.bw.fsm.datamodel;

import com.bw.fsm.*;
import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.eventIoProcessor.EventIOProcessor;
import com.bw.fsm.tracer.Tracer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <b>W3C says</b>:<br>
 * <b>Global variables</b><br>
 * The following variables are global from the point of view of the algorithm.
 * Their values will be set in the procedure interpret().<br>
 * <b>Actual Implementation</b><br>
 * In the W3C algorithm the datamodel is simple a global variable.<br>
 * As the datamodel needs access to other global variables and some languages (rust!) doesn't like
 * accessing data of parents from inside a member, most global data is moved to
 * this struct that is owned by the datamodel.
 */
public class GlobalData {

    public String source;
    public FsmExecutor executor;
    public ActionWrapper actions = new ActionWrapper();
    public @NotNull
    final Tracer tracer;
    public OrderedSet<State> configuration = new OrderedSet<>();
    public OrderedSet<State> statesToInvoke = new OrderedSet<>();
    public HashTable<State, OrderedSet<State>> historyValue = new HashTable<>();
    public boolean running = false;

    public Queue<Event> internalQueue = new Queue<>();

    public BlockingQueue<Event> externalQueue = new BlockingQueue<>();

    /**
     * Invoked Sessions. Key: InvokeId.
     */
    public HashMap<String, ScxmlSession> child_sessions = new HashMap<>();

    /**
     * Set if this FSM was created as result of some invoke.
     */
    public String caller_invoke_id;
    public Integer parent_session_id;

    /**
     * Unique Id of the owning session.
     */
    public Integer session_id = 0;

    /**
     * Will contain after execution the final configuration.
     */
    public final java.util.List<String> final_configuration = new ArrayList<>();
    public Map<String, Data> environment = new HashMap<>();

    /**
     * Stores any delayed send (with a "sendid"), Key: sendid
     */
    public Map<String, FsmTimer> delayed_send = new HashMap<>();
    public Map<String, EventIOProcessor> io_processors = new HashMap<>();

    /**
     * For datamodels, that need an explicit storage.
     */
    public final Map<String, Data> data = new HashMap<>();

    public GlobalData(@NotNull Tracer tracer) {
        this.tracer = Objects.requireNonNull(tracer);
    }

    public void enqueue_internal(Event event) {
        if (StaticOptions.trace_event) {
            tracer.event_internal_send(session_id, event);
        }
        this.internalQueue.enqueue(event);
    }
}
