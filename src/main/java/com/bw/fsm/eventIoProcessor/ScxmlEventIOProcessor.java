package com.bw.fsm.eventIoProcessor;

import com.bw.fsm.*;
import com.bw.fsm.datamodel.GlobalData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the SCXML I/O Event Processor.<br>
 * I/O Processor implementation for "http://www.w3.org/TR/scxml/#SCXMLEventProcessor" (or short-cut "scxml").
 * See <a href="https://www.w3.org/TR/scxml/#SCXMLEventProcessor">W3C:SCXML - SCXML Event I/O Processor</a>,
 */
public class ScxmlEventIOProcessor extends EventIOProcessor {

    public static final String SCXML_EVENT_PROCESSOR = "http://www.w3.org/TR/scxml/#SCXMLEventProcessor";

    /**
     * W3C: Processors MAY define short form notations as an authoring convenience.<br>
     * Shortcut for SCXML I/O Processors type
     */
    public static final String SCXML_EVENT_PROCESSOR_SHORT_TYPE = "scxml";


    /**
     * SCXML Processors specific target:<br>
     * If the target is the special term '#_internal', the Processor must add the
     * event to the internal event queue of the sending session.
     */
    public static final String SCXML_TARGET_INTERNAL = "#_internal";

    /**
     * SCXML Processors specific target:<br>
     * If the target is the special term '#_scxml_sessionid', where sessionid is the id of an SCXML session that
     * is accessible to the Processor, the Processor must add the event to the external queue of that session.
     * The set of SCXML sessions that are accessible to a given SCXML Processor is platform-dependent.
     */
    public static final String SCXML_TARGET_SESSION_ID_PREFIX = "#_scxml_";

    /**
     * SCXML Processors specific target:<br>
     * If the target is the special term '#_parent', the Processor must add the event to the external
     * event queue of the SCXML session that invoked the sending session, if there is one.
     */
    public static final String SCXML_TARGET_PARENT = "#_parent";

    /**
     * SCXML Processors specific target:<br>
     * If the target is the special term '#_invokeid', where invokeid is the invokeid of an SCXML session that the sending session has created by &lt;invoke>,
     * the Processor must add the event to the external queue of that session.<br>
     * This value is prefix of the other SCXML targets and need special care.
     */
    public static final String SCXML_TARGET_INVOKE_ID_PREFIX = "#_";

    public String location = SCXML_TARGET_SESSION_ID_PREFIX;
    public Map<Integer, BlockingQueue<Event>> sessions = new HashMap<>();

    public ScxmlEventIOProcessor() {
        if (StaticOptions.debug)
            Log.debug("Scxml Event Processor starting");
    }

    public boolean send_to_session(GlobalData gd, int toSessionId, Event event) {
        if (gd.executor == null) {
            Log.panic("Executor not available");
            return false;
        } else {
            if (StaticOptions.trace_event)
                gd.tracer.event_external_sent(gd.session_id, toSessionId, event);
            gd.executor.send_to_session(toSessionId, event);
            return true;
        }
    }

    @Override
    public String get_location(Integer id) {
        return String.format("%s%s", this.location, id);
    }

    public static final List<String> TYPES = List.of(SCXML_EVENT_PROCESSOR, SCXML_EVENT_PROCESSOR_SHORT_TYPE);

    @Override
    public List<String> get_types() {
        return TYPES;
    }

    @Override
    public Map<Integer, BlockingQueue<Event>> get_external_queues() {
        return this.sessions;
    }

    /**
     * <b>W3C</b>: (only the relevant parts)<br>
     * Generated Events: <ul>
     * <li>The 'origin' field of the event raised in the receiving session must match the value of the
     * 'location' field inside the entry for the SCXML Event I/O Processor in the _ioprocessors
     * system variable in the sending session.</li>
     * <li>The 'origintype' field of the event raised in the receiving session must have the value "scxml".</li>
     * </ul>
     * SCXML Processors must support the following special targets for &lt;send\>:<ul>
     * <li>#_internal. If the target is the special term '#_internal', the Processor must add the event to the internal event queue of the sending session.</li>
     * <li>#_scxml_sessionid. If the target is the special term '#_scxml_sessionid', where sessionid is the id of an SCXML session that is accessible to the Processor, the Processor must add the event to the external queue of that session. The set of SCXML sessions that are accessible to a given SCXML Processor is platform-dependent.</li>
     * <li>#_parent. If the target is the special term '#_parent', the Processor must add the event to the external event queue of the SCXML session that invoked the sending session, if there is one. See 6.4 &lt;invoke\> for details.</li>
     * <li>#_invokeid. If the target is the special term '#_invokeid', where invokeid is the invokeid of an SCXML session that the sending session has created by &lt;invoke\>, the Processor must add the event to the external queue of that session. See 6.4 &lt;invoke\> for details.</li>
     * <li>If neither the 'target' nor the 'targetexpr' attribute is specified, the SCXML Processor must add the event to the external event queue of the sending session.</li>
     * </ul>
     */
    @Override
    public boolean send(@NotNull GlobalData global, @NotNull String target, @NotNull Event event) {
        event.origin_type = SCXML_EVENT_PROCESSOR_SHORT_TYPE;
        if (event.origin == null) {
            event.origin = this.get_location(global.session_id);
        }
        // For SCXMLEventProcessor: Target is an SCXML session.

        // W3C: If the sending SCXML session specifies a session that does not exist or is inaccessible,
        //      the SCXML Processor must place the error "error.communication" on the internal event queue of the sending session.
        if (target.isEmpty()) {
            global.externalQueue.enqueue(event);
            return true;
        } else if (SCXML_TARGET_INTERNAL.equals(target)) {
            event.etype = EventType.internal;
            global.enqueue_internal(event);
            return true;
        } else if (SCXML_TARGET_PARENT.equals(target)) {
            return this.send_to_session(global, global.parent_session_id, event);
        } else if (target.startsWith(SCXML_TARGET_SESSION_ID_PREFIX)) {
            String session_id_s = target.substring(SCXML_TARGET_SESSION_ID_PREFIX.length());
            if (session_id_s.isEmpty()) {
                Log.error("Send target '%s' has wrong format.", target);
                global.enqueue_internal(Event.error_communication(event));
                return false;
            } else {
                try {
                    int session_id = Integer.parseInt(session_id_s);
                    return this.send_to_session(global, session_id, event);
                } catch (NumberFormatException ne) {
                    Log.error("Send target '%s' has wrong format.", target);
                    global.enqueue_internal(Event.error_communication(event));
                    return false;
                }
            }
        } else if (target.startsWith(SCXML_TARGET_INVOKE_ID_PREFIX)) {
            String invokeid = target.substring(SCXML_TARGET_INVOKE_ID_PREFIX.length());
            if (invokeid.isEmpty()) {
                Log.error("Send target '%s' has wrong format.", target);
                global.enqueue_internal(Event.error_communication(event));
                return false;
            } else {
                ScxmlSession session = global.child_sessions.get(invokeid);
                if (session == null) {
                    Log.error("InvokeId '%s' of target '%s' is not available.", invokeid, target);
                    global.enqueue_internal(Event.error_communication(event));
                    return false;
                } else {
                    return this.send_to_session(global, session.session_id, event);
                }
            }
        } else {
            // W3C says:
            // If the value ... is not supported or invalid, the Processor MUST place the
            // event error.execution on the internal event queue.
            global.enqueue_internal(Event.error_execution(event.sendid, event.invoke_id));
            return false;
        }
    }

    /**
     * This processor doesn't really need a shutdown.
     * The implementation does nothing.
     */
    @Override
    public void shutdown() {
        if (StaticOptions.debug)
            Log.debug("Scxml Event IO Processor shutdown...");
        shutdownQueues(this.sessions);
    }
}
