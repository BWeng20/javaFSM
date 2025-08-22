package com.bw.fsm.datamodel.ecma;

import com.bw.fsm.*;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.datamodel.ScriptException;
import com.bw.fsm.event_io_processor.EventIOProcessor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class ECMAScriptDatamodel extends Datamodel {

    public final static String ECMA_SCRIPT = "ECMAScript";
    public final static String ECMA_SCRIPT_LC = "ecmascript";

    public static void register() {
        DatamodelFactory.register_datamodel(ECMA_SCRIPT_LC, new ECMAScriptDatamodelFactory());
    }

    public final static String ECMA_OPTION_INFIX = "ecma:";
    public final static String ECMA_OPTION_STRICT_POSTFIX = "strict";

    public final static String ECMA_STRICT_OPTION = "datamodel:ecma:strict";

    public final static Arguments.Option ECMA_STRICT_ARGUMENT = new Arguments.Option(ECMA_STRICT_OPTION);


    public final GlobalData global_data;
    public boolean strict_mode;

    protected Value bindings;
    protected Context context;

    public ECMAScriptDatamodel(GlobalData global_data) {
        this.global_data = global_data;

        context = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", StaticOptions.debug_option ? "true" : "false")
                .option("js.strict", "true")
                .allowHostAccess(HostAccess.ALL)
                .out(Log.getPrintStream())
                .err(Log.getPrintStream())
                .build();
        bindings = context.getBindings("js");
    }

    @Override
    public ECMAScriptProducer createScriptProducer() {
        return new ECMAScriptProducer();
    }

    @Override
    public GlobalData global() {
        return global_data;
    }

    @Override
    public String get_name() {
        return ECMA_SCRIPT;
    }

    public class Helper {

        @HostAccess.Export
        public boolean in(String name) {
            for (State s : global().configuration.data) {
                if (s.name.equals(name))
                    return true;
            }
            return false;
        }

        @HostAccess.Export
        public Object action(String name, Object[] arguments) {
            return null;
        }

        @HostAccess.Export
        public void log(Object message) {
            ECMAScriptDatamodel.this.log(String.valueOf(message));
        }

    }

    @Override
    public void add_functions(Fsm fsm) {
        final GlobalData global = global();
        StringBuilder functions = new StringBuilder(300);
        for (String name : global.actions.actions.keySet()) {
            functions.append(
                    String.format(
                            "function %s(){{ return __helper.action('%s', Array.from(arguments)); }}\n",
                            name, name
                    )
            );
        }
        // Implement "In" function.
        functions.append("function In(state){ return __helper.in(state);}\n");
        // Implement "log" function.
        functions.append("function log(msg){ __helper.log(msg);}\n");

        bindings.putMember("__helper", new Helper());
        try {
            evalSource(functions.toString());
        } catch (Exception e) {
            Log.exception("Failed to add functions", e);
        }
    }

    protected @NotNull Data evalSource(String sourceText) throws ScriptException {
        try {
            if (StaticOptions.trace_script)
                global().tracer.trace(String.format("JS: %s", sourceText));
            Source source = Source.newBuilder("js", sourceText, null).build();
            return js_to_data_value(context.eval(source));
        } catch (Exception e) {
            throw new ScriptException(sourceText, e);
        }

    }


    protected @NotNull Data evalSourceNoThrow(String sourceText) {
        try {
            return evalSource(sourceText);
        } catch (Exception e) {
            Data d = new Data.Error(String.format("Eval of '%s' failed: %s", sourceText, e.getMessage()));
            Log.error("%s", d.toString());
            return d;
        }
    }

    @Override
    public void set_ioprocessors() {
        final GlobalData gd = this.global();
        int session_id = gd.session_id;

        // Create I/O-Processor Objects.
        ECMAScriptProducer script = createScriptProducer();
        script.startMap();
        for (var entry : gd.io_processors.entrySet()) {
            script.startMember(entry.getKey());
            script.startMap();
            script.addStringMember("location", entry.getValue().get_location(session_id));
            script.endMap();
            script.endMember();
        }
        script.endMap();
        setReadOnly(EventIOProcessor.SYS_IO_PROCESSORS, script);
    }

    @Override
    public void set_from_state_data(Map<String, Data> data, boolean set_data) {
        ECMAScriptProducer script = createScriptProducer();
        for (var entry : data.entrySet()) {
            try {
                if (set_data) {
                    script.addToken("var ");
                    script.addToken(entry.getKey());
                    script.addToken(" = ");
                    entry.getValue().as_script(script);
                    script.addToken(";\n");
                    evalSource(script.finish());

                } else {
                    evalSource("var " + entry.getKey() + "= undefined;");
                }
            } catch (Exception se) {
                script.finish();

                Log.error("Error on Initialize '%s': %s", entry.getKey(), se.getMessage());
                Log.exception("Trace", se);
                // W3C says:
                // If the value specified for a <data> element (by 'src', children, or
                // the environment) is not a legal data value, the SCXML Processor MUST
                // raise place error.execution in the internal event queue and MUST
                // create an empty data element in the data model with the specified id.
                try {
                    evalSource("var " + entry.getKey() + "= undefined;");
                } catch (Exception seInner) {
                    Log.error("Error on error handling for setting '%s': %s", entry.getKey(), seInner.getMessage());
                }
                this.internal_error_execution();
            }
        }
    }

    @Override
    public void initialize_read_only(String name, Data value) {
        try {
            ECMAScriptProducer script = new ECMAScriptProducer(true);
            script.addToken("const ");
            script.addToken(name);
            script.addToken("=");
            value.as_script(script);
            script.addToken(";");
            evalSource(script.finish());
        } catch (Exception se) {
            Log.exception("failed to set read only member '" + name + "'", se);
        }

    }

    @Override
    public void set(String name, Data data, boolean allow_undefined) {
        ECMAScriptProducer script = new ECMAScriptProducer();
        if (allow_undefined) script.addToken("var ");
        script.addToken(name);
        script.addToken("=");
        data.as_script(script);
        script.addToken(";");
        evalSourceNoThrow(script.finish());
    }

    @Override
    public void set_event(Event event) {

        ECMAScriptProducer eventScript = new ECMAScriptProducer(true);

        eventScript.startMap();
        eventScript.addStringMember(EVENT_VARIABLE_FIELD_NAME, event.name);
        eventScript.addStringMember(EVENT_VARIABLE_FIELD_TYPE, event.etype.name());
        eventScript.addStringMember(EVENT_VARIABLE_FIELD_ORIGIN, event.origin);
        eventScript.addStringMember(EVENT_VARIABLE_FIELD_ORIGIN_TYPE, event.origin_type);
        eventScript.addStringMember(EVENT_VARIABLE_FIELD_INVOKE_ID, event.invoke_id);
        eventScript.addStringMember(EVENT_VARIABLE_FIELD_SEND_ID, event.sendid);

        if (event.param_values == null) {
            Data r = (event.content == null || event.content instanceof Data.Error) ? Data.None.NONE : event.content;
            eventScript.addDataMember(EVENT_VARIABLE_FIELD_DATA, r);
        } else {
            eventScript.startMember(EVENT_VARIABLE_FIELD_DATA);
            eventScript.startMap();
            for (var pair : event.param_values) {
                eventScript.addDataMember(pair.name, pair.value);
            }
            eventScript.endMap();
            eventScript.endMember();
        }
        eventScript.endMap();
        setReadOnly(EVENT_VARIABLE_NAME, eventScript);
    }

    protected void setReadOnly(String name, ECMAScriptProducer script) {

        StringBuilder sb = new StringBuilder(200);
        sb
                .append("Object.defineProperty(globalThis,")
                .append(script.asStringValue(name))
                .append(",{value:")
                .append(script.finish())
                .append(",writable:false,configurable:true,enumerable:true});");

        try {
            if (bindings.hasMember(name)) {
                bindings.removeMember(name);
            }
            String src = sb.toString();
            if (StaticOptions.trace_script)
                global().tracer.trace("JS: " + src);
            Source source = Source.newBuilder("js", src, null).build();
            context.eval(source);
        } catch (Exception e) {
            Log.exception("Failed to set " + name, e);
        }

    }

    @Override
    public boolean assign(Data left_expr, Data right_expr) {
        ECMAScriptProducer script = new ECMAScriptProducer();

        left_expr.as_script(script);
        String left = script.finish();
        right_expr.as_script(script);
        String right = script.finish();

        return this.assign_internal(
                left,
                right,
                false
        );
    }

    @Override
    public @NotNull Data get_by_location(String location) {
        Data r = evalSourceNoThrow(location);
        if (r instanceof Data.Error) {
            internal_error_execution();
        }
        return r;
    }

    @Override
    public void clear() {
        bindings.getMemberKeys().clear();
    }

    @Override
    public @NotNull Data execute(Data script) {
        Data res = evalData(script);
        if (StaticOptions.debug_option)
            Log.debug("Execute: %s => %s", script, res);
        return res;
    }

    @Override
    public boolean execute_for_each(Data array_expression, String item_name, String index, Supplier<Boolean> execute_body) {
        if (StaticOptions.debug_option)
            Log.debug("ForEach: array: %s", array_expression);
        Data r = evalData(array_expression);
        if (r instanceof Data.Error) {
            return false;
        } else if (r instanceof Data.Map obj) {
            // Iterate through all members
            int idx = 0;

            if (assign_internal(item_name, "null", true)) {
                for (var item_prop : obj.values.keySet()) {
                    Data item = obj.values.get(item_prop);
                    if (StaticOptions.debug_option)
                        Log.debug("ForEach: #%s %s=%s", idx, item_name, item);
                    var str = item.toString();
                    if (assign(new Data.Source(item_name), new Data.Source(str))) {
                        if (!index.isEmpty()) {
                            evalSourceNoThrow("var " + index + "=" + idx + ";");
                        }
                        if (!execute_body.get()) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    idx += 1;
                }
            }
        } else if (r instanceof Data.Array array) {
            // Iterate through all elements
            int idx = 0;

            if (assign_internal(item_name, "null", true)) {
                for (var item : array.values) {
                    if (StaticOptions.debug_option)
                        Log.debug("ForEach: #%s %s=%s", idx, item_name, item);
                    var str = item.toString();
                    if (assign(new Data.Source(item_name), new Data.Source(str))) {
                        if (!index.isEmpty()) {
                            evalSourceNoThrow("var " + index + "=" + idx + ";");
                        }
                        if (!execute_body.get()) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    idx += 1;
                }
            }
        } else {
            Log.error("Resulting value is not a supported collection.");
            internal_error_execution();
        }
        return true;
    }

    @Override
    public boolean execute_condition(Data condition) {
        try {
            ECMAScriptProducer script = new ECMAScriptProducer();
            condition.as_script(script);
            Data r = evalSource(script.finish());
            if (r instanceof Data.Boolean b) {
                return b.value;
            } else if (r.is_numeric()) {
                return r.as_number().doubleValue() != 0;
            } else if (r instanceof Data.Map || r instanceof Data.FsmDefinition || r instanceof Data.Array) {
                // All object are true
                return true;
            } else if (r instanceof Data.Error) {
                this.internal_error_execution();
                return false;
            } else
                return !r.is_empty();
        } catch (ScriptException e) {
            Log.exception(e.getMessage(), e);
            this.internal_error_execution();
            return false;
        }
    }

    @Override
    public boolean executeContent(Fsm fsm, ExecutableContent content) {
        return content.execute(this, fsm);
    }

    protected @NotNull Data js_to_data_value(Value o) {
        if (o == null) {
            return Data.Null.NULL;
        } else if (o.isNull()) {
            if ("undefined".equals(o.toString())) {
                return Data.None.NONE;
            } else {
                return Data.Null.NULL;
            }
        } else if (o.isString()) {
            return new Data.String(o.asString());
        } else if (o.isNumber()) {
            if (o.fitsInInt()) {
                return new Data.Integer(o.asInt());
            } else if (o.fitsInDouble()) {
                return new Data.Double(o.asDouble());
            } else {
                // TODO
                return new Data.Double(o.asDouble());
            }
        } else if (o.isBoolean()) {
            return new Data.Boolean(o.asBoolean());
        } else if (o.hasArrayElements()) {
            final int N = (int) o.getArraySize();
            List<Data> l = new ArrayList<>(N);
            for (int i = 0; i < N; ++i)
                l.add(js_to_data_value(o.getArrayElement(i)));
            return new Data.Array(l);
        } else if (o.hasHashEntries()) {
            final int N = (int) o.getHashSize();
            Map<String, Data> l = new HashMap<>(N);
            for (Value i = o.getHashEntriesIterator(); i.hasIteratorNextElement(); ) {
                Value item = i.getIteratorNextElement();
                l.put(item.getArrayElement(0).asString(), js_to_data_value(item.getArrayElement(1)));
            }
            return new Data.Map(l);
        } else if (o.hasMembers()) {
            Set<String> member = o.getMemberKeys();
            if (member.isEmpty())
                return new Data.String(o.toString());
            else {
                Map<String, Data> l = new HashMap<>(member.size());
                for (String k : member) {
                    Data d = js_to_data_value(o.getMember(k));
                    l.put(k, d);
                }
                return new Data.Map(l);
            }
        } else {
            return new Data.String(o.toString());
        }
    }

    protected boolean assign_internal(String left_expr, String right_expr, boolean allow_undefined) {
        String exp = String.format("%s%s=%s;", allow_undefined ? "var " : "", left_expr, right_expr);
        Object d = this.evalData(new Data.Source(exp));
        if (d instanceof Data.Error err) {
            // W3C says:\
            // If the location expression does not denote a valid location in the data model or
            // if the value specified (by 'expr' or children) is not a legal value for the
            // location specified, the SCXML Processor must place the error 'error.execution'
            // in the internal event queue.
            Log.error(
                    "Could not assign %s=%s, '%s'.",
                    left_expr, right_expr, err.toString());
            this.internal_error_execution();
            return false;
        }
        return true;
    }

    protected @NotNull Data evalData(Data source) {
        try {
            ECMAScriptProducer script = new ECMAScriptProducer();
            source.as_script(script);
            return evalSource(script.finish());
        } catch (Exception se) {
            Log.error("%s", se.getMessage());
            return new Data.Error(String.format("Eval of '%s' failed: %s", source, se.getMessage()));
        }
    }
}
