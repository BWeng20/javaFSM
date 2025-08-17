package com.bw.fsm;

import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.event_io_processor.ScxmlEventIOProcessor;
import com.bw.fsm.tracer.Argument;
import com.bw.fsm.tracer.Tracer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The FSM implementation, according to W3C proposal.
 */
@SuppressWarnings("unused")
public class Fsm {
    public static final AtomicInteger ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger SESSION_ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger PLATFORM_ID_COUNTER = new AtomicInteger(1);
    public static final AtomicInteger THREAD_ID_COUNTER = new AtomicInteger(1);

    /// Platform specific event to cancel the current session.
    public static final String EVENT_CANCEL_SESSION = "error.platform.cancel";
    public static final String EVENT_DONE_INVOKE_PREFIX = "done.invoke.";

    public static final Comparator<State> state_document_order = Comparator.comparingInt(s -> s.doc_id);

    public static final Comparator<State> state_entry_order =
            // Same as Document order
            Fsm.state_document_order;

    public static final Comparator<State> state_exit_order =
            (s1, s2) -> {
                // Reverse Document order
                return Fsm.state_document_order.compare(s2, s1);
            };

    public static final Comparator<Transition> transition_document_order =
            Comparator.comparingInt(t -> t.doc_id);

    public static final Comparator<Invoke> invoke_document_order = Comparator.comparing(s -> s.doc_id);


    public Tracer tracer = Tracer.create_tracer();
    public String datamodel;
    public BindingType binding = BindingType.Early;
    public String version;
    public final Map<String, State> statesNames = new HashMap<>();

    public String name;
    public String file;

    /**
     * An FSM can have actual multiple initial-target-states, so this state may be artificial.
     * Reader has to generate a parent state if needed.<br>
     * This state also serve as the "scxml" state element were mentioned.
     */
    public State pseudo_root;

    public ExecutableContentRegion script;

    /**
     * Set if this FSM was created as result of some invoke.
     * See also Global.caller_invoke_id
     */
    public String caller_invoke_id;
    public Integer parent_session_id;

    public Timer timer;

    public int generate_id_count = 0;

    /**
     * Starts the FSM inside a worker thread.
     */
    public @NotNull ScxmlSession start_fsm(ActionWrapper actions, FsmExecutor executor) {
        return start_fsm_with_data(actions, executor, new ArrayList<>());
    }

    public @NotNull ScxmlSession start_fsm_with_data(
            ActionWrapper actions,
            FsmExecutor executor,
            java.util.List<ParamPair> data) {
        return start_fsm_with_data_and_finish_mode(actions, executor, data, FinishMode.DISPOSE);
    }

    @NotNull
    public ScxmlSession start_fsm_with_data_and_finish_mode(
            @NotNull ActionWrapper actions,
            @NotNull FsmExecutor executor,
            @NotNull java.util.List<ParamPair> data,
            @NotNull FinishMode finish_mode) {
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

        // Take over the current log stream to new Thread
        final PrintStream os = Log.getPrintStream();

        var thread = new Thread(
                () -> {
                    try {
                        Log.setLogStream(os);
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
                    } catch (Exception e) {
                        Log.exception("FSM terminated with exception.", e);
                    } finally {
                        Log.releaseStream();
                    }
                }, String.format("fsm_%s", THREAD_ID_COUNTER.incrementAndGet()));

        session.thread = thread;
        Log.info("Starting " + name + " in tread " + thread.getName());
        thread.start();
        return session;
    }

    /// Implements variant "initializeDataModel(datamodel, doc)" from W3C.
    protected void initialize_data_models_recursive(Datamodel datamodel, State state, boolean set_data) {
        datamodel.initializeDataModel(this, state, set_data);

        for (var child_state : state.states) {
            this.initialize_data_models_recursive(datamodel, child_state, set_data);
        }
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
        if (StaticOptions.trace_method) {
            this.tracer.enter_method("interpret");
        }
        if (!this.valid()) {
            this.failWithError();
            return;
        }
        this.expandScxmlSource();

        var gd = datamodel.global();
        datamodel.clear();
        // Initialize session variables "_name" and "_sessionid"
        var session_id = datamodel.global().session_id;
        datamodel.initialize_read_only(Datamodel.SESSION_ID_VARIABLE_NAME, new Data.Integer(session_id));
        // TODO :Escape name
        datamodel.initialize_read_only(Datamodel.SESSION_NAME_VARIABLE_NAME, this.name == null ? Data.Null.NULL : new Data.String(this.name));

        gd.internalQueue.clear();
        gd.historyValue.clear();
        gd.running = true;

        datamodel.add_functions(this);
        datamodel.set_ioprocessors();

        this.initialize_data_models_recursive(
                datamodel,
                this.pseudo_root,
                this.binding == BindingType.Early
        );

        this.executeGlobalScriptElement(datamodel);

        var inital_states = new List<Transition>();
        var itid = this.pseudo_root.initial;
        if (itid != null) {
            inital_states.push(itid);
        }
        this.enterStates(datamodel, inital_states);
        this.mainEventLoop(datamodel);
        if (StaticOptions.trace_method) {
            this.tracer.exit_method("interpret");
        }
    }

    private boolean validateState(State state) {
        if (state.doc_id == 0) {
            if (StaticOptions.trace)
                this.tracer.trace(String.format("Referenced state '%s' is not declared", state.name));
            return false;
        }
        for (var subState : state.states) {
            if (!validateState(subState))
                return false;
        }
        return true;

    }

    /**
     * <b>Actual implementation:</b>
     * TODO
     * <ul>
     * <li>check if all state/transition references are correct (all states have a document-id)</li>
     * <li>check if all special scxml conditions are satisfied.</li>
     * </ul>
     */
    protected boolean valid() {
        if (pseudo_root != null) {
            return validateState(pseudo_root);
        }
        return true;
    }


    /**
     * <b>#Actual implementation</b>:* Throws a runtime error
     */
    protected void failWithError() {
        if (StaticOptions.trace)
            this.tracer.trace("FSM has failed");
        throw new RuntimeException("FSM has failed");
    }

    /**
     * This method is called on the fsm model, after
     * the xml document was processed. It should check if all References to states are fulfilled.
     */
    protected void expandScxmlSource() {
    }

    protected void executeGlobalScriptElement(Datamodel datamodel) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method("executeGlobalScriptElement");
        }

        if (this.script != null) {
            datamodel.executeContent(this, this.script);
        }
        if (StaticOptions.trace_method) {
            this.tracer.exit_method("executeGlobalScriptElement");
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure mainEventLoop()</b><br>
     * This loop runs until we enter a top-level final state or an external entity cancels processing.
     * In either case 'running' will be set to false (see EnterStates, below, for termination by
     * entering a top-level final state).<br>
     * <br>
     * At the top of the loop, we have either just entered the state machine, or we have just
     * processed an external event. Each iteration through the loop consists of four main steps:
     * <ol>
     * <li> Complete the macrostep by repeatedly taking any internally enabled transitions, namely
     * those that don't require an event or that are triggered by an internal event.<br>
     * After each such transition/microstep, check to see if we have reached a final state.</li>
     * <li>When there are no more internally enabled transitions available, the macrostep is done. Execute
     * any &lt;invoke> tags for states that we entered on the last iteration through the loop </li>
     * <li>If any internal events have been generated by the invokes, repeat step 1 to handle any errors
     * raised by the &lt;invoke> elements.
     * <li> When the internal event queue is empty, wait for an
     * external event and then execute any transitions that it triggers.<br>
     * However special preliminary processing is applied to the event if the state has executed any &lt;invoke> elements. First,
     * if this event was generated by an invoked process, apply &lt;finalize> processing to it.
     * Secondly, if any &lt;invoke> elements have autoforwarding set, forward the event to them.</li>
     * </ol>
     * These steps apply before the transitions are taken.<br>
     * <br>
     * This event loop thus enforces run-to-completion semantics, in which the system process an external event and then takes all the 'follow-up' transitions that the processing has enabled before looking for another external event. For example, suppose that the external event queue contains events ext1 and ext2 and the machine is in state s1. If processing ext1 takes the machine to s2 and generates internal event int1, and s2 contains a transition t triggered by int1, the system is guaranteed to take t, no matter what transitions s2 or other states have that would be triggered by ext2. Note that this is true even though ext2 was already in the external event queue when int1 was generated. In effect, the algorithm treats the processing of int1 as finishing up the processing of ext1.
     * <pre>
     * procedure mainEventLoop():
     *     while running:
     *         enabledTransitions = null
     *         macrostepDone = false
     *         # Here we handle eventless transitions and transitions
     *         # triggered by internal events until macrostep is complete
     *         while running and not macrostepDone:
     *             enabledTransitions = selectEventlessTransitions()
     *             if enabledTransitions.isEmpty():
     *                 if internalQueue.isEmpty():
     *                     macrostepDone = true
     *                 else:
     *                     internalEvent = internalQueue.dequeue()
     *                     datamodel["_event"] = internalEvent
     *                     enabledTransitions = selectTransitions(internalEvent)
     *             if not enabledTransitions.isEmpty():
     *                 microstep(enabledTransitions.toList())
     *         # either we're in a final state, and we break out; of the loop
     *         if not running:
     *             break
     *         # or; we've completed a macrostep, so we start a new macrostep by waiting for an external event
     *         # Here we invoke whatever needs to be invoked. The implementation of 'invoke' is platform-specific
     *         for state in statesToInvoke.sort(entryOrder):
     *             for inv in state.invoke.sort(documentOrder):
     *                 invoke(inv)
     *         statesToInvoke.clear()
     *         # Invoking may have raised internal error events and we iterate to handle them
     *         if not internalQueue.isEmpty():
     *             continue;
     *         # A blocking wait for an external event.  Alternatively, if we have been invoked
     *         # our parent session also might cancel us.  The mechanism for this is platform specific,
     *         # but here we assume it’s a special event we receive
     *         externalEvent = externalQueue.dequeue()
     *         if isCancelEvent(externalEvent):
     *             running = false
     *             continue;
     *         datamodel["_event"] = externalEvent
     *         for state in configuration:
     *             for inv in state.invoke:
     *                 if inv.invokeid == externalEvent.invokeid:
     *                     applyFinalize(inv, externalEvent)
     *                 if inv.autoforward:
     *                     send(inv.id, externalEvent)
     *         enabledTransitions = selectTransitions(externalEvent)
     *         if not enabledTransitions.isEmpty():
     *             microstep(enabledTransitions.toList())
     *     # End of outer while running loop.  If we get here, we have reached a top-level final state or have been cancelled
     *     exitInterpreter()
     * </pre>
     */
    protected void mainEventLoop(Datamodel datamodel) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method("mainEventLoop");
        }

        final var caller_invoke_id = this.caller_invoke_id == null ? "" : this.caller_invoke_id;

        while (datamodel.global().running) {
            OrderedSet<Transition> enabledTransitions;
            boolean macrostepDone = false;
            // Here we handle eventless transitions and transitions
            // triggered by internal events until macrostep is complete
            while (datamodel.global().running && !macrostepDone) {
                enabledTransitions = this.selectEventlessTransitions(datamodel);
                if (enabledTransitions.isEmpty()) {
                    if (datamodel.global().internalQueue.isEmpty()) {
                        macrostepDone = true;
                    } else {
                        if (StaticOptions.trace_method) {
                            this.tracer.enter_method("internalQueue.dequeue");
                        }
                        Event internalEvent = datamodel.global().internalQueue.dequeue();
                        if (StaticOptions.trace_method)
                            this.tracer.exit_method("internalQueue.dequeue");
                        if (StaticOptions.trace_event)
                            this.tracer.event_internal_received(internalEvent);
                        // TODO: Optimize it, set event only once
                        datamodel.set_event(internalEvent);
                        enabledTransitions = this.selectTransitions(datamodel, internalEvent);
                    }
                }
                if (!enabledTransitions.isEmpty()) {
                    this.microstep(datamodel, enabledTransitions.toList());
                }
            }
            // either we're in a final state, and we break out of the loop
            if (!datamodel.global().running) {
                break;
            }
            // or we've completed a macrostep, so we start a new macrostep by waiting for an external event
            // Here we invoke whatever needs to be invoked. The implementation of 'invoke' is platform-specific
            var sortedStatesToInvoke = datamodel.global().statesToInvoke.sort(Fsm.state_entry_order);
            for (Iterator<State> it = sortedStatesToInvoke.iterator(); it.hasNext(); ) {
                var state = it.next();
                for (Iterator<Invoke> iter = state.invoke.sort(Fsm.invoke_document_order).iterator(); iter.hasNext(); ) {
                    var inv = iter.next();
                    this.invoke(datamodel, state, inv);
                }
            }

            Event externalEvent;
            {

                datamodel.global().statesToInvoke.clear();
                // Invoking may have raised internal error events and we iterate to handle them
                if (!datamodel.global().internalQueue.isEmpty()) {
                    continue;
                }

                // W3C says:
                //   A blocking wait for an external event.  Alternatively, if we have been invoked
                //   our parent session also might cancel us.  The mechanism for this is platform specific,
                //   but here we assume it’s a special event we receive
                if (StaticOptions.trace_method)
                    this.tracer.enter_method("externalQueue.dequeue");
                while (true) {
                    var externalEventTmp = datamodel.global().externalQueue.dequeue();
                    if (externalEventTmp.name.startsWith(EVENT_DONE_INVOKE_PREFIX)) {
                        externalEvent = externalEventTmp;
                        break;
                    }
                    if (externalEventTmp.invoke_id != null) {
                        if (!caller_invoke_id.equals((externalEventTmp.invoke_id))) {
                            // W3C says:
                            //    Once it cancels the invoked session, the Processor MUST ignore any events
                            //    it receives from that session. In particular it MUST NOT not insert them
                            //    into the external event queue of the invoking session.
                            // Check if the session is active.
                            if (datamodel.global().child_sessions
                                    .containsKey(externalEventTmp.invoke_id)) {
                                externalEvent = externalEventTmp;
                                break;
                            } else {
                                if (StaticOptions.debug_option)

                                    Log.debug(
                                            "Ignore event %s from invoke %s",
                                            externalEventTmp.name, externalEventTmp.invoke_id
                                    );
                            }
                        } else {
                            externalEvent = externalEventTmp;
                            break;
                        }
                    } else {
                        externalEvent = externalEventTmp;
                        break;
                    }
                }
                if (StaticOptions.trace_method)
                    this.tracer.exit_method("externalQueue.dequeue");
                if (StaticOptions.trace_event)
                    this.tracer.event_external_received(externalEvent);
                if (this.isCancelEvent(externalEvent)) {
                    datamodel.global().running = false;
                    continue;
                }

                if (externalEvent.name.startsWith(EVENT_DONE_INVOKE_PREFIX)) {
                    if (externalEvent.invoke_id != null) {
                        datamodel.global().child_sessions.remove(externalEvent.invoke_id);
                    }
                }
            }
            java.util.List<ExecutableContent> toFinalize = new ArrayList<>();
            java.util.List<String> toForward = new ArrayList<>();

            if (externalEvent.invoke_id != null) {
                ScxmlSession session = datamodel.global().child_sessions.get(externalEvent.invoke_id);
                if (session != null) {
                    // Get state of invokeid
                    if (session.state != null) {
                        var invoke_doc_id = session.invoke_doc_id;
                        for (Iterator<Invoke> it = session.state.invoke.iterator(); it.hasNext(); ) {
                            var inv = it.next();
                            if (Objects.equals(inv.doc_id, invoke_doc_id) && inv.finalize != null) {
                                toFinalize.addAll(inv.finalize.content);
                            }
                            if (inv.autoforward) {
                                toForward.add(externalEvent.invoke_id);
                            }
                        }
                    }
                }
            }

            datamodel.set_event(externalEvent);
            // applyFinalize
            this.executeContent(datamodel, toFinalize);
            for (var invokeId : toForward) {
                // When the 'autoforward' attribute is set to true, the SCXML Processor must send an
                // exact copy of every external event it receives to the invoked process.
                // All the fields specified in 5.10.1 The Internal Structure of Events must have the
                // same values in the forwarded copy of the event. The SCXML Processor must forward
                // the event at the point at which it removes it from the external event queue of
                // the invoking session for processing.
                ScxmlSession session = datamodel.global().child_sessions.get(invokeId);
                if (session == null) {
                    // TODO: Clarify, communication error?
                } else {
                    session.sender.enqueue(externalEvent.get_copy());
                }
            }

            enabledTransitions = this.selectTransitions(datamodel, externalEvent);
            if (!enabledTransitions.isEmpty()) {
                this.microstep(datamodel, enabledTransitions.toList());
            }
        }
        // End of outer while running loop.  If we get here, we have reached a top-level final state or have been cancelled
        this.exitInterpreter(datamodel);
        if (StaticOptions.trace_method) {
            this.tracer.exit_method("mainEventLoop");
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure exitInterpreter()</b><br>
     * The purpose of this procedure is to exit the current SCXML process by exiting all active
     * states. If the machine is in a top-level final state, a Done event is generated.
     * (Note that in this case, the final state will be the only active state.)
     * The implementation of returnDoneEvent is platform-dependent, but if this session is the
     * result of an &lt;invoke> in another SCXML session, returnDoneEvent will cause the event
     * done.invoke.&lt;id> to be placed in the external event queue of that session, where &lt;id> is
     * the id generated in that session when the &lt;invoke> was executed.
     * <pre>
     * procedure exitInterpreter():
     *     statesToExit = configuration.toList().sort(exitOrder)
     *     for s in statesToExit:
     *         for content in s.onexit.sort(documentOrder):
     *             executeContent(content)
     *         for inv in s.invoke:
     *             cancelInvoke(inv)
     *         configuration.delete(s)
     *         if isFinalState(s) and isScxmlElement(s.parent):
     *             returnDoneEvent(s.donedata)
     * </pre>
     */
    protected void exitInterpreter(Datamodel datamodel) {
        var global = datamodel.global();
        if (global.final_configuration != null) {
            global.final_configuration.clear();
            for (Iterator<State> it = global.configuration.iterator(); it.hasNext(); ) {
                global.final_configuration.add(it.next().name);
            }
        }
        List<State> statesToExit = global
                .configuration
                .toList()
                .sort(Fsm.state_exit_order);

        for (var session : global.child_sessions.values()) {
            datamodel.send(
                    ScxmlEventIOProcessor.SCXML_EVENT_PROCESSOR_SHORT_TYPE,
                    new Data.String(String.format("%s%s", ScxmlEventIOProcessor.SCXML_TARGET_SESSION_ID_PREFIX,
                            session.session_id)),
                    Event.new_simple(EVENT_CANCEL_SESSION)
            );
        }

        for (Iterator<State> it = statesToExit.iterator(); it.hasNext(); ) {
            State s = it.next();
            this.executeContent(datamodel, s.onexit);
            global.configuration.delete(s);
            if (this.isFinalState(s) && this.isSCXMLElement(s.parent)) {
                this.returnDoneEvent(s.donedata, datamodel);
            }
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * The implementation of returnDoneEvent is platform-dependent, but if this session is the
     * result of an &lt;invoke> in another SCXML session, returnDoneEvent will cause the event
     * done.invoke.&lt;id> to be placed in the external event queue of that session, where &lt;id> is
     * the id generated in that session when the &lt;invoke> was executed.
     */
    protected void returnDoneEvent(DoneData done_data, Datamodel datamodel) {
        final var global = datamodel.global();

        var caller_invoke_id = global.caller_invoke_id;
        var parent_session_id = global.parent_session_id;

        if (parent_session_id == null) {
            // No parent
        } else {
            if (caller_invoke_id == null) {
                Log.panic("Internal Error: Caller-Invoke-Id not available but Parent-Session-Id is set.");
            } else {
                // TODO: Evaluate done_data, EventType::external ?
                var event = new Event(
                        EVENT_DONE_INVOKE_PREFIX,
                        caller_invoke_id,
                        null,
                        null,
                        EventType.external
                );
                event.invoke_id = caller_invoke_id;
                datamodel.send(
                        ScxmlEventIOProcessor.SCXML_EVENT_PROCESSOR_SHORT_TYPE,
                        new Data.String(String.format("%s%s",
                                ScxmlEventIOProcessor.SCXML_TARGET_SESSION_ID_PREFIX,
                                parent_session_id)), event);
            }
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function selectEventlessTransitions()</b><br>
     * This function selects all transitions that are enabled in the current configuration that
     * do not require an event trigger. First find a transition with no 'event' attribute whose
     * condition evaluates to true. If multiple matching transitions are present, take the first
     * in document order. If none are present, search in the state's ancestors in ancestry order
     * until one is found. As soon as such a transition is found, add it to enabledTransitions,
     * and proceed to the next atomic state in the configuration. If no such transition is found
     * in the state or its ancestors, proceed to the next state in the configuration.<br>
     * When all atomic states have been visited and transitions selected, filter the set of enabled
     * transitions, removing any that are preempted by other transitions, then return the
     * resulting set.
     * <pre>
     * function selectEventlessTransitions():
     *     enabledTransitions = new OrderedSet()
     *     atomicStates = configuration.toList().filter(isAtomicState).sort(documentOrder)
     *     for state in atomicStates:
     *         loop: for s in [state].append(getProperAncestors(state, null)):
     *             for t in s.transition.sort(documentOrder):
     *                 if not t.event and conditionMatch(t):
     *                     enabledTransitions.add(t)
     *                     break loop;
     *     enabledTransitions = removeConflictingTransitions(enabledTransitions)
     *     return enabledTransitions;
     * </pre>
     */
    protected OrderedSet<Transition> selectEventlessTransitions(Datamodel datamodel) {
        var atomicStates = datamodel.global()
                .configuration
                .toList()
                .filter_by(this::isAtomicState)
                .sort(Fsm.state_document_order);

        if (StaticOptions.trace_method)
            this.tracer.enter_method_with_arguments(
                    "selectEventlessTransitions",
                    new Argument("atomicStates", atomicStates));

        OrderedSet<Transition> enabledTransitions = new OrderedSet<>();

        List<State> states = new List<>();
        for (Iterator<State> it = atomicStates.iterator(); it.hasNext(); ) {
            var sid = it.next();
            states.data.clear();
            states.push(sid);
            states.push_set(this.getProperAncestors(sid, null));
            java.util.List<Transition> condT = new ArrayList<>();
            for (Iterator<State> iter = states.iterator(); iter.hasNext(); ) {
                var state = iter.next();
                for (Iterator<Transition> iterator = state.transitions
                        .sort(Fsm.transition_document_order)
                        .iterator(); iterator.hasNext(); ) {
                    var t = iterator.next();
                    if (t.events.isEmpty()) {
                        condT.add(t);
                    }
                }
            }
            for (Transition ct : condT) {
                if (this.conditionMatch(datamodel, ct)) {
                    enabledTransitions.add(ct);
                    break;
                }
            }
        }
        enabledTransitions = this.removeConflictingTransitions(datamodel, enabledTransitions);
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("enabledTransitions", enabledTransitions);
            this.tracer.exit_method("selectEventlessTransitions");
        }
        return enabledTransitions;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function selectTransitions(event)</b><br>
     * The purpose of the selectTransitions()procedure is to collect the transitions that are enabled by this event in
     * the current configuration.<br>
     * <br>
     * Create an empty set of enabledTransitions. For each atomic state , find a transition whose 'event' attribute
     * matches event and whose condition evaluates to true. If multiple matching transitions are present, take the
     * first in document order. If none are present, search in the state's ancestors in ancestry order until one is
     * found. As soon as such a transition is found, add it to enabledTransitions, and proceed to the next atomic state
     * in the configuration. If no such transition is found in the state or its ancestors, proceed to the next state
     * in the configuration. When all atomic states have been visited and transitions selected, filter out any
     * preempted transitions and return the resulting set.
     * <pre>
     * function selectTransitions(event):
     *     enabledTransitions = new OrderedSet()
     *     atomicStates = configuration.toList().filter(isAtomicState).sort(documentOrder)
     *     for state in atomicStates:
     *         loop: for s in [state].append(getProperAncestors(state, null)):
     *             for t in s.transition.sort(documentOrder):
     *                 if t.event and nameMatch(t.event, event.name) and conditionMatch(t):
     *                     enabledTransitions.add(t)
     *                     break loop;
     *     enabledTransitions = removeConflictingTransitions(enabledTransitions)
     *     return enabledTransitions;
     * </pre>
     */
    protected OrderedSet<Transition> selectTransitions(Datamodel datamodel, Event event) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method("selectTransitions");
        }
        final GlobalData gd = datamodel.global();

        OrderedSet<Transition> enabledTransitions = new OrderedSet<>();
        var atomicStates = gd.configuration.toList()
                .filter_by(this::isAtomicState)
                .sort(Fsm.state_document_order);
        for (State state : atomicStates.data) {
            java.util.List<Transition> condT = new ArrayList<>();
            for (State s : List.from_array(new State[]{state})
                    .append_set(this.getProperAncestors(state, null))
                    .data) {
                java.util.List<Transition> transition = new ArrayList<>(s.transitions.data);
                transition.sort(Fsm.transition_document_order);
                for (Transition t : transition) {
                    if ((!t.events.isEmpty()) && t.nameMatch(event.name)) {
                        condT.add(t);
                    }
                }
            }
            for (Transition ct : condT) {
                if (this.conditionMatch(datamodel, ct)) {
                    enabledTransitions.add(ct);
                    break;
                }
            }
        }
        enabledTransitions = this.removeConflictingTransitions(datamodel, enabledTransitions);
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("enabledTransitions", enabledTransitions);
            this.tracer.exit_method("selectTransitions");
        }
        return enabledTransitions;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function removeConflictingTransitions(enabledTransitions)</b><br>
     * enabledTransitions will contain multiple transitions only if a parallel state is active.
     * In that case, we may have one transition selected for each of its children.<br>
     * These transitions may conflict with each other in the sense that they have incompatible
     * target states. Loosely speaking, transitions are compatible when each one is contained
     * within a single &lt;state> child of the &lt;parallel> element.<br>
     * Transitions that aren't contained within a single child force the state
     * machine to leave the &lt;parallel> ancestor (even if they reenter it later). Such transitions
     * conflict with each other, and with transitions that remain within a single &lt;state> child, in that
     * they may have targets that cannot be simultaneously active. The test that transitions have non-
     * intersecting exit sets captures this requirement. (If the intersection is null, the source and
     * targets of the two transitions are contained in separate &lt;state> descendants of &lt;parallel>.
     * If intersection is non-null, then at least one of the transitions is exiting the &lt;parallel>).<br>
     * When such a conflict occurs, then if the source state of one of the transitions is a descendant
     * of the source state of the other, we select the transition in the descendant. Otherwise we prefer
     * the transition that was selected by the earlier state in document order and discard the other
     * transition. Note that targetless transitions have empty exit sets and thus do not conflict with
     * any other transitions.<br>
     * <br>
     * We start with a list of enabledTransitions and produce a conflict-free list of filteredTransitions.
     * For each t1 in enabledTransitions, we test it against all t2 that are already selected in
     * filteredTransitions. If there is a conflict, then if t1's source state is a descendant of
     * t2's source state, we prefer t1 and say that it preempts t2
     * (so we make a note to remove t2 from filteredTransitions).<br>
     * Otherwise, we prefer t2 since it was selected in an earlier state in document order,
     * so we say that it preempts t1.
     * (There's no need to do anything in this case since t2 is already in filteredTransitions.
     * Furthermore, once one transition preempts t1, there is no need to test t1 against any other
     * transitions.)<br>
     * Finally, if t1 isn't preempted by any transition in filteredTransitions, remove any
     * transitions that it preempts and add it to that list.
     * <pre>
     * function removeConflictingTransitions(enabledTransitions):
     *     filteredTransitions = new OrderedSet()
     *     //toList sorts the transitions in the order of the states that selected them
     *     for t1 in enabledTransitions.toList():
     *         t1Preempted = false
     *         transitionsToRemove = new OrderedSet()
     *         for t2 in filteredTransitions.toList():
     *             if computeExitSet([t1]).hasIntersection(computeExitSet([t2])):
     *                 if isDescendant(t1.source, t2.source):
     *                     transitionsToRemove.add(t2)
     *                 else:
     *                     t1Preempted = true
     *                     break
     *         if not; t1Preempted:
     *             for t3 in transitionsToRemove.toList():
     *                 filteredTransitions.delete(t3)
     *             filteredTransitions.add(t1)
     *
     *     return filteredTransitions;
     * </pre>
     */
    protected OrderedSet<Transition> removeConflictingTransitions(
            Datamodel datamodel, OrderedSet<Transition> enabledTransitions) {

        OrderedSet<Transition> filteredTransitions = new OrderedSet<>();
        //toList sorts the transitions in the order of the states that selected them
        for (Transition t1 : enabledTransitions.toList().data) {
            boolean t1Preempted = false;
            OrderedSet<Transition> transitionsToRemove = new OrderedSet<>();
            var filteredTransitionList = filteredTransitions.toList();
            for (Transition t2 : filteredTransitionList.data) {
                if (this.computeExitSet(datamodel, List.from_array(new Transition[]{t1}))
                        .hasIntersection(this.computeExitSet(datamodel, List.from_array(new Transition[]{t2})))) {
                    if (this.isDescendant(t1.source, t2.source)) {
                        transitionsToRemove.add(t2);
                    } else {
                        t1Preempted = true;
                        break;
                    }
                }
            }
            if (!t1Preempted) {
                for (Transition t3 : transitionsToRemove.toList().data) {
                    filteredTransitions.delete(t3);
                }
                filteredTransitions.add(t1);
            }
        }
        return filteredTransitions;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure microstep(enabledTransitions)</b><br>
     * The purpose of the microstep procedure is to process a single set of transitions.<br>
     * These may have been enabled by an external event, an internal event, or by the presence or absence of certain
     * values in the data model at the current point in time. The processing of the enabled transitions must be done in
     * parallel ('lock step') in the sense that their source states must first be exited, then their actions must be
     * executed, and finally their target states entered.<br>
     * <br>
     * If a single atomic state is active, then enabledTransitions will contain only a single transition.
     * If multiple states are active (i.e., we are in a parallel region), then there may be multiple transitions, one
     * per active atomic state (though some states may not select a transition.) In this case, the transitions are
     * taken in the document order of the atomic states that selected them.
     * <pre>
     *  procedure microstep(enabledTransitions):
     *      exitStates(enabledTransitions)
     *      executeTransitionContent(enabledTransitions)
     *      enterStates(enabledTransitions)
     * </pre>
     */
    protected void microstep(Datamodel datamodel, List<Transition> enabledTransitions) {
        if (StaticOptions.trace_method)
            this.tracer.enter_method("microstep");
        if (StaticOptions.debug_option) {
            if (enabledTransitions.size() > 0) {
                if (enabledTransitions.size() > 1) {
                    Log.debug("Enabled Transitions:");
                    for (var t : enabledTransitions.data) {
                        Log.debug("\t%s", t);
                    }
                } else {
                    Log.debug("Enabled Transition %s", enabledTransitions.head());
                }
            }
        }
        this.exitStates(datamodel, enabledTransitions);
        this.executeTransitionContent(datamodel, enabledTransitions);
        this.enterStates(datamodel, enabledTransitions);
        if (StaticOptions.trace_method)
            this.tracer.exit_method("microstep");
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure exitStates(enabledTransitions)</b><br>
     * Compute the set of states to exit. Then remove all the states on statesToExit from the set
     * of states that will have invoke processing done at the start of the next macrostep.<br>
     * (Suppose macrostep M1 consists of microsteps m11 and m12. We may enter state s in m11 and
     * exit it in m12. We will add s to statesToInvoke in m11, and must remove it in m12. In the
     * subsequent macrostep M2, we will apply invoke processing to all states that were entered,
     * and not exited, in M1.) Then convert statesToExit to a list and sort it in exitOrder.<br>
     * <br>
     * For each state s in the list, if s has a deep history state h, set the history value of h
     * to be the list of all atomic descendants of s that are members in the current configuration,
     * else set its value to be the list of all immediate children of s that are members of the
     * current configuration. Again for each state s in the list, first execute any onexit
     * handlers, then cancel any ongoing invocations, and finally remove s from the current
     * configuration.
     *
     * <pre>
     * procedure exitStates(enabledTransitions):
     *     statesToExit = computeExitSet(enabledTransitions)
     *     for s in statesToExit:
     *         statesToInvoke.delete(s)
     *     statesToExit = statesToExit.toList().sort(exitOrder)
     *     for s in statesToExit:
     *         for h in s.history:
     *             if h.type == "deep":
     *                 f = lambda s0: isAtomicState(s0) and isDescendant(s0,s)
     *             else:
     *                 f = lambda s0: s0.parent == s
     *             historyValue[h.id] = configuration.toList().filter(f)
     *     for s in statesToExit:
     *         for content in s.onexit.sort(documentOrder):
     *             executeContent(content)
     *         for inv in s.invoke:
     *             cancelInvoke(inv)
     *         configuration.delete(s)
     * </pre>
     */
    protected void exitStates(Datamodel datamodel, List<Transition> enabledTransitions) {
        if (StaticOptions.trace_method)
            this.tracer.enter_method("exitStates");

        final GlobalData gd = datamodel.global();

        var statesToExit = this.computeExitSet(datamodel, enabledTransitions);
        for (State s : statesToExit.data) {
            gd.statesToInvoke.delete(s);
        }
        statesToExit = statesToExit.sort(Fsm.state_exit_order);

        HashTable<State, OrderedSet<State>> ahistory = new HashTable<>();
        var configStateList = gd.configuration.toList();

        for (State s : statesToExit.data) {
            for (var h : s.history.data) {
                if (h.history_type == HistoryType.Deep) {
                    OrderedSet<State> stateIdList = configStateList
                            .filter_by(s0 -> this.isAtomicState(s0) && this.isDescendant(s0, s)).to_set();
                    ahistory.put(h, stateIdList);
                } else {
                    var fl = gd.configuration
                            .toList()
                            .filter_by(s0 -> s0.parent == s).to_set();
                    ahistory.put(h, fl);
                }
            }
        }

        gd.historyValue.put_all(ahistory);

        for (State s : statesToExit.data) {
            // Use the document-id of Invoke to identify sessions to cancel.
            HashSet<Integer> invoke_doc_ids = new HashSet<>();
            List<ExecutableContent> exitList = new List<>();
            if (StaticOptions.trace_state)
                this.tracer.trace_exit_state(s);
            for (var inv : s.invoke.data) {
                invoke_doc_ids.add(inv.doc_id);
            }
            for (var ec : s.onexit) {
                exitList.push(ec);
            }
            if (!invoke_doc_ids.isEmpty()) {
                for (var item : gd.child_sessions.entrySet()) {
                    if (invoke_doc_ids.contains(item.getValue().invoke_doc_id)) {
                        this.cancelInvoke(datamodel, item.getKey(), item.getValue());
                    }
                }
            }
            this.executeContent(datamodel, exitList.data);
            gd.configuration.delete(s);
        }
        if (StaticOptions.trace_method)
            this.tracer.exit_method("exitStates");
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure enterStates(enabledTransitions)</b><br>
     * First, compute the list of all the states that will be entered as a result of taking the
     * transitions in enabledTransitions. Add them to statesToInvoke so that invoke processing can
     * be done at the start of the next macrostep. Convert statesToEnter to a list and sort it in
     * entryOrder. For each state s in the list, first add s to the current configuration.
     * Then if we are using late binding, and this is the first time we have entered s, initialize
     * its data model. Then execute any onentry handlers. If s's initial state is being entered by
     * default, execute any executable content in the initial transition. If a history state in s
     * was the target of a transition, and s has not been entered before, execute the content
     * inside the history state's default transition. Finally, if s is a final state, generate
     * relevant Done events. If we have reached a top-level final state, set running to false as a
     * signal to stop processing.
     * <pre>
     *    procedure enterStates(enabledTransitions):
     *        statesToEnter = new OrderedSet()
     *        statesForDefaultEntry = new OrderedSet()
     *        // initialize the temporary table for default content in history states
     *        defaultHistoryContent = new HashTable()
     *        computeEntrySet(enabledTransitions, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     *        for s in statesToEnter.toList().sort(entryOrder):
     *           configuration.add(s)
     *           statesToInvoke.add(s)
     *           if binding == "late" and s.isFirstEntry:
     *              initializeDataModel(datamodel.s,doc.s)
     *              s.isFirstEntry = false
     *           for content in s.onentry.sort(documentOrder):
     *              executeContent(content)
     *           if statesForDefaultEntry.isMember(s):
     *              executeContent(s.initial.transition)
     *           if defaultHistoryContent[s.id]:
     *              executeContent(defaultHistoryContent[s.id])
     *           if isFinalState(s):
     *              if isSCXMLElement(s.parent):
     *                 running = false
     *              else:
     *                 parent = s.parent
     *                 grandparent = parent.parent
     *                 internalQueue.enqueue(new Event("done.state." + parent.id, s.donedata))
     *                 if isParallelState(grandparent):
     *                    if getChildStates(grandparent).every(isInFinalState):
     *                       internalQueue.enqueue(new Event("done.state." + grandparent.id))
     * </pre>
     */
    protected void enterStates(Datamodel datamodel, List<Transition> enabledTransitions) {
        if (StaticOptions.trace_method)
            this.tracer.enter_method("enterStates");

        var gd = datamodel.global();
        OrderedSet<State> statesToEnter = new OrderedSet<>();
        OrderedSet<State> statesForDefaultEntry = new OrderedSet<>();

        // initialize the temporary table for default content in history states
        HashTable<State, ExecutableContentRegion> defaultHistoryContent = new HashTable<>();
        this.computeEntrySet(datamodel, enabledTransitions, statesToEnter, statesForDefaultEntry, defaultHistoryContent);
        for (State s : statesToEnter.toList().sort(Fsm.state_entry_order).data) {
            if (StaticOptions.trace_state)
                this.tracer.trace_enter_state(s);
            gd.configuration.add(s);
            gd.statesToInvoke.add(s);
            State to_init = null;
            {
                if (this.binding == BindingType.Late && s.isFirstEntry) {
                    to_init = s;
                    s.isFirstEntry = false;
                }
            }
            if (to_init != null) {
                datamodel.initializeDataModel(this, to_init, true);
            }
            java.util.List<ExecutableContent> exe = new ArrayList<>(s.onentry);
            if (statesForDefaultEntry.isMember(s) && s.initial != null && s.initial.content != null) {
                exe.addAll(s.initial.content.content);
            }
            if (defaultHistoryContent.has(s)) {
                exe.addAll(defaultHistoryContent.get(s).content);
            }

            this.executeContent(datamodel, exe);

            if (this.isFinalState(s)) {
                State parent = s.parent;
                if (this.isSCXMLElement(parent)) {
                    gd.running = false;
                } else {
                    java.util.List<ParamPair> name_values = new ArrayList<>();
                    Data content = null;
                    if (s.donedata != null) {
                        datamodel.evaluate_params(s.donedata.params, name_values);
                        content = datamodel.evaluate_content(s.donedata.content).getCopy();
                    }
                    var param_values = name_values.isEmpty() ? null : name_values;

                    this.enqueue_internal(
                            datamodel,
                            // TODO: EventType::external ?
                            new Event(
                                    "done.state.",
                                    parent.name,
                                    param_values,
                                    content,
                                    EventType.external)
                    );
                    State grandparent = parent.parent;
                    if (grandparent != null && this.isParallelState(grandparent)
                            && this.getChildStates(grandparent)
                            .every((state) -> this.isInFinalState(datamodel, state))) {
                        this.enqueue_internal(
                                datamodel,
                                // TODO: EventType::external ?
                                new Event("done.state.", grandparent.name, null, null,
                                        EventType.external)
                        );
                    }
                }
            }
        }
        if (StaticOptions.trace_method)
            this.tracer.exit_method("enterStates");
    }

    /**
     * Puts an event into the internal queue.
     */
    protected void enqueue_internal(Datamodel datamodel, Event event) {
        if (StaticOptions.trace_event)
            this.tracer.event_internal_send(event);
        datamodel.global().internalQueue.enqueue(event);
    }

    protected void executeContent(@NotNull Datamodel datamodel, @Nullable ExecutableContentRegion content) {
        if (content != null)
            executeContent(datamodel, content.content);
    }

    protected void executeContent(@NotNull Datamodel datamodel, @Nullable java.util.List<ExecutableContent> content) {
        if (content != null)
            for (var ct : content)
                executeContent(datamodel, ct);
    }


    protected void executeContent(@NotNull Datamodel datamodel, @Nullable ExecutableContent content) {
        if (content != null) {
            if (StaticOptions.trace_method) {
                this.tracer.enter_method_with_arguments("executeContent",
                        new Argument("content", content));
            }
            datamodel.executeContent(this, content);
            if (StaticOptions.trace_method)
                this.tracer.exit_method("executeContent");
        }
    }

    public boolean isParallelState(State state) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("isParallelState",
                    new Argument("state", state.name));
        }
        final boolean b = state.is_parallel;
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("parallel", Boolean.toString(b));
            this.tracer.exit_method("isParallelState");
        }
        return b;
    }

    protected boolean isSCXMLElement(State state) {
        return state == this.pseudo_root;
    }

    protected boolean isFinalState(State state) {
        return state != null && state.is_final;
    }

    protected boolean isAtomicState(State state) {
        return state != null && state.states.isEmpty();
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure computeExitSet(enabledTransitions)</b><br>
     * For each transition t in enabledTransitions, if t is targetless then do nothing, else compute the transition's domain.
     * (This will be the source state in the case of internal transitions) or the least common compound ancestor
     * state of the source state and target states of t (in the case of external transitions. Add to the statesToExit
     * set all states in the configuration that are descendants of the domain.
     * <pre>
     * function computeExitSet(transitions)
     *     statesToExit = new OrderedSet
     *     for t in transitions:
     *         if t.target:
     *             domain = getTransitionDomain(t)
     *             for s in configuration:
     *                 if isDescendant(s,domain):
     *                     statesToExit.add(s)
     *     return statesToExit;
     * </pre>
     */
    protected OrderedSet<State> computeExitSet(Datamodel datamodel, List<Transition> transitions) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("computeExitSet",
                    new Argument("transitions", transitions));
        }
        OrderedSet<State> statesToExit = new OrderedSet<>();
        for (var t : transitions.data) {
            if (!t.target.isEmpty()) {
                State domain = this.getTransitionDomain(datamodel, t);
                for (var s : datamodel.global().configuration.data) {
                    if (this.isDescendant(s, domain)) {
                        statesToExit.add(s);
                    }
                }
            }
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("statesToExit", statesToExit);
            this.tracer.exit_method("computeExitSet");
        }
        return statesToExit;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure executeTransitionContent(enabledTransitions)</b><br>
     * For each transition in the list of enabledTransitions, execute its executable content.
     * <pre>
     * procedure executeTransitionContent(enabledTransitions):
     *     for t in enabledTransitions:
     *         executeContent(t)
     * </pre>
     */
    protected void executeTransitionContent(Datamodel datamodel, List<Transition> enabledTransitions) {
        for (Transition t : enabledTransitions.data) {
            this.executeContent(datamodel, t.content);
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure computeEntrySet(transitions, statesToEnter, statesForDefaultEntry, defaultHistoryContent)</b><br>
     * Compute the complete set of states that will be entered as a result of taking 'transitions'.
     * This value will be returned in 'statesToEnter' (which is modified by this procedure). Also
     * place in 'statesForDefaultEntry' the set of all states whose default initial states were
     * entered. First gather up all the target states in 'transitions'. Then add them and, for all
     * that are not atomic states, add all of their (default) descendants until we reach one or
     * more atomic states. Then add any ancestors that will be entered within the domain of the
     * transition. (Ancestors outside of the domain of the transition will not have been exited.)
     * <pre>
     * procedure computeEntrySet(transitions, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     *     for t in transitions:
     *         for s in t.target:
     *             addDescendantStatesToEnter(s,statesToEnter,statesForDefaultEntry, defaultHistoryContent)
     *         ancestor = getTransitionDomain(t)
     *         for s in getEffectiveTargetStates(t):
     *             addAncestorStatesToEnter(s, ancestor, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     * </pre>
     */
    protected void computeEntrySet(
            Datamodel datamodel,
            List<Transition> transitions,
            OrderedSet<State> statesToEnter,
            OrderedSet<State> statesForDefaultEntry,
            HashTable<State, ExecutableContentRegion> defaultHistoryContent
    ) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("computeEntrySet",
                    new Argument("transitions", transitions));
        }
        for (Transition t : transitions.data) {
            for (var s : t.target) {
                this.addDescendantStatesToEnter(
                        datamodel,
                        s,
                        statesToEnter,
                        statesForDefaultEntry,
                        defaultHistoryContent
                );
            }
            var ancestor = this.getTransitionDomain(datamodel, t);
            for (State s : this.getEffectiveTargetStates(datamodel, t).data) {
                this.addAncestorStatesToEnter(
                        datamodel,
                        s,
                        ancestor,
                        statesToEnter,
                        statesForDefaultEntry,
                        defaultHistoryContent
                );
            }
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("statesToEnter>", statesToEnter);
            this.tracer.exit_method("computeEntrySet");
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure addDescendantStatesToEnter(state,statesToEnter,statesForDefaultEntry, defaultHistoryContent)</b><br>
     * The purpose of this procedure is to add to statesToEnter 'state' and any of its descendants
     * that the state machine will end up entering when it enters 'state'. (N.B. If 'state' is a
     * history pseudo-state, we dereference it and add the history value instead.) Note that this '
     * procedure permanently modifies both statesToEnter and statesForDefaultEntry.<br>
     * <br>
     * First, If state is a history state then add either the history values associated with state or state's default
     * target to statesToEnter. Then (since the history value may not be an immediate descendant of 'state's parent)
     * add any ancestors between the history value and state's parent. Else (if state is not a history state),
     * add state to statesToEnter. Then if state is a compound state, add state to statesForDefaultEntry and
     * recursively call addStatesToEnter on its default initial state(s). Then, since the default initial states
     * may not be children of 'state', add any ancestors between the default initial states and 'state'.
     * Otherwise, if state is a parallel state, recursively call addStatesToEnter on any of its child states that
     * don't already have a descendant on statesToEnter.
     * <pre>
     * procedure addDescendantStatesToEnter(state,statesToEnter,statesForDefaultEntry, defaultHistoryContent):
     *     if isHistoryState(state):
     *         if historyValue[state.id]:
     *             for s in historyValue[state.id]:
     *                 addDescendantStatesToEnter(s,statesToEnter,statesForDefaultEntry, defaultHistoryContent)
     *             for s in historyValue[state.id]:
     *                 addAncestorStatesToEnter(s, state.parent, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     *         else:
     *             defaultHistoryContent[state.parent.id] = state.transition.content
     *             for s in state.transition.target:
     *                 addDescendantStatesToEnter(s,statesToEnter,statesForDefaultEntry, defaultHistoryContent)
     *             for s in state.transition.target:
     *                 addAncestorStatesToEnter(s, state.parent, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     *     else:
     *         statesToEnter.add(state)
     *         if isCompoundState(state):
     *             statesForDefaultEntry.add(state)
     *             for s in state.initial.transition.target:
     *                 addDescendantStatesToEnter(s,statesToEnter,statesForDefaultEntry, defaultHistoryContent)
     *             for s in state.initial.transition.target:
     *                 addAncestorStatesToEnter(s, state, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     *         else:
     *             if isParallelState(state):
     *                 for child in getChildStates(state):
     *                     if not statesToEnter.some(lambda s: isDescendant(s,child)):
     *                         addDescendantStatesToEnter(child,statesToEnter,statesForDefaultEntry, defaultHistoryContent)
     * </pre>
     */
    protected void addDescendantStatesToEnter(
            Datamodel datamodel,
            State state,
            OrderedSet<State> statesToEnter,
            OrderedSet<State> statesForDefaultEntry,
            HashTable<State, ExecutableContentRegion> defaultHistoryContent
    ) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("addDescendantStatesToEnter",
                    new Argument("State", state));
        }
        var global = datamodel.global();
        if (this.isHistoryState(state)) {
            var hv = global.historyValue.get(state);
            if (hv != null) {
                for (State s : hv.data) {
                    this.addDescendantStatesToEnter(
                            datamodel,
                            s,
                            statesToEnter,
                            statesForDefaultEntry,
                            defaultHistoryContent
                    );
                }
                for (State s : hv.data) {
                    this.addAncestorStatesToEnter(
                            datamodel,
                            s,
                            state.parent,
                            statesToEnter,
                            statesForDefaultEntry,
                            defaultHistoryContent
                    );
                }
            } else {
                // A history state have exactly one transition which specified the default history configuration.
                var defaultTransition = state.transitions.head();
                defaultHistoryContent.put(state.parent, defaultTransition.content);
                for (State s : defaultTransition.target) {
                    this.addDescendantStatesToEnter(
                            datamodel,
                            s,
                            statesToEnter,
                            statesForDefaultEntry,
                            defaultHistoryContent
                    );
                }
                for (State s : defaultTransition.target) {
                    this.addAncestorStatesToEnter(
                            datamodel,
                            s,
                            state.parent,
                            statesToEnter,
                            statesForDefaultEntry,
                            defaultHistoryContent
                    );
                }
            }
        } else {
            statesToEnter.add(state);
            if (this.isCompoundState(state)) {
                statesForDefaultEntry.add(state);
                if (state.initial != null) {
                    var initialTransition = state.initial;
                    for (State s : initialTransition.target) {
                        this.addDescendantStatesToEnter(
                                datamodel,
                                s,
                                statesToEnter,
                                statesForDefaultEntry,
                                defaultHistoryContent
                        );
                    }
                    for (State s : initialTransition.target) {
                        this.addAncestorStatesToEnter(
                                datamodel,
                                s,
                                state,
                                statesToEnter,
                                statesForDefaultEntry,
                                defaultHistoryContent
                        );
                    }
                }
            } else if (this.isParallelState(state)) {
                for (var child : state.states) {
                    if (!statesToEnter.some((s) -> this.isDescendant(s, child))) {
                        this.addDescendantStatesToEnter(
                                datamodel,
                                child,
                                statesToEnter,
                                statesForDefaultEntry,
                                defaultHistoryContent
                        );
                    }
                }
            }
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("statesToEnter", statesToEnter);
            this.tracer.exit_method("addDescendantStatesToEnter");
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure addAncestorStatesToEnter(state, ancestor, statesToEnter, statesForDefaultEntry, defaultHistoryContent)</b>:<br>
     * Add to statesToEnter any ancestors of 'state' up to, but not including, 'ancestor' that must be entered in order to enter 'state'. If any of these ancestor states is a parallel state, we must fill in its descendants as well.
     * <pre>
     * procedure addAncestorStatesToEnter(state, ancestor, statesToEnter, statesForDefaultEntry, defaultHistoryContent)
     *     for anc in getProperAncestors(state,ancestor):
     *         statesToEnter.add(anc)
     *         if isParallelState(anc):
     *             for child in getChildStates(anc):
     *                 if not statesToEnter.some(lambda s: isDescendant(s,child)):
     *                     addDescendantStatesToEnter(child,statesToEnter,statesForDefaultEntry, defaultHistoryContent)
     * </pre>
     */
    protected void addAncestorStatesToEnter(
            Datamodel datamodel,
            State state, State ancestor,
            OrderedSet<State> statesToEnter,
            OrderedSet<State> statesForDefaultEntry,
            HashTable<State, ExecutableContentRegion> defaultHistoryContent
    ) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("addAncestorStatesToEnter",
                    new Argument("state", state));
        }
        for (var anc : this.getProperAncestors(state, ancestor).data) {
            statesToEnter.add(anc);
            if (this.isParallelState(anc)) {
                for (var child : anc.states) {
                    if (!statesToEnter.some(s -> this.isDescendant(s, child))) {
                        this.addDescendantStatesToEnter(
                                datamodel,
                                child,
                                statesToEnter,
                                statesForDefaultEntry,
                                defaultHistoryContent
                        );
                    }
                }
            }
        }
        if (StaticOptions.trace_method)
            this.tracer.exit_method("addAncestorStatesToEnter");
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>procedure isInFinalState(s)</b><br>
     * Return true if s is a compound &lt;state> and one of its children is an active &lt;final> state
     * (i.e. is a member of the current configuration), or if s is a &lt;parallel> state and
     * isInFinalState is true of all its children.
     * <pre>
     * function isInFinalState(s):
     *     if isCompoundState(s):
     *         return getChildStates(s).some(lambda s: isFinalState(s) and configuration.isMember(s));
     *     elif isParallelState(s):
     *         return getChildStates(s).every(isInFinalState);
     *     else:
     *         return false;
     * </pre>
     */
    protected boolean isInFinalState(Datamodel datamodel, State s) {
        if (this.isCompoundState(s)) {
            final GlobalData gd = datamodel.global();
            return this.getChildStates(s).some(cs -> this.isFinalState(cs) && gd.configuration.isMember(cs));
        } else if (this.isParallelState(s)) {
            return this.getChildStates(s)
                    .every(cs -> this.isInFinalState(datamodel, cs));
        } else {
            return false;
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function getTransitionDomain(transition)</b><br>
     * Return the compound state such that<ol>
     * <li>all states that are exited or entered as a result of taking 'transition' are descendants of it</li>
     * <li>no descendant of it has this property.</li>
     * </ol>
     * <pre>
     * function getTransitionDomain(t)
     *     tstates = getEffectiveTargetStates(t)
     *     if not tstates:
     *         return null;
     *     elif t.type == "internal" and isCompoundState(t.source) and tstates.every(lambda s: isDescendant(s,t.source)):
     *         return t.source;
     *     else:
     *         return findLCCA([t.source].append(tstates));
     * </pre>
     */
    @Nullable
    protected State getTransitionDomain(@NotNull Datamodel datamodel, Transition t) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("getTransitionDomain",
                    new Argument("t", t));
        }
        var tstates = this.getEffectiveTargetStates(datamodel, t);
        State domain;
        if (tstates.isEmpty()) {
            domain = null;
        } else if (t.transition_type == TransitionType.Internal
                && this.isCompoundState(t.source)
                && tstates.every((s) -> this.isDescendant(s, t.source))) {
            domain = t.source;
        } else {
            List<State> l = new List<>();
            l.push(t.source);
            domain = this.findLCCA(l.append_set(tstates));
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("domain", domain);
            this.tracer.exit_method("getTransitionDomain");
        }
        return domain;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function findLCCA(stateList)</b><br>
     * The Least Common Compound Ancestor is the &lt;state> or &lt;scxml> element s such that s is a
     * proper ancestor of all states on stateList and no descendant of s has this property.
     * Note that there is guaranteed to be such an element since the &lt;scxml> wrapper element is a
     * common ancestor of all states. Note also that since we are speaking of proper ancestor
     * (parent or parent of a parent, etc.) the LCCA is never a member of stateList.
     * <pre>
     * function findLCCA(stateList):
     *     for anc in getProperAncestors(stateList.head(),null).filter(isCompoundStateOrScxmlElement):
     *         if stateList.tail().every(lambda s: isDescendant(s,anc)):
     *             return anc;
     * </pre>
     */
    protected State findLCCA(List<State> stateList) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("findLCCA",
                    new Argument("stateList", stateList));
        }
        State lcca = null;
        for (var anc : this.getProperAncestors(stateList.head(), null).toList()
                .filter_by(this::isCompoundStateOrScxmlElement).data) {
            if (stateList.tail().every(s -> this.isDescendant(s, anc))) {
                lcca = anc;
                break;
            }
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("lcca", lcca);
            this.tracer.exit_method("findLCCA");
        }
        return lcca;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function getEffectiveTargetStates(transition)</b><br>
     * Returns the states that will be the target when 'transition' is taken, dereferencing any history states.
     * <pre>
     * function getEffectiveTargetStates(transition)
     *     targets = new OrderedSet()
     *     for s in transition.target
     *         if isHistoryState(s):
     *             if historyValue[s.id]:
     *                 targets.union(historyValue[s.id])
     *             else:
     *                 targets.union(getEffectiveTargetStates(s.transition))
     *         else:
     *             targets.add(s)
     *     return targets;
     * </pre>
     */
    protected OrderedSet<State> getEffectiveTargetStates(Datamodel datamodel, Transition transition) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("getEffectiveTargetStates",
                    new Argument("transition", transition));
        }
        OrderedSet<State> targets = new OrderedSet<>();
        for (State state : transition.target) {
            if (this.isHistoryState(state)) {
                if (datamodel.global().historyValue.has(state)) {
                    targets.union(datamodel.global().historyValue.get(state));
                } else {
                    // History states have exactly one "transition"
                    targets.union(this.getEffectiveTargetStates(datamodel, state.transitions.head()));
                }
            } else {
                targets.add(state);
            }
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("targets", targets);
            this.tracer.exit_method("getEffectiveTargetStates");
        }
        return targets;
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function getProperAncestors(state1, state2)</b><br>
     * If state2 is null, returns the set of all ancestors of state1 in ancestry order
     * (state1's parent followed by the parent's parent, etc. up to an including the &lt;scxml> element).
     * If state2 is non-null, returns in ancestry order the set of all ancestors of state1,
     * up to but not including state2.<br>
     * (A "proper ancestor" of a state is its parent, or the parent's parent,
     * or the parent's parent's parent, etc.))<br>
     * If state2 is state1's parent, or equal to state1, or a descendant of state1, this returns the empty set.
     */
    protected OrderedSet<State> getProperAncestors(@NotNull State state1, @Nullable State state2) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("getProperAncestors",
                    new Argument("state1", state1),
                    new Argument("state2", state2));
        }

        OrderedSet<State> properAncestors = new OrderedSet<>();
        if (!this.isDescendant(state2, state1)) {
            State currState = state1.parent;
            while (currState != null && currState != state2) {
                properAncestors.add(currState);
                currState = currState.parent;
            }
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("properAncestors", properAncestors);
            this.tracer.exit_method("getProperAncestors");
        }
        return properAncestors;
    }

    /**
     * <b>W3C says</b>:<br>
     * function isDescendant(state1, state2)
     * Returns 'true' if state1 is a descendant of state2 (a child, or a child of a child, or a child of a child of a child, etc.) Otherwise returns 'false'.
     */
    protected boolean isDescendant(State state1, State state2) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("isDescendant",
                    new Argument("state1", state1),
                    new Argument("state2", state2));
        }
        boolean result;
        if (state1 == null || state2 == null || state1 == state2) {
            result = false;
        } else {
            State currState = state1.parent;
            while (currState != null && currState != state2) {
                currState = currState.parent;
            }
            result = (currState == state2);
        }
        if (StaticOptions.trace_method) {
            this.tracer.trace_result("result", result);
            this.tracer.exit_method("isDescendant");
        }
        return result;
    }

    /**
     * <b>W3C says</b>:<br>
     * A Compound State: A state of type &lt;state\> with at least one child state.
     */
    protected boolean isCompoundState(State state) {
        if (state != null) {
            return !(state.is_final || state.is_parallel || state.states.isEmpty());
        } else {
            return false;
        }
    }

    protected boolean isCompoundStateOrScxmlElement(State state) {
        if (state == this.pseudo_root) {
            return true;
        } else {
            return this.isCompoundState(state);
        }
    }

    protected boolean isHistoryState(State state) {
        return state != null && state.history_type != HistoryType.None;
    }

    protected boolean isCancelEvent(Event event) {
        // Cancel-Events (outer fsm cancels a fsm instance that was started by some invoke)
        // are platform specific.
        return event != null && EVENT_CANCEL_SESSION.equals(event.name);
    }

    /**
     * <b>W3C says</b>:<br>
     * <b>function getChildStates(state1)</b><br>
     * Returns a list containing all &lt;state\>, &lt;final\>, and &lt;parallel\> children of state1.
     */
    protected List<State> getChildStates(State state1) {
        List<State> l = new List<>();
        l.data.addAll(state1.states);
        return l;
    }

    protected void invoke(Datamodel datamodel, State state, Invoke inv) {

        if (StaticOptions.trace_method) {
            this.tracer.enter_method_with_arguments("invoke",
                    new Argument("state", state),
                    new Argument("inv", inv));
        }

        final GlobalData global = datamodel.global();

        // W3C: if the evaluation of its arguments produces an error, the SCXML Processor must
        // terminate the processing of the element without further action.

        Data type_name_data = datamodel.get_expression_alternative_value(inv.type_name, inv.type_expr);
        if (type_name_data == null || type_name_data instanceof Data.Error) {
            // Error -> abort
            if (StaticOptions.trace_method) {
                this.tracer.exit_method("invoke");
            }
            return;
        }

        String type_name = type_name_data.toString();
        if (Datamodel.SCXML_INVOKE_TYPE_SHORT.equals(type_name)) {
            type_name = Datamodel.SCXML_INVOKE_TYPE;
        }

        if (!(type_name.isEmpty()
                || (type_name.startsWith(Datamodel.SCXML_INVOKE_TYPE) && type_name.length() <= (Datamodel.SCXML_INVOKE_TYPE.length() + 1)))) {
            Log.error("Unsupported <invoke> type %s", type_name);
            if (StaticOptions.trace_method) {
                this.tracer.exit_method("invoke");
            }
            return;
        }

        String invokeId = inv.invoke_id;
        if (invokeId.isEmpty()) {
            // W3C:
            // A conformant SCXML document may specify either the 'id' or 'idlocation' attribute, but
            // must not specify both. If the 'idlocation' attribute is present, the SCXML Processor
            // must generate an id automatically when the <invoke> element is evaluated and store it
            // in the location specified by 'idlocation'. (In the rest of this document, we will refer
            // to this identifier as the "invokeid", regardless of whether it is specified by the
            // author or generated by the platform). The automatically generated identifier must have
            // the form stateid.platformid, where stateid is the id of the state containing this
            // element and platformid is automatically generated. platformid must be unique within
            // the current session.
            invokeId = String.format("%s.%d", inv.parent_state_name, PLATFORM_ID_COUNTER.incrementAndGet());
        }

        Data src = datamodel.get_expression_alternative_value(inv.src, inv.src_expr);
        if (src == null || src instanceof Data.Error) {
            // Error -> Abort
            if (StaticOptions.trace_method) {
                this.tracer.exit_method("invoke");
            }
            return;
        }
        java.util.List<ParamPair> name_values = new ArrayList<>();
        for (String name : inv.name_list) {
            Data value = datamodel.get_by_location(name);
            if (value == null || value instanceof Data.Error) {
                // Error -> Abort
                if (StaticOptions.trace_method) {
                    this.tracer.exit_method("invoke");
                }
                return;
            }
            name_values.add(new ParamPair(name, value));
        }
        datamodel.evaluate_params(inv.params, name_values);

        if (StaticOptions.debug_option)
            Log.debug(
                    "Invoke: type '%s' invokeId '%s' src '%s' namelist '%s'",
                    type_name, invokeId, src, name_values
            );

        // We currently don't check if id and idLocation are exclusive set.
        if (!inv.external_id_location.isEmpty()) {
            // If "idlocation" is specified, we have to store the generated id to this location
            datamodel.set(
                    inv.external_id_location,
                    new Data.String(invokeId),
                    true
            );
        }

        ScxmlSession session;

        if (src.is_empty()) {
            var content = datamodel.evaluate_content(inv.content);
            if (content == null || content instanceof Data.Error) {
                Log.error("No content to execute");
                if (StaticOptions.trace_method) {
                    this.tracer.exit_method("invoke");
                }
                return;
            } else {
                try {
                    session = global
                            .executor
                            .execute_with_data_from_xml(
                                    content.as_script(),
                                    global.actions,
                                    name_values,
                                    global.session_id,
                                    invokeId,
                                    FinishMode.DISPOSE,
                                    this.tracer.trace_mode()
                            );
                } catch (IOException ioe) {
                    Log.error("Execute of '%s' failed: %s", content.toString(), ioe.getMessage());
                    if (StaticOptions.trace_method) {
                        this.tracer.exit_method("invoke");
                    }
                    return;
                }
            }
        } else {
            session = global.executor.execute_with_data(
                    src.as_script(),
                    global.actions,
                    name_values,
                    global.session_id, invokeId, this.tracer.trace_mode()
            );
        }

        session.state = state;
        session.invoke_doc_id = inv.doc_id;

        global.child_sessions.put(invokeId, session);
        if (StaticOptions.trace_method) {
            this.tracer.exit_method("invoke");
        }
    }

    protected void cancelInvoke(Datamodel datamodel, String invokeId, ScxmlSession session) {
        if (StaticOptions.trace_method) {
            this.tracer.enter_method("cancelInvoke");
        }
        datamodel.global().child_sessions.remove(invokeId);
        datamodel.send(
                ScxmlEventIOProcessor.SCXML_EVENT_PROCESSOR_SHORT_TYPE,
                new Data.String(String.format("%s%s", ScxmlEventIOProcessor.SCXML_TARGET_SESSION_ID_PREFIX, session.session_id.toString())),
                Event.new_simple(EVENT_CANCEL_SESSION)
        );
        if (StaticOptions.trace_method) {
            this.tracer.exit_method("cancelInvoke");
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * 5.9.1 Conditional Expressions<br>
     * Conditional expressions are used inside the 'cond' attribute of &lt;transition>, &lt;if> and &lt;elseif>.<br>
     * If a conditional expression cannot be evaluated as a boolean value ('true' or 'false') or if
     * its evaluation causes an error, the SCXML Processor must treat the expression as if it evaluated to
     * 'false' and must place the error 'error.execution' in the internal event queue.<br>
     * <br>
     * See {@link Datamodel#execute_condition(Data)}
     */
    protected boolean conditionMatch(Datamodel datamodel, Transition t) {
        if (t.cond == null || t.cond.is_empty()) {
            return true;
        } else {
            if (datamodel.execute_condition(t.cond)) {
                return true;
            } else {
                datamodel.internal_error_execution();
                return false;
            }
        }
    }

    public synchronized FsmTimer schedule(int delay_ms, @NotNull Runnable r) {
        if (timer == null)
            timer = new Timer();

        FsmTimer fsmTimer = new FsmTimer(r);
        timer.schedule(fsmTimer, delay_ms);
        return fsmTimer;
    }

}
