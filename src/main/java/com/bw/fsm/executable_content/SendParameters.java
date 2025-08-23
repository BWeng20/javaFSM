package com.bw.fsm.executable_content;

import com.bw.fsm.*;
import com.bw.fsm.Log;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.event_io_processor.EventIOProcessor;
import com.bw.fsm.event_io_processor.ScxmlEventIOProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all parameters of a &lt;send> call.
 */
public class SendParameters implements ExecutableContent {
    /// SCXML \<send\> attribute 'idlocation'
    public @NotNull String name_location = "";
    /// SCXML \<send\> attribute 'id'.
    public @NotNull String name = "";
    /// In case the id is generated, the parent state of the send.
    public @NotNull String parent_state_name = "";
    /// SCXML \<send\> attribute 'event'.
    public @NotNull Data event = Data.None.NONE;
    /// SCXML \<send\> attribute 'eventexpr'.
    public Data event_expr = Data.None.NONE;
    /// SCXML \<send\> attribute 'target'.
    public Data target = Data.None.NONE;
    /// SCXML \<send\> attribute 'targetexpr'.
    public Data target_expr = Data.None.NONE;
    /// SCXML \<send\> attribute 'type'.
    public Data type_value = Data.None.NONE;
    /// SCXML \<send\> attribute 'typeexpr'.
    public Data type_expr = Data.None.NONE;
    /// SCXML \<send\> attribute 'delay' in milliseconds.
    public int delay_ms = 0;
    /// SCXML \<send\> attribute 'delayexpr'.
    public Data delay_expr = Data.None.NONE;
    /// SCXML \<send\> attribute 'namelist'. Must not be specified in conjunction with 'content'.
    public final List<String> name_list = new ArrayList<>();

    /// &lt;param> children
    public @Nullable List<Parameter> params;
    public @Nullable CommonContent content;

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        Data target = datamodel.get_expression_alternative_value(this.target, this.target_expr);
        if (target instanceof Data.Error) {
            // Error -> abort
            return false;
        }

        Data event_name = datamodel.get_expression_alternative_value(this.event, this.event_expr);
        if (event_name instanceof Data.Error) {
            // Error -> abort
            return false;
        }

        String send_id;

        if (this.name_location.isEmpty()) {
            send_id = this.name.isEmpty() ? null : this.name;
        } else {
            // W3c says:
            // If 'idlocation' is present, the SCXML Processor MUST generate an id when the parent
            // <send> element is evaluated and store it in this location.
            // note that the automatically generated id for <invoke> has a special format.
            // See 6.4.1 Attribute Details for details.
            // The SCXML processor MAY generate all other ids in any format, as long as they are unique.
            //
            // Implementation: we do it the same as for invoke

            send_id = String.format("%s.%s", this.parent_state_name, Fsm.PLATFORM_ID_COUNTER.incrementAndGet());

            datamodel.set(this.name_location, new Data.String(send_id), true);
        }

        var data_vec = new ArrayList<ParamPair>();

        Data content = null;

        // A conformant document MUST NOT specify "namelist" or <param> with <content>.
        if (this.content != null) {
            content = datamodel.evaluate_content(this.content);
        } else {
            datamodel.evaluate_params(this.params, data_vec);
            for (String name : this.name_list) {
                Data value = datamodel.get_by_location(name);
                if (value instanceof Data.Error) {
                    // Error -> abort
                    return false;
                }
                data_vec.add(new ParamPair(name, value));
            }
        }
        int delay_ms;
        if (!this.delay_expr.is_empty()) {
            Data delay = datamodel.execute(this.delay_expr);
            if (delay instanceof Data.Error) {
                // Error -> Abort
                return false;
            } else {
                delay_ms = ExecutableContent.parse_duration_to_milliseconds(delay.toString());
            }
        } else {
            delay_ms = this.delay_ms;
        }

        if (delay_ms < 0) {
            // Delay is invalid -> Abort
            Log.error("Send: delay %s is negative", this.delay_expr);
            datamodel.internal_error_execution_for_event(send_id, fsm.caller_invoke_id);
            return false;
        }

        if (delay_ms > 0 && ScxmlEventIOProcessor.SCXML_TARGET_INTERNAL.equals(target.toString())) {
            // Can't send via internal queue
            Log.error("Send: illegal delay for target %s", target);
            datamodel.internal_error_execution_for_event(send_id, fsm.caller_invoke_id);
            return false;
        }
        Data type_result = datamodel.get_expression_alternative_value(this.type_value, this.type_expr);

        Data type_val;
        if (type_result != null && !(type_result instanceof Data.Error)) {
            type_val = type_result;
        } else {
            Log.error("Failed to evaluate send type: %s", type_result);
            datamodel.internal_error_execution_for_event(send_id, fsm.caller_invoke_id);
            return false;
        }

        String type_val_string = type_val.is_empty() ? Datamodel.SCXML_EVENT_PROCESSOR : type_val.toString();

        Event event = new Event();
        event.name = event_name.toString();
        event.etype = EventType.external;
        event.sendid = send_id;
        event.origin = null;
        event.origin_type = null;
        event.invoke_id = fsm.caller_invoke_id;
        event.param_values = data_vec.isEmpty() ? null : data_vec;
        event.content = content;

        boolean result;
        if (delay_ms > 0) {
            EventIOProcessor iop = datamodel.get_io_processor(type_val_string);
            if (iop != null) {
                if (StaticOptions.debug_option)
                    Log.debug("schedule '%s' for %d", event, delay_ms);
                GlobalData global = datamodel.global();
                var tg = fsm.schedule(delay_ms, () -> {
                    if (send_id != null) {
                        global.delayed_send.remove(send_id);
                    }
                    iop.send(global, target.toString(), event);
                });
                if (tg != null) {
                    if (send_id != null) {
                        global.delayed_send.put(send_id, tg);
                    }
                }
                result = true;
            } else {
                Log.error("Unknown io-processor %s", type_val);
                result = false;
            }
            if (!result) {
                // W3C:  If the SCXML Processor does not support the type that is specified,
                // it must place the event error.execution on the internal event queue.
                datamodel.internal_error_execution_for_event(send_id, fsm.caller_invoke_id);
            }

        } else {
            if (StaticOptions.debug_option)
                Log.debug("send '%s' to '%s'", event, target);
            // "send" triggers error events already, no need to check the result here
            result = datamodel.send(type_val_string, target, event);
        }
        return result;
    }

    @Override
    public int get_type() {
        return TYPE_SEND;
    }

    @Override
    public Map<String, Object> get_trace() {
        HashMap<String, Object> r = new HashMap<>(25);
        r.put("name_location", this.name_location);
        r.put("name", this.name);
        r.put("parent_state_name", this.parent_state_name);
        r.put("event", this.event);
        r.put("event_expr", this.event_expr);
        r.put("target", this.target);
        r.put("target_expr", this.target_expr);
        r.put("type_value", this.type_value);
        r.put("type_expr", this.type_expr);
        r.put("delay_ms", this.delay_ms);
        r.put("delay_expr", this.delay_expr);
        r.put("name_list", this.name_list);
        r.put("params", this.params);
        r.put("content", this.content);
        return r;
    }

    public void push_param(Parameter param) {
        if (params == null)
            params = new ArrayList<>(1);
        params.add(param);
    }
}
