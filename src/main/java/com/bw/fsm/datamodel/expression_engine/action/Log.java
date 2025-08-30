package com.bw.fsm.datamodel.expression_engine.action;

import com.bw.fsm.Data;
import com.bw.fsm.actions.Action;
import com.bw.fsm.datamodel.GlobalData;

import java.util.List;

public class Log implements Action {

    @Override
    public Data execute(List<Data> arguments, GlobalData global) throws Exception {
        StringBuilder sb = new StringBuilder(100);
        sb.append("Exp: ");
        for (Data d : arguments) {

            sb.append(d.toString());
        }
        com.bw.fsm.Log.info(sb.toString());
        return Data.None.NONE;
    }

}
