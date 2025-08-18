package com.bw.fsm.event_io_processor;

import com.bw.fsm.*;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.GlobalData;

import java.util.List;
import java.util.Map;

/**
 * Interface for Event I/O Processors.<br>
 * As the I/O Processors hold session related data, an instance of this trait must be bound to one session,
 * but may share backends with other sessions, e.g. an http server.
 */
public abstract class EventIOProcessor {

    public static final String SYS_IO_PROCESSORS = "_ioprocessors";

    /** Returns the location of this session and processor. */
    public abstract String get_location(Integer id);

    /**  Returns the type names of this processor.*/
    public abstract List<String> get_types();

    public abstract Map<Integer, BlockingQueue<Event>> get_external_queues();

    public void add_fsm(Fsm fsm, Datamodel datamodel) {
        GlobalData global = datamodel.global();
        this.get_external_queues()
                .put(global.session_id, global.externalQueue);
    }

    public abstract boolean send(GlobalData global, String target, Event event);

    public abstract void shutdown();

    protected void shutdownQueues(Map<Integer, BlockingQueue<Event>> queues) {
        Event cancel_event = Event.new_simple(Fsm.EVENT_CANCEL_SESSION);
        for (var queue : queues.entrySet()) {
            if (StaticOptions.debug_option)
                Log.debug("Send cancel to fsm #%s", queue.getKey());
            queue.getValue().enqueue(cancel_event.get_copy());
        }
    }
}
