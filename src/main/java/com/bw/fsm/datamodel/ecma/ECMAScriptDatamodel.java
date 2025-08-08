package com.bw.fsm.datamodel.ecma;

import com.bw.fsm.*;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.executable_content.ExecutableContentTracer;

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

    public ECMAScriptDatamodel(GlobalData global_data) {
        this.global_data = global_data;
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

    }

    @Override
    public void initialize_read_only(String name, Data value) {

    }

    @Override
    public void set(String name, Data data, boolean allow_undefined) {

    }

    @Override
    public void set_event(Event event) {

    }

    @Override
    public boolean assign(Data left_expr, Data right_expr) {
        return false;
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
        return false;
    }

    @Override
    public boolean executeContent(Fsm fsm, ExecutableContent content) {
        if (this.tracer != null) {
            content.trace(this.tracer, fsm);
        }
        return content.execute(this, fsm);
    }
}
