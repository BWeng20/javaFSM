package com.bw.fsm.actions;

import com.bw.fsm.Data;
import com.bw.fsm.datamodel.GlobalData;

import java.util.List;

/// Interface to inject custom actions into the datamodel.
public interface Action {
    /// Executes the action.
    Data execute(List<Data> arguments, GlobalData global) throws Exception;
}
