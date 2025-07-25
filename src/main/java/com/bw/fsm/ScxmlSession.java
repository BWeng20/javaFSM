package com.bw.fsm;

import com.bw.fsm.datamodel.GlobalData;

/// Represents some external session.
/// Holds thread-id and channel-sender to the external queue of the session.
public class ScxmlSession {

    public Integer session_id;

    public Thread thread;
    public BlockingQueue<Event> sender;

    /// global_data should be access after the FSM is finished to avoid deadlocks.
    public GlobalData global_data;

    /// Doc-id of the Invoke element that triggered this session.
    /// InvokeIds are generated if not specified, to identify the invoke element, the doc-id
    /// is used.
    public Integer invoke_doc_id;

    /// State of the invoke or null.
    public State state;

    public ScxmlSession(Integer id, BlockingQueue<Event> sender) {
        this.session_id = id;
        this.sender = sender;
        this.global_data = new GlobalData();
    }
}
