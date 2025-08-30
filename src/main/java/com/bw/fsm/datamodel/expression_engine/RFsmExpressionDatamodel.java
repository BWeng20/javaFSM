package com.bw.fsm.datamodel.expression_engine;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.ScriptProducer;
import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.datamodel.JsonScriptProducer;
import com.bw.fsm.datamodel.expression_engine.action.*;
import com.bw.fsm.expression_engine.Expression;
import com.bw.fsm.expression_engine.ExpressionException;
import com.bw.fsm.expression_engine.ExpressionParser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class RFsmExpressionDatamodel extends Datamodel {

    public static final String RFSM_EXPRESSION_DATAMODEL = "RFSM-EXPRESSION";
    public static final String RFSM_EXPRESSION_DATAMODEL_LC = "rfsm-expression";

    public static void register() {
        DatamodelFactory.register_datamodel(RFSM_EXPRESSION_DATAMODEL_LC, new RFsmExpressionDatamodelFactory());
    }

    public RFsmExpressionDatamodel(GlobalData data) {
        this.global_data = data;
    }

    public GlobalData global_data;
    public HashMap<Integer, Expression> compilations = new HashMap<>();

    @Override
    public GlobalData global() {
        return global_data;
    }

    @Override
    public String get_name() {
        return RFSM_EXPRESSION_DATAMODEL;
    }

    protected void add_internal_functions_to_wrapper(ActionWrapper actions) {
        actions.add_action("indexOf", new IndexOf());
        actions.add_action("length", new Length());
        actions.add_action("isDefined", new IsDefined());
        actions.add_action("abs", new Abs());
        actions.add_action("toString", new ToString());
        actions.add_action("log", new Log());
    }

    @Override
    public void add_functions(Fsm fsm) {
        GlobalData gd = global();
        add_internal_functions_to_wrapper(gd.actions);
        gd.actions.add_action("In", new In(fsm));

    }

    @Override
    public boolean execute_condition(Data condition) {
        try {
            JsonScriptProducer scripter = new JsonScriptProducer();
            condition.as_script(scripter);
            Data r = ExpressionParser.execute(scripter.finish(), global());
            return switch (r.type) {
                case Boolean -> ((Data.Boolean) r).value;
                case Integer, Double -> r.as_number().doubleValue() != 0;
                default -> {
                    this.internal_error_execution();
                    yield false;
                }
            };
        } catch (ExpressionException e) {
            com.bw.fsm.Log.exception(e.getMessage(), e);
            this.internal_error_execution();
            return false;
        }
    }

    @Override
    public boolean executeContent(Fsm fsm, ExecutableContent content) {
        return content.execute(this, fsm);
    }

    @Override
    public @NotNull Data execute(Data script) {
        try {
            JsonScriptProducer scripter = new JsonScriptProducer();
            script.as_script(scripter);
            return ExpressionParser.execute(scripter.finish(), global());
        } catch (Exception se) {
            com.bw.fsm.Log.error("%s", se.getMessage());
            return new Data.Error(String.format("Eval of '%s' failed: %s", script, se.getMessage()));
        }
    }

    @Override
    public ScriptProducer createScriptProducer() {
        return null;
    }

}
