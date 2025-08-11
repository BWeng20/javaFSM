package com.bw.fsm.datamodel.ecma;

import com.bw.fsm.*;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.event_io_processor.EventIOProcessor;
import com.bw.fsm.executable_content.ExecutableContentTracer;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

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
    public ExecutableContentTracer tracer;
    public boolean strict_mode;

    protected Value bindings;
    protected Context context;
    protected Value valueUndefined;

    public ECMAScriptDatamodel(GlobalData global_data) {
        this.global_data = global_data;

        context = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", StaticOptions.debug_option ? "true" : "false")
                .option("js.strict", "true")
                .allowHostAccess(HostAccess.ALL)
                .build();
        bindings = context.getBindings("js");
        valueUndefined = context.eval("js", "undefined;");
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
            Log.exception( "Failed to add functions", e);
        }
    }

    protected Data evalSource(String sourceText) throws Exception {
        Source source = Source.newBuilder("js", sourceText, null).build();
        return js_to_data_value(context.eval(source));

    }

    @Override
    public void set_ioprocessors() {
        final GlobalData gd = this.global();
        int session_id = gd.session_id;

        // Create I/O-Processor Objects.
        Map<String, Object> processors = new HashMap<>();
        for (var entry : gd.io_processors.entrySet()) {
            Map<String, String> processor = new HashMap<>();
            processor.put("location", entry.getValue().get_location(session_id));
            processors.put(entry.getKey(), processor);
        }
        bindings.putMember(EventIOProcessor.SYS_IO_PROCESSORS, processors);
    }

    @Override
    public void set_from_state_data(Map<String, Data> data, boolean set_data) {
        for (var entry : data.entrySet()) {
            if (set_data) {
                Data value = entry.getValue();
                if (value instanceof Data.Source src) {
                    if (!src.is_empty()) {
                        try {
                            // The data from state-data needs to be evaluated
                            evalSource("var " + entry.getKey() + "=" + src.as_script());
                        } catch (Exception se) {
                            Log.error("Error on Initialize '%s': %s", entry.getKey(), se.getMessage());
                            Log.exception( "Trace", se);
                            // W3C says:
                            // If the value specified for a <data> element (by 'src', children, or
                            // the environment) is not a legal data value, the SCXML Processor MUST
                            // raise place error.execution in the internal event queue and MUST
                            // create an empty data element in the data model with the specified id.
                            try {
                                evalSource(entry.getKey() + "= undefined;");
                            } catch (Exception seInner) {
                                Log.error("Error on error handling for setting '%s': %s", entry.getKey(), seInner.getMessage());
                            }
                            this.internal_error_execution();
                        }
                    }
                } else {
                    // let ds = self.data_value_to_js(value.lock().unwrap().deref());
                    //  self.set_js_property(name.as_str(), ds);
                }
            } else {
                // self.set_js_property(name.as_str(), JsValue::Undefined);
            }
        }
    }

    @Override
    public void initialize_read_only(String name, Data value) {
        try {
            evalSource("const " + name + "= " + value.as_script() + ";");
        } catch (Exception se) {
            Log.exception( "failed to set read only member '"+name+"'", se);
        }

    }

    @Override
    public void set(String name, Data data, boolean allow_undefined) {
        bindings.putMember(name, eval(data));
    }

    @Override
    public void set_event(Event event) {
        Map<String, Object> eventMember = new HashMap<>();
        Value data_value;
        if (event.param_values == null) {
            if (event.content == null)
                data_value = null;
            else
                data_value = null; // self.data_value_to_js(c),
        } else {
            java.util.Map<String, Object> data = new HashMap<>(event.param_values.size());

            for (var pair : event.param_values) {
                data.put(pair.name, data_value_to_js(pair.value));
            }
            data_value = null;
        }

        eventMember.put(EVENT_VARIABLE_FIELD_NAME, event.name);
        eventMember.put(EVENT_VARIABLE_FIELD_TYPE, event.etype.name());
        eventMember.put(EVENT_VARIABLE_FIELD_SEND_ID, event.sendid);
        eventMember.put(EVENT_VARIABLE_FIELD_ORIGIN, event.origin);
        eventMember.put(EVENT_VARIABLE_FIELD_ORIGIN_TYPE, event.origin_type);
        eventMember.put(EVENT_VARIABLE_FIELD_INVOKE_ID, event.invoke_id);
        eventMember.put(EVENT_VARIABLE_FIELD_DATA, data_value);

        bindings.putMember(EVENT_VARIABLE_NAME, Value.asValue(eventMember));

        try {
            evalSource("Object.freeze(" + EVENT_VARIABLE_NAME + ");");
            evalSource(EVENT_VARIABLE_NAME + ".name = 1;");
            evalSource("log( 'Event '+" + EVENT_VARIABLE_NAME + ".name);");
        } catch (Exception e) {
            Log.exception( "Failed to set event", e);
        }
    }

    @Override
    public boolean assign(Data left_expr, Data right_expr) {
        return this.assign_internal(
                left_expr.as_script(),
                right_expr.as_script(),
                false
        );
    }

    @Override
    public Data get_by_location(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        bindings.getMemberKeys().clear();
    }

    @Override
    public Data execute(Data script) {
        Data res = eval(script);
        if (StaticOptions.debug_option)
            Log.debug("Execute: %s => %s", script, res);
        return res;
    }

    @Override
    public boolean execute_for_each(Data array_expression, String item, String index, Supplier<Boolean> execute_body) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean execute_condition(Data script) {
        try {
            Data r = evalSource(script.as_script());
            if (r instanceof Data.Boolean b)
                return b.value;
            else if (r instanceof Data.String s)
                return Boolean.parseBoolean(s.value);
            else if (r.is_numeric())
                return r.as_number().doubleValue() != 0;
            return false;
        } catch (Exception se) {
            // TODO
            Log.exception( "Failed to execute condition "+script.as_script(), se);
        }
        return false;
    }

    @Override
    public boolean executeContent(Fsm fsm, ExecutableContent content) {
        if (this.tracer != null) {
            content.trace(this.tracer, fsm);
        }
        return content.execute(this, fsm);
    }

    protected Value data_value_to_js(Data o) {
        if (o == null || o == Data.Null.NULL)
            return Value.asValue(null);
        if (o instanceof Data.Boolean b) {
            return Value.asValue(b.value);
        } else if (o instanceof Data.String s) {
            return Value.asValue(s.value);
        } else if (o instanceof Data.None s) {
            return valueUndefined;
        } else if (o instanceof Data.Map m)
            return Value.asValue(m.values);
        else if (o instanceof Data.Array a)
            return Value.asValue(a.values.toArray());
        else if (o.is_numeric())
            return Value.asValue(o.as_number());
        else
            throw new IllegalArgumentException("Unsupported type " + o.getClass());

    }


    protected Data js_to_data_value(Value o) {
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
        String exp = String.format("%s=%s", left_expr, right_expr);
        if (allow_undefined && this.strict_mode) {
            // this.context.strict(false);
        }
        Object d = this.eval(new Data.Source(exp));
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
        if (allow_undefined && this.strict_mode) {
            // this.context.strict(true);
        }
        return true;
    }

    protected Data eval(Data source) {
        try {
            return evalSource(source.as_script());
        } catch (Exception se) {
            return new Data.Error(se.getMessage());
        }
    }
}
