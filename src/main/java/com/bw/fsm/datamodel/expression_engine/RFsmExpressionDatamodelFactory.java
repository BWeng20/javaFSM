package com.bw.fsm.datamodel.expression_engine;

import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;

import java.util.Map;

public class RFsmExpressionDatamodelFactory extends DatamodelFactory {
    @Override
    public Datamodel create(GlobalData global_data, Map<String, String> options) {
        return new RFsmExpressionDatamodel(global_data);
    }
}
