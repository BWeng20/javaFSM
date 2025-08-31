package com.bw.fsm;

import com.bw.fsm.eventIoProcessor.EventIOProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutorState {
    public final List<EventIOProcessor> processors = new ArrayList<>();
    public final Map<Integer, ScxmlSession> sessions = new HashMap<>();
    public final Map<String, String> datamodel_options = new HashMap<>();
}
