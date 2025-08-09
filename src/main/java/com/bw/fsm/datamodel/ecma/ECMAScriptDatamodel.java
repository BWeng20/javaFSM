package com.bw.fsm.datamodel.ecma;

import com.bw.fsm.*;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.executable_content.ExecutableContentTracer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
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

    protected ScriptEngine engine;

    public ECMAScriptDatamodel(GlobalData global_data) {
        this.global_data = global_data;
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("JavaScript");

    }

    @Override
    public GlobalData global() {
        return global_data;
    }

    @Override
    public String get_name() {
        return ECMA_SCRIPT;
    }

    @Override
    public void add_functions(Fsm fsm) {

    }

    @Override
    public void set_ioprocessors() {

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
                            Object rs = engine.eval(entry.getKey() + "=" + src.as_script());
                            if (rs instanceof Data.Error err) {
                                Log.error("Error on Initialize '%s': %s", entry.getKey(), err);
                                // W3C says:
                                // If the value specified for a <data> element (by 'src', children, or
                                // the environment) is not a legal data value, the SCXML Processor MUST
                                // raise place error.execution in the internal event queue and MUST
                                // create an empty data element in the data model with the specified id.
                                this.engine.eval(entry.getKey() + "= undefined;");
                            }
                        } catch (ScriptException se) {
                            se.printStackTrace();
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
            this.engine.eval(name + "= " + value.as_script() + ";");
        } catch (ScriptException se) {
            se.printStackTrace();
        }

    }

    @Override
    public void set(String name, Data data, boolean allow_undefined) {
        try {
            engine.eval(name + " = " + data.as_script());
        } catch (ScriptException e) {
            Log.error(e.getMessage());
        }

    }

    @Override
    public void set_event(Event event) {

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
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public Data execute(Data script) {
        return null;
    }

    @Override
    public boolean execute_for_each(Data array_expression, String item, String index, Supplier<Boolean> execute_body) {
        return false;
    }

    @Override
    public boolean execute_condition(Data script) {
        try {
            Object r = engine.eval(script.as_script());
            return (Boolean) r;
        } catch (ScriptException se) {
            se.printStackTrace();
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
        ;
        if (allow_undefined && this.strict_mode) {
            // this.context.strict(true);
        }
        return true;
    }

    protected Object eval(Data source) {
        try {
            Object r = engine.eval(source.as_script());
            return r;
        } catch (ScriptException se) {
            return new Data.Error(se.getMessage());
        }
    }
}
