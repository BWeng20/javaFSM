package com.bw.fsm.datamodel.expression_engine.action;

import com.bw.fsm.Data;
import com.bw.fsm.Fsm;
import com.bw.fsm.actions.Action;
import com.bw.fsm.datamodel.GlobalData;

import java.util.List;

public class In implements Action {

    public In(Fsm fsm) {
    }

    @Override
    public Data execute(List<Data> arguments, GlobalData global) throws Exception {
        return null;
    }
}
