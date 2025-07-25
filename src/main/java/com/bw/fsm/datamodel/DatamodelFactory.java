package com.bw.fsm.datamodel;

import java.util.Map;

public interface DatamodelFactory {

    /// Create a NEW datamodel.
    Datamodel create(GlobalData global_data, Map<String, String> options);
}
