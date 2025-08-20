package com.bw.fsm.datamodel.expression_engine;

import com.bw.fsm.Fsm;
import com.bw.fsm.ScriptProducer;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.expression_engine.Expression;

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

    @Override
    public void add_functions(Fsm fsm) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public ScriptProducer createScriptProducer() {
        return null;
    }

}
