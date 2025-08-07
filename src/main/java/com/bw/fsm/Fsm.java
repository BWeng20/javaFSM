package com.bw.fsm;

import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.tracer.Tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

public class Fsm {
    public static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger SESSION_ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger PLATFORM_ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger THREAD_ID_COUNTER = new AtomicInteger(1);

    /// Platform specific event to cancel the current session.
    public static final String EVENT_CANCEL_SESSION = "error.platform.cancel";
    public static final String EVENT_DONE_INVOKE_PREFIX = "done.invoke.";


    public Tracer tracer = Tracer.create_tracer();
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
    public String caller_invoke_id;
    public Integer parent_session_id;

    public Timer timer;

    public int generate_id_count = 0;


    /**
     * Starts the FSM inside a worker thread.
     */
    public ScxmlSession start_fsm(ActionWrapper actions, FsmExecutor executor) {
        return start_fsm_with_data(actions, executor, new ArrayList<>());
    }

    public ScxmlSession start_fsm_with_data(
            ActionWrapper actions,
            FsmExecutor executor,
            java.util.List<ParamPair> data) {
        return start_fsm_with_data_and_finish_mode(actions, executor, data, FinishMode.DISPOSE);
    }

    public ScxmlSession start_fsm_with_data_and_finish_mode(
            ActionWrapper actions,
            FsmExecutor executor,
            java.util.List<ParamPair> data,
            FinishMode finish_mode) {
        BlockingQueue<Event> externalQueue = new BlockingQueue<>();

        Integer session_id = SESSION_ID_COUNTER.incrementAndGet();
        final var session = new ScxmlSession(session_id, externalQueue);
        session.global_data.source = this.name;

        switch (finish_mode) {
            case DISPOSE -> {
            }
            case KEEP_CONFIGURATION ->
                // FSM shall keep the final configuration after exit.
                    session.global_data.final_configuration = new ArrayList<>();
            case NOTHING -> {
            }
        }

        executor.state.sessions.put(session_id, session);
        final var options = executor.state.datamodel_options;

        session.global_data.actions = actions;
        for (var p : executor.state.processors) {
            for (var t : p.get_types()) {
                session.global_data.io_processors.put(t, p);
            }
        }

        var thread = new Thread(
                () -> {
                    if (StaticOptions.debug_option)
                        Log.debug("SM Session %s starting...", session_id);
                    Datamodel datamodel = DatamodelFactory.create_datamodel(this.datamodel, session.global_data, options);
                    var global = datamodel.global();
                    global.externalQueue = externalQueue;
                    global.session_id = session_id;
                    global.caller_invoke_id = this.caller_invoke_id;
                    global.parent_session_id = this.parent_session_id;
                    global.executor = executor;

                    // W3C:
                    // If the value of a key ... matches the 'id' of a <data> element
                    // in the top-level data model of the invoked session, the SCXML Processor
                    // MUST use the value of the key as the initial value of the corresponding
                    // <data> element.
                    if (!data.isEmpty()) {
                        var root_state = this.pseudo_root;
                        for (var val : data) {
                            if (root_state.data.get(val.name) != null) {
                                root_state.data
                                        .put(val.name, val.value.getCopy());
                            }
                        }
                    }
                    this.interpret(datamodel);
                    if (StaticOptions.debug_option)
                        Log.debug("SM finished");
                }, String.format("fsm_%s", THREAD_ID_COUNTER.incrementAndGet()));

        session.thread = thread;
        return session;
    }

    /**
     * <b>W3C says:</b><br>
     * The purpose of this procedure is to initialize the interpreter and to start processing.<br>
     * <p>
     * In order to interpret an SCXML document, first (optionally) perform xinclude processing and (optionally) validate
     * the document, throwing an exception if validation fails.<br>
     * Then convert initial attributes to &lt;initial&gt; container children with transitions
     * to the state specified by the attribute. (This step is done purely to simplify the statement of
     * the algorithm and has no effect on the system's behavior).<br>
     * Such transitions will not contain any executable content.<br>
     * Initialize the global data structures, including the data model.<br>
     * If binding is set to 'early', initialize the data model.<br>
     * Then execute the global &lt;script&gt; element, if any.<br>
     * Finally, call enterStates on the initial configuration, set the global running
     * variable to true and start the interpreter's event loop.<br>
     * <pre>
     * procedure interpret(doc):
     *     if not valid(doc): failWithError()
     *     expandScxmlSource(doc)
     *     configuration = new OrderedSet()
     *     statesToInvoke = new OrderedSet()
     *     internalQueue = new Queue()
     *     externalQueue = new BlockingQueue()
     *     historyValue = new HashTable()
     *     datamodel = new Datamodel(doc)
     *     if doc.binding == "early":
     *         initializeDatamodel(datamodel, doc)
     *     running = true
     *     executeGlobalScriptElement(doc)
     *     enterStates([doc.initial.transition])
     *     mainEventLoop()
     *     ...
     * </pre>
     */
    public void interpret(Datamodel datamodel) {
        // TODO
        throw new UnsupportedOperationException();
    }


}
