package com.bw.fsm.datamodel.ecma;

import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;

import java.util.Map;

public class ECMAScriptDatamodelFactory extends DatamodelFactory {
    @Override
    public Datamodel create(GlobalData global_data, Map<String, String> options) {
        return new ECMAScriptDatamodel(global_data);
    }
}
