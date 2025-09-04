package com.bw.fsm.serializer;

import com.bw.fsm.*;
import com.bw.fsm.Log;
import com.bw.fsm.executableContent.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes FSM definitions as binary file.
 */
public class FsmWriter implements DefaultProtocolDefinitions {

    protected @NotNull ProtocolWriter writer;

    /**
     * Initialize a new instance,
     *
     * @param writer The writer to use.
     */
    public FsmWriter(@NotNull ProtocolWriter writer) {
        this.writer = writer;
    }

    protected final List<Transition> transitions = new ArrayList<>();
    protected final List<ExecutableContentBlock> executableContentBlocks = new ArrayList<>();

    /**
     * Writes a FSM into the ProtocolWriter.
     *
     * @param fsm The FSM to write.
     * @throws IOException In case some IO operation failed.
     */
    public void write(Fsm fsm) throws IOException {
        writer.write_str(FSM_SERIALIZER_VERSION);
        writer.write_str(fsm.name);
        writer.write_str(fsm.datamodel);
        write_int(fsm.binding.get_ordinal());

        write_size(fsm.statesNames.size());
        for (State state : fsm.statesNames.values()) {
            write_state(state);
        }

        write_size(transitions.size());
        for (Transition transition : transitions) {
            write_transition(transition);
        }
        if (StaticOptions.debug_serializer)
            Log.debug("%d transitions written", transitions.size());

        write_int(executableContentBlocks.size());
        for (ExecutableContentBlock ec : executableContentBlocks) {
            write_size(ec.content.size());
            for (var executable_content : ec.content) {
                write_executable_content(executable_content);
            }
        }
        if (StaticOptions.debug_serializer)
            Log.debug("%d executable content blocks written", executableContentBlocks.size());

        write_int(fsm.pseudo_root.id);
        write_executable_content_block_id(fsm.script);
    }

    protected void write_executable_content(ExecutableContent executable_content) throws IOException {
        var ec_type = executable_content.get_type();

        if (StaticOptions.debug_serializer)
            Log.debug(">>ExecutableContent " + ec_type);

        write_int(ec_type);

        switch (ec_type) {
            case ExecutableContent.TYPE_IF -> write_executable_content_if((If) (executable_content));
            case ExecutableContent.TYPE_EXPRESSION ->
                    write_executable_content_expression((Expression) executable_content);
            case ExecutableContent.TYPE_LOG ->
                    write_executable_content_log((com.bw.fsm.executableContent.Log) executable_content);
            case ExecutableContent.TYPE_FOREACH -> write_executable_content_for_each((ForEach) executable_content);
            case ExecutableContent.TYPE_SEND -> write_executable_content_send((SendParameters) executable_content);
            case ExecutableContent.TYPE_RAISE -> write_executable_content_raise((Raise) executable_content);
            case ExecutableContent.TYPE_CANCEL -> write_executable_content_cancel((Cancel) executable_content);
            case ExecutableContent.TYPE_ASSIGN -> write_executable_content_assign((Assign) executable_content);
            default -> Log.panic("Unknown Executable Content Type: %d", ec_type);
        }

        if (StaticOptions.debug_serializer)
            Log.debug("<<ExecutableContent");

    }

    protected void write_executable_content_if(If executable_content_if) throws IOException {
        writer.write_data(executable_content_if.condition);
        write_int(getExecutableContentId(executable_content_if.content));
        write_executable_content_block_id(executable_content_if.else_content);
    }

    protected void write_executable_content_expression(Expression executable_content_expression) throws IOException {
        writer.write_data(executable_content_expression.content);
    }

    protected void write_executable_content_log(com.bw.fsm.executableContent.Log executable_content_log) throws IOException {
        writer.write_option_string(executable_content_log.label);
        writer.write_data(executable_content_log.expression);
    }

    protected void write_executable_content_for_each(ForEach executable_content_for_each) throws IOException {
        write_executable_content_block_id(executable_content_for_each.content);
        writer.write_str(executable_content_for_each.index);
        writer.write_data(executable_content_for_each.array);
        writer.write_str(executable_content_for_each.item);
    }

    protected void write_executable_content_send(SendParameters executable_content_send) throws IOException {
        writer.write_str(executable_content_send.name);
        writer.write_data(executable_content_send.target);
        writer.write_data(executable_content_send.target_expr);

        if (executable_content_send.content != null) {
            writer.write_boolean(true);
            write_common_content(executable_content_send.content);
        } else {
            writer.write_boolean(false);
        }

        write_string_list(executable_content_send.name_list);
        writer.write_str(executable_content_send.name_location);
        write_parameters(executable_content_send.params);

        writer.write_data(executable_content_send.event);
        writer.write_data(executable_content_send.event_expr);

        writer.write_data(executable_content_send.type_value);
        writer.write_data(executable_content_send.type_expr);

        writer.write_long(executable_content_send.delay_ms);
        writer.write_data(executable_content_send.delay_expr);
    }

    protected void write_executable_content_raise(Raise executable_content_raise) throws IOException {
        writer.write_str(executable_content_raise.event);
    }

    protected void write_executable_content_cancel(Cancel executable_content_cancel) throws IOException {
        writer.write_str(executable_content_cancel.send_id);
        writer.write_data(executable_content_cancel.send_id_expr);
    }

    protected void write_executable_content_assign(Assign executable_content_assign) throws IOException {
        writer.write_data(executable_content_assign.expr);
        writer.write_data(executable_content_assign.location);
    }

    protected void write_int(int value) throws IOException {
        writer.write_long(value);
    }

    protected void write_transition(Transition transition) throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug(">>Transition #%s", transition.id);

        write_int(transition.id);
        write_int(transition.doc_id);
        write_int(transition.source.id);
        write_size(transition.target.size());
        for (State t : transition.target) {
            write_int(t.id);
        }
        write_size(transition.events.size());
        for (String e : transition.events) {
            writer.write_str(e);
        }
        write_int(
                transition.transition_type.get_ordinal() // 0 - 1
                        | (transition.wildcard ? 2 : 0)
                        | (transition.cond.is_empty() ? 0 : 4)
                        | ((transition.content != null) ? 8 : 0)
        );

        if (!transition.cond.is_empty()) {
            writer.write_data(transition.cond);
        }
        if (transition.content != null) {
            write_executable_content_block_id(transition.content);
        }

        if (StaticOptions.debug_serializer)
            Log.debug("<<Transition");
    }

    protected void write_data_map(Map<String, Data> value) throws IOException {
        write_size(value.size());
        for (var entry : value.entrySet()) {
            writer.write_str(entry.getKey());
            writer.write_data(entry.getValue());
        }
    }

    protected void write_state(State state) throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug(">>State %s", state.name);

        write_int(state.id);
        write_int(state.doc_id);
        writer.write_str(state.name);

        int flags = state.history_type.get_ordinal() // 0 - 2
                | (state.onentry.isEmpty() ? 0 : FSM_PROTOCOL_FLAG_ON_ENTRY)
                | (state.onexit.isEmpty() ? 0 : FSM_PROTOCOL_FLAG_ON_EXIT)
                | (state.states.isEmpty() ? 0 : FSM_PROTOCOL_FLAG_STATES)
                | (state.is_final ? FSM_PROTOCOL_FLAG_IS_FINAL : 0)
                | (state.is_parallel ? FSM_PROTOCOL_FLAG_IS_PARALLEL : 0)
                | (state.donedata != null ? FSM_PROTOCOL_FLAG_DONE_DATA : 0)
                | (state.invoke.size() > 0 ? FSM_PROTOCOL_FLAG_INVOKE : 0)
                | (state.data.isEmpty() ? 0 : FSM_PROTOCOL_FLAG_DATA)
                | (state.history.size() > 0 ? FSM_PROTOCOL_FLAG_HISTORY : 0);
        write_int(flags);

        if (!state.states.isEmpty()) {
            write_int(state.initial == null ? 0 : state.initial.id);
            write_size(state.states.size());
            for (State s : state.states) {
                write_int(s.id);
            }
        }

        if (!state.onentry.isEmpty()) {
            write_size(state.onentry.size());
            for (ExecutableContentBlock ec : state.onentry) {
                write_executable_content_block_id(ec);
            }
        }
        if (!state.onexit.isEmpty()) {
            write_size(state.onexit.size());
            for (ExecutableContentBlock ec : state.onentry)
                write_executable_content_block_id(ec);
        }

        write_size(state.transitions.size());
        for (Transition t : state.transitions.data) {
            transitions.add(t);
            write_int(t.id);
        }

        if (state.invoke.size() > 0) {
            write_size(state.invoke.size());
            for (var invoke : state.invoke.data) {
                write_invoke(invoke);
            }
        }

        if (state.history.size() > 0) {
            write_size(state.history.size());
            for (var history : state.history.data) {
                write_int(history.id);
            }
        }

        if (!state.data.isEmpty()) {
            write_data_map(state.data);
        }

        write_int(state.parent == null ? 0 : state.parent.id);

        if (state.donedata != null) {
            write_done_data(state.donedata);
        }

        if (StaticOptions.debug_serializer)
            Log.debug("<<State");
    }

    protected void write_done_data(@NotNull DoneData value) throws IOException {
        writer.write_boolean(value.content != null);
        if (value.content != null) {
            write_common_content(value.content);
        }
        write_parameters(value.params);
    }

    protected void write_invoke(Invoke invoke) throws IOException {
        writer.write_str(invoke.invoke_id);
        if (invoke.invoke_id.isEmpty()) {
            writer.write_str(invoke.parent_state_name);
        }
        write_int(invoke.doc_id);
        writer.write_data(invoke.src_expr);
        writer.write_data(invoke.src);
        writer.write_data(invoke.type_expr);
        writer.write_data(invoke.type_name);
        writer.write_str(invoke.external_id_location);
        writer.write_boolean(invoke.autoforward);
        write_executable_content_block_id(invoke.finalize);

        if (invoke.content != null) {
            writer.write_boolean(true);
            write_common_content(invoke.content);
        } else {
            writer.write_boolean(false);
        }

        write_parameters(invoke.params);
        write_string_list(invoke.name_list);

        // parent_state_name is not written, the reader needs to
        // restore it from the current state.
    }

    protected void write_common_content(CommonContent value) throws IOException {
        writer.write_data(value.content);
        writer.write_option_string(value.content_expr);
    }

    protected void write_parameters(List<Parameter> parameters) throws IOException {
        if (parameters != null) {
            write_size(parameters.size());
            for (var p : parameters) {
                write_parameter(p);
            }
        } else {
            write_size(0);
        }
    }

    protected void write_parameter(Parameter value) throws IOException {
        writer.write_str(value.name);
        writer.write_str(value.expr);
        writer.write_str(value.location);
    }

    protected void write_size(int size) throws IOException {
        writer.write_long(size);
    }

    protected void write_string_list(List<String> strings) throws IOException {
        write_size(strings.size());
        for (String s : strings) {
            writer.write_str(s);
        }
    }

    protected int getExecutableContentId(ExecutableContentBlock ec) {
        if (ec == null)
            return 0;
        else {
            int idx = executableContentBlocks.indexOf(ec);
            if (idx < 0) {
                idx = executableContentBlocks.size();
                executableContentBlocks.add(ec);
            }
            return idx + 1;
        }
    }

    protected void write_executable_content_block_id(ExecutableContentBlock ec) throws IOException {
        write_int(getExecutableContentId(ec));
    }


}
