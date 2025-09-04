package com.bw.fsm.serializer;

import com.bw.fsm.*;
import com.bw.fsm.Log;
import com.bw.fsm.executableContent.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.List;

public class FsmReader implements DefaultProtocolDefinitions {

    protected @NotNull ProtocolReader reader;

    public FsmReader(@NotNull ProtocolReader reader) {
        this.reader = reader;
    }

    public boolean has_error() {
        return reader.has_error();
    }

    protected Map<Integer, State> states = new HashMap<>();
    protected Map<Integer, Transition> transitions = new HashMap<>();
    Map<Integer, ExecutableContentBlock> executableContent = new HashMap<>();

    public Fsm read() throws IOException {
        final long start = System.currentTimeMillis();
        var fsm = new Fsm();

        String version = reader.read_string();
        if (FSM_SERIALIZER_VERSION.equals(version)) {
            fsm.name = reader.read_string();
            fsm.datamodel = reader.read_string();
            fsm.binding = BindingType.from_ordinal(read_int());

            int states_len = read_int();
            Log.info("states: %d", states_len);
            for (int idx = 0; idx < states_len; ++idx) {
                var state = read_state();
                if ( state != null) {
                    fsm.statesNames.put(state.name, state);
                    states.put(state.id, state);
                }
            }
            int transitions_len = read_int();
            if (StaticOptions.debug_serializer)
                Log.debug("%d transitions to read", transitions_len);

            for (int idx = 0; idx < transitions_len; ++idx) {
               read_transition();
            }
            int executable_content_len = read_int();
            if (StaticOptions.debug_serializer)
                Log.debug("%d executable content blocks to read", executable_content_len);

            int ec_id = 0;
            for (int idx = 0; idx < executable_content_len; ++idx) {
                ++ec_id;
                ExecutableContentBlock block = executableContent.computeIfAbsent(ec_id,
                        k -> new ExecutableContentBlock(Collections.emptyList(), ""));
                int content_len = read_int();
                for (int idx2 = 0; idx2 < content_len; ++idx2) {
                    block.content.add(read_executable_content());
                }
            }
            fsm.pseudo_root = read_or_create_state_by_id();
            fsm.script = read_executable_content_id();

            long end = System.currentTimeMillis();
            Log.info("'%s' (RFSM) loaded in %dms", fsm.name, end - start);

            return fsm;
        } else if (reader.has_error()) {
            Log.error("Can't read");
        } else {
            Log.error("Version mismatch: '%s' is not '%s' as expected", version, FSM_SERIALIZER_VERSION);
        }
        return fsm;
    }

    public void close() {
        reader.close();
    }

    public @Nullable State read_or_create_state_by_id() throws IOException {
        int id = read_int();

        if (id == 0) {
            return null;
        }
        State s = states.get(id);
        if (s == null) {
            s = new State(null);
            s.id = id;
            states.put(id, s);
        }
        return s;
    }

    public Transition read_or_create_transition_by_id() throws IOException {
        int id = read_int();
        Transition t = transitions.get(id);
        if (t == null) {
            t = new Transition();
            t.id = id;
            // throw new IOException("Protocol Error: Unknown Transition id " + id);
            transitions.put(id, t);
        }
        return t;

    }

    public ExecutableContentBlock read_executable_content_id() throws IOException {
        int id = read_int();
        if (id == 0)
            return null;
        ExecutableContentBlock e = executableContent
                .computeIfAbsent(id, k -> new ExecutableContentBlock(Collections.emptyList(), ""));
        return e;
    }

    public int read_int() throws IOException {
        long l = reader.read_long();
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
            throw new IOException("Protocol Error: Integer Value to large ");
        return (int) l;
    }

    public void read_data_map(Map<String, Data> map) throws IOException {
        map.clear();
        int len = read_int();
        for (int i = 0; i < len; ++i) {
            String key = reader.read_string();
            map.put(key, reader.read_data());
        }
    }

    public CommonContent read_common_content() throws IOException {
        Data content = reader.read_data();
        String content_expr = reader.read_option_string();
        return new CommonContent(content, content_expr);
    }

    public void read_parameter(Parameter value) throws IOException {
        value.name = reader.read_string();
        value.expr = reader.read_string();
        value.location = reader.read_string();
    }

    public void read_done_data(DoneData value) throws IOException {
        if (reader.read_boolean()) {
            value.content = read_common_content();
        } else {
            value.content = null;
        }
        value.params = read_parameters();
    }

    public @Nullable List<Parameter> read_parameters() throws IOException {
        int param_len = read_int();
        if (param_len == 0) {
            return null;
        } else {
            List<Parameter> pvec = new ArrayList<>(param_len);
            for (int i = 0; i < param_len; ++i) {
                var param = new Parameter();
                read_parameter(param);
                pvec.add(param);
            }
            return pvec;
        }
    }

    public List<String> read_string_list() throws IOException {
        int len = read_int();
        List<String> pv = new ArrayList<>(len);
        for (int i = 0; i < len; ++i) {
            pv.add(reader.read_string());
        }
        return pv;
    }

    public void read_invoke(Invoke invoke) throws IOException {
        invoke.invoke_id = reader.read_string();
        if (invoke.invoke_id.isEmpty()) {
            invoke.parent_state_name = reader.read_string();
        }
        invoke.doc_id = read_int();
        invoke.src_expr = reader.read_data();
        invoke.src = reader.read_data();
        invoke.type_expr = reader.read_data();
        invoke.type_name = reader.read_data();
        invoke.external_id_location = reader.read_string();
        invoke.autoforward = reader.read_boolean();
        invoke.finalize = read_executable_content_id();

        if (reader.read_boolean()) {
            invoke.content = read_common_content();
        } else {
            invoke.content = null;
        }
        invoke.params = read_parameters();
        invoke.name_list.clear();
        invoke.name_list.addAll(read_string_list());
    }

    public Transition read_transition() throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug(">>Transition");

        var transition = read_or_create_transition_by_id();
        transition.doc_id = read_int();
        transition.source = read_or_create_state_by_id();

        int target_len = read_int();
        for (int idx = 0; idx < target_len; ++idx) {
            transition.target.add(read_or_create_state_by_id());
        }

        int events_len = read_int();
        if (events_len == 0) {
            transition.events = Collections.emptyList();
        } else {
            transition.events = new ArrayList<>(events_len);
            for (int idx = 0; idx < events_len; ++idx) {
                transition.events.add(reader.read_string());
            }
        }

        int flags = read_int();

        transition.transition_type = TransitionType.from_ordinal(flags & 1);
        transition.wildcard = (flags & 2) != 0;

        transition.cond = ((flags & 4) != 0) ? reader.read_data() : Data.Null.NULL;
        transition.content = ((flags & 8) != 0) ? read_executable_content_id() : null;

        if (StaticOptions.debug_serializer)
            Log.debug("<<Transition");

        return transition;
    }


    public @Nullable State read_state() throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug(">>State");

        State state = read_or_create_state_by_id();
        if ( state == null)
            return null;
        state.doc_id = read_int();
        state.name = reader.read_string();

        int flags = read_int();

        state.history_type = HistoryType.from_ordinal((flags & FSM_PROTOCOL_FLAG_HISTORY_TYPE_MASK));
        state.is_parallel = (flags & FSM_PROTOCOL_FLAG_IS_PARALLEL) != 0;
        state.is_final = (flags & FSM_PROTOCOL_FLAG_IS_FINAL) != 0;

        if ((flags & FSM_PROTOCOL_FLAG_STATES) != 0) {
            state.initial = read_or_create_transition_by_id();
            int states_len = read_int();
            for (int si = 0; si < states_len; ++si) {
                state.states.add(read_or_create_state_by_id());
            }
        }

        if ((flags & FSM_PROTOCOL_FLAG_ON_ENTRY) != 0) {
            int len = read_int();
            for (int si = 0; si < len; ++si) {
                state.onentry.add(read_executable_content_id());
            }
        }
        if ((flags & FSM_PROTOCOL_FLAG_ON_EXIT) != 0) {
            int len = read_int();
            for (int si = 0; si < len; ++si) {
                state.onexit.add(read_executable_content_id());
            }
        }

        int transition_len = read_int();
        for (int si = 0; si < transition_len; ++si) {
            state.transitions.push(read_or_create_transition_by_id());
        }

        if ((flags & FSM_PROTOCOL_FLAG_INVOKE) != 0) {
            int len = read_int();
            for (int si = 0; si < len; ++si) {
                Invoke invoke = new Invoke();
                read_invoke(invoke);
                state.invoke.push(invoke);
            }
        }

        if ((flags & FSM_PROTOCOL_FLAG_HISTORY) != 0) {
            int len = read_int();
            for (int si = 0; si < len; ++si) {
                state.history.push(read_or_create_state_by_id());
            }
        }

        if ((flags & FSM_PROTOCOL_FLAG_DATA) != 0) {
            read_data_map(state.data);
        }

        state.parent = read_or_create_state_by_id();

        if ((flags & FSM_PROTOCOL_FLAG_DONE_DATA) != 0) {
            state.donedata = new DoneData();
            read_done_data(state.donedata);
        } else {
            state.donedata = null;
        }

        if (StaticOptions.debug_serializer)
            Log.debug("<<State");

        return state;
    }

    public ExecutableContent read_executable_content() throws IOException {

        if (StaticOptions.debug_serializer)
            Log.debug(">>ExecutableContent");

        int ec_type = read_int();

        var ec = switch (ec_type) {
            case ExecutableContent.TYPE_IF -> read_executable_content_if();
            case ExecutableContent.TYPE_EXPRESSION -> read_executable_content_expression();
            case ExecutableContent.TYPE_LOG -> read_executable_content_log();
            case ExecutableContent.TYPE_FOREACH -> read_executable_content_for_each();
            case ExecutableContent.TYPE_SEND -> read_executable_content_send();
            case ExecutableContent.TYPE_RAISE -> read_executable_content_raise();
            case ExecutableContent.TYPE_CANCEL -> read_executable_content_cancel();
            case ExecutableContent.TYPE_ASSIGN -> read_executable_content_assign();
            default -> {
                Log.panic("Unknown Executable Content: %s", ec_type);
                yield null;
            }
        };
        if (StaticOptions.debug_serializer)
            Log.debug("<<ExecutableContent");

        return ec;
    }

    public ExecutableContent read_executable_content_if() throws IOException {
        var condition = reader.read_data();
        var ec = new If(condition);

        ec.content = read_executable_content_id();
        ec.else_content = read_executable_content_id();
        return ec;
    }

    public ExecutableContent read_executable_content_expression() throws IOException {
        Expression ec = new Expression();
        ec.content = reader.read_data();
        return ec;
    }

    public ExecutableContent read_executable_content_log() throws IOException {
        String label = reader.read_option_string();
        Data expression = reader.read_data();
        return new com.bw.fsm.executableContent.Log(label, expression);
    }

    public ExecutableContent read_executable_content_for_each() throws IOException {
        ForEach ec = new ForEach();
        ec.content = read_executable_content_id();
        ec.index = reader.read_string();
        ec.array = reader.read_data();
        ec.item = reader.read_string();
        return ec;
    }

    public ExecutableContent read_executable_content_send() throws IOException {
        var ec = new SendParameters();

        ec.name = reader.read_string();
        ec.target = reader.read_data();
        ec.target_expr = reader.read_data();

        boolean content_flag = reader.read_boolean();
        if (content_flag) {
            ec.content = read_common_content();
        }
        ec.name_list.clear();
        ec.name_list.addAll(read_string_list());
        ec.name_location = reader.read_string();
        ec.params = read_parameters();

        ec.event = reader.read_data();
        ec.event_expr = reader.read_data();

        ec.type_value = reader.read_data();
        ec.type_expr = reader.read_data();

        ec.delay_ms = read_int();
        ec.delay_expr = reader.read_data();

        return ec;
    }

    public ExecutableContent read_executable_content_raise() throws IOException {
        Raise ec = new Raise();
        ec.event = reader.read_string();
        return ec;
    }

    public ExecutableContent read_executable_content_cancel() throws IOException {
        Cancel ec = new Cancel();
        ec.send_id = reader.read_string();
        ec.send_id_expr = reader.read_data();
        return ec;
    }

    public ExecutableContent read_executable_content_assign() throws IOException {
        Assign ec = new Assign();
        ec.expr = reader.read_data();
        ec.location = reader.read_data();
        return ec;
    }
}
    
