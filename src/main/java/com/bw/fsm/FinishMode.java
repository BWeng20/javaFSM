package com.bw.fsm;

/// Mode how the executor handles the ScxmlSession
/// if the FSM is finished.
public enum FinishMode {
    DISPOSE,
    KEEP_CONFIGURATION,
    NOTHING,
}
