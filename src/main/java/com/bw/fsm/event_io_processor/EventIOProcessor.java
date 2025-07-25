package com.bw.fsm.event_io_processor;

import com.bw.fsm.BlockingQueue;
import com.bw.fsm.Event;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.GlobalData;

import java.util.List;
import java.util.Map;

/// Trait for Event I/O Processors. \
/// See [../../../doc/W3C_SCXML_2024_07_13/index.html#eventioprocessors].
/// As the I/O Processors hold session related data, an instance of this trait must be bound to one session,
/// but may share backends with other sessions, e.g. a http server.
public abstract class EventIOProcessor {

    /// Returns the location of this session and processor.
    public abstract String get_location(Integer id);

    /// Returns the type names of this processor.
    public abstract List<String> get_types();

    public abstract Map<Integer, BlockingQueue<Event>> get_external_queues();

    public void add_fsm(Fsm fsm, Datamodel datamodel) {
        GlobalData global = datamodel.global();
        this.get_external_queues()
                .put(global.session_id, global.externalQueue);
    }

    public abstract boolean send(GlobalData global, String target, Event event);

    public abstract void shutdown();
}
