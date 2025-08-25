package com.bw.fsm;

import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.event_io_processor.EventIOProcessor;
import com.bw.fsm.event_io_processor.ScxmlEventIOProcessor;
import com.bw.fsm.serializer.DefaultProtocolReader;
import com.bw.fsm.serializer.FsmReader;
import com.bw.fsm.tracer.TraceMode;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class FsmExecutor {

    public ExecutorState state = new ExecutorState();

    public void add_processor(EventIOProcessor processor) {
        this.state.processors.add(processor);
    }

    public FsmExecutor(boolean withIoProcessors) {
        this.add_processor(new ScxmlEventIOProcessor());
        if (withIoProcessors) {
            /* TODO
            var w = new BasicHTTPEventIOProcessor(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), "localhost", 5555, e.state )
            this.add_processor(w);
            w = Box::new( ThriftEventIOProcessor::new(e.state.clone()) );
            this.add_processor(w);
           */
        }
    }

    private IncludePaths include_paths = new IncludePaths();


    public void set_global_options_from_arguments(Map<String, String> named_arguments) {
        // Currently only Datamodel options are relevant. Ignore all other stuff.
        for (var entry : named_arguments.entrySet()) {
            if (entry.getKey().startsWith(Datamodel.DATAMODEL_OPTION_PREFIX)) {
                this.state
                        .datamodel_options
                        .put(entry.getKey().substring(Datamodel.DATAMODEL_OPTION_PREFIX.length()),
                                entry.getValue());
            }
        }
    }

    public void set_include_paths(IncludePaths include_path) {
        this.include_paths.add(include_path);
    }

    /**
     * Shutdown of all FSMs and IO-Processors.
     */
    public void shutdown() {
        final var processors = state.processors;
        while (!processors.isEmpty()) {
            var pp = processors.remove(processors.size() - 1);
            if (pp != null) {
                pp.shutdown();
            }
        }
    }

    /**
     * Loads and starts the specified FSM.
     */
    public ScxmlSession execute(
            String uri,
            ActionWrapper actions,
            TraceMode trace
    ) {
        return this.execute_with_data(
                uri,
                actions,
                Collections.emptyList(),
                null,
                "",
                trace
        );
    }

    /**
     * Loads and starts the specified FSM with some data set.
     * <br> Normally used if a child-FSM is started from a parent FSM.
     *
     * @param parent The session if of the parent session or null.
     */
    public ScxmlSession execute_with_data(
            String url,
            ActionWrapper actions,
            java.util.List<ParamPair> data,
            Integer parent,
            String invoke_id,
            TraceMode trace
    ) {
        String extension = IOTool.getFileExtension(url);

        Fsm fsm = null;

        // Use reader to parse the scxml file:
        if ("scxml".equalsIgnoreCase(extension) || "xml".equalsIgnoreCase(extension)) {
            if (StaticOptions.debug)
                Log.debug("Loading FSM from XML %s", url);
            ScxmlReader sr = new ScxmlReader().withIncludePaths(this.include_paths);
            try {
                fsm = sr.parse_from_url(new URL(url));
            } catch (Exception e) {
                Log.error("Failed to parse %s", url);
                throw new RuntimeException(e);
            }
        } else if ("rfsm".equalsIgnoreCase(extension)) {
            if (StaticOptions.debug)
                Log.debug("Loading FSM from binary %s", url);
            FsmReader<DefaultProtocolReader<?>> reader = new FsmReader<>();
            // @TODO
            // sm = reader.read();
        }

        if (fsm != null) {
            fsm.file = url;

            fsm.tracer.enable_trace(trace);
            fsm.caller_invoke_id = invoke_id;
            fsm.parent_session_id = parent;
            return fsm.start_fsm_with_data(actions, this, data);
        } else {
            return null;
        }
    }

    /// Loads and starts the specified FSM with some data set.\
    /// Normally used if a child-FSM is started from a parent FSM, in this case via inline content.
    public ScxmlSession execute_with_data_from_xml(
            String xml,
            ActionWrapper actions,
            java.util.List<ParamPair> data,
            Integer parent,
            String invoke_id,
            TraceMode trace
    ) throws IOException {
        if (StaticOptions.debug)
            Log.debug("Loading FSM from XML");

        // Use reader to parse the XML:
        ScxmlReader reader = new ScxmlReader().withIncludePaths(this.include_paths);
        Fsm fsm = reader.parse_from_xml(xml);
        fsm.tracer.enable_trace(trace);
        fsm.caller_invoke_id = invoke_id;
        fsm.parent_session_id = parent;
        var session = fsm.start_fsm_with_data(
                actions,
                this,
                data
        );
        return session;
    }

    /**
     * Called by FSM after session ends and FinishMode.DISPOSE.
     */
    public void remove_session(Integer session_id) {
        this.state.sessions.remove(session_id);
    }

    /**
     * Sends some event to a session.
     */
    public void send_to_session(Integer session_id, Event event) {
        ScxmlSession session = this.state.sessions.get(session_id);
        if (session == null) {
            Log.panic("TODO: Handling of unknown session");
        } else {
            session.sender.enqueue(event);
        }
    }

}
