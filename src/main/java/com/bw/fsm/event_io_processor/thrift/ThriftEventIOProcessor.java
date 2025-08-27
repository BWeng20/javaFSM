package com.bw.fsm.event_io_processor.thrift;

import com.bw.fsm.BlockingQueue;
import com.bw.fsm.Data;
import com.bw.fsm.Event;
import com.bw.fsm.Log;
import com.bw.fsm.datamodel.GlobalData;
import com.bw.fsm.datamodel.JsonScriptProducer;
import com.bw.fsm.event_io_processor.EventIOProcessor;
import com.bw.fsm.thrift.Argument;
import com.bw.fsm.thrift.DatamodelType;
import com.bw.fsm.thrift.ThriftIO;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThriftEventIOProcessor extends EventIOProcessor {

    public static final String THRIFT_EVENT_PROCESSOR = "http://www.w3.org/TR/scxml/#ThriftEventProcessor";

    /**
     * Shortcut for Thrift I/O Processors type
     */
    public static final String THRIFT_EVENT_PROCESSOR_SHORT_TYPE = "thrift";

    private String location;

    @Override
    public String get_location(Integer id) {
        return String.format("%s/session-%d", this.location, id);
    }

    @Override
    public List<String> get_types() {
        return List.of(THRIFT_EVENT_PROCESSOR, THRIFT_EVENT_PROCESSOR_SHORT_TYPE);
    }

    @Override
    public Map<Integer, BlockingQueue<Event>> get_external_queues() {
        return Map.of();
    }

    @Override
    public boolean send(GlobalData global, String target, @NotNull Event event) {
        int sessionIndex = target.indexOf("/session-");
        if (sessionIndex < 0) {
            Log.error("Thrift target format invalid: " + target);
            global.enqueue_internal(Event.error_communication(event));
        }

        try {
            int targetSessionId = Integer.parseInt(target.substring(sessionIndex + 9));
            return sendViaThrift(target.substring(0, sessionIndex), global.session_id, targetSessionId, event);

        } catch (NumberFormatException be) {
            Log.error("Thrift session id invalid");
        }

        return false;
    }


    public static com.bw.fsm.thrift.Data toThrift(@NotNull Data data) {

        var thriftData = new com.bw.fsm.thrift.Data();

        var producer = new JsonScriptProducer(false);
        data.as_script(producer);
        thriftData.value = producer.finish();

        switch (data.type) {
            case Integer, Double -> thriftData.type = DatamodelType.Number;
            case Boolean -> thriftData.type = DatamodelType.Boolean;
            case String -> thriftData.type = DatamodelType.String;
            case Array -> thriftData.type = DatamodelType.Array;
            case Map -> thriftData.type = DatamodelType.Map;
            case Fsm -> thriftData.type = DatamodelType.Fsm;
            case Source -> thriftData.type = DatamodelType.Source;
            case None -> thriftData.type = DatamodelType.None;
            case Null -> thriftData.type = DatamodelType.Null;
            case Error -> thriftData.type = DatamodelType.Error;
        }
        return thriftData;
    }


    public static com.bw.fsm.thrift.Event toThrift(@NotNull Event event) {
        com.bw.fsm.thrift.Event thriftEvent = new com.bw.fsm.thrift.Event();

        thriftEvent.type = switch (event.etype) {
            case external -> com.bw.fsm.thrift.EventType.external;
            case internal -> com.bw.fsm.thrift.EventType.internal;
            case platform -> com.bw.fsm.thrift.EventType.platform;
        };

        thriftEvent.name = event.name;
        thriftEvent.invoke_id = event.invoke_id;
        thriftEvent.sendid = event.sendid;

        thriftEvent.origin = event.origin;
        thriftEvent.origin_type = event.origin_type;
        thriftEvent.content = event.content == null ? null : toThrift(event.content);

        if (event.param_values != null) {
            thriftEvent.param_values = new ArrayList<>(event.param_values.size());
            for (var p : event.param_values) {
                Argument a = new Argument();
                a.name = p.name;
                a.value = toThrift(p.value);
                thriftEvent.param_values.add(a);
            }
        }
        return thriftEvent;
    }


    /**
     *
     * @param url             The transport Url as used by {@link ThriftIO#createClientTransportFromAddress(String)}
     * @param sourceSessionId The id of the local session that sends this event                    .
     * @param targetSessionId The id of the session inside the target processor.
     * @param event           The event to send.
     * @return
     */
    protected boolean sendViaThrift(String url, int sourceSessionId, int targetSessionId, @NotNull Event event) {
        event.origin_type = THRIFT_EVENT_PROCESSOR_SHORT_TYPE;
        if (event.origin == null) {
            event.origin = this.get_location(sourceSessionId);
        }

        try (TTransport transport = ThriftIO.createClientTransportFromAddress(url)) {

            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);

            com.bw.fsm.thrift.Event thriftEvent = toThrift(event);
            thriftEvent.sourceFsm = location + "/session-" + sourceSessionId;
            thriftEvent.targetFsm = url + "/session-" + targetSessionId;

            var eventIOClient = new com.bw.fsm.thrift.EventIOProcessor.Client(protocol);
            eventIOClient.sendEvent(targetSessionId, thriftEvent);

        } catch (TTransportException te) {
            Log.exception("Failed to connect", te);
        } catch (TException e) {
            Log.exception("Failed to send", e);
        }
        return false;
    }

    @Override
    public void shutdown() {

    }
}
