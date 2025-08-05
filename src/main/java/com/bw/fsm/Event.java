package com.bw.fsm;

/// *W3C says*:
/// ## The Internal Structure of Events.
/// Events have an internal structure which is reflected in the _event variable. This variable can be
/// accessed to condition transitions (via boolean expressions in the 'cond' attribute) or to update
/// the data model (via \<assign\>), etc.
///
/// The SCXML Processor must ensure that the following fields are present in all events, whether
/// internal or external.
///
/// - name. This is a character string giving the name of the event. The SCXML Processor must set
///   the name field to the name of this event. It is what is matched against the 'event' attribute
///   of \<transition\>. Note that transitions can do additional tests by using the value of this
///   field inside boolean expressions in the 'cond' attribute.
/// - type. This field describes the event type. The SCXML Processor must set it to: "platform"
///   (for events raised by the platform itself, such as error events), "internal" (for events
///   raised by \<raise\> and \<send\> with target '_internal') or "external" (for all other events).
/// - sendid. If the sending entity has specified a value for this, the Processor must set this
///   field to that value (see C Event I/O Processors for details). Otherwise, in the case of error
///   events triggered by a failed attempt to send an event, the Processor must set this field to
///   the send id of the triggering \<send\> element. Otherwise it must leave it blank.
/// - origin. This is a URI, equivalent to the 'target' attribute on the \<send\> element. For
///   external events, the SCXML Processor should set this field to a value which, when used as the
///   value of 'target', will allow the receiver of the event to \<send\> a response back to the
///   originating entity via the Event I/O Processor specified in 'origintype'. For internal and
///   platform events, the Processor must leave this field blank.
/// - origintype. This is equivalent to the 'type' field on the \<send\> element. For external events,
///   the SCXML Processor should set this field to a value which, when used as the value of 'type',
///   will allow the receiver of the event to \<send\> a response back to the originating entity at
///   the URI specified by 'origin'. For internal and platform events, the Processor must leave this
///   field blank.
/// - invokeid. If this event is generated from an invoked child process, the SCXML Processor must
///   set this field to the invoke id of the invocation that triggered the child process. Otherwise
///   it must leave it blank.
/// - data. This field contains whatever data the sending entity chose to include in this event.
///   The receiving SCXML Processor should reformat this data to match its data model, but must not
///   otherwise modify it. If the conversion is not possible, the Processor must leave the field
///   blank and must place an error 'error.execution' in the internal event queue.
public class Event {
    public String name;
    public EventType etype;
    public String sendid;
    public String origin;
    public String origin_type;
    public Integer invoke_id;

    /// Name-Value pairs from \<param\> elements.
    public java.util.List<ParamPair> param_values;

    /// Content from \<content\> element.
    public Data content;

    public Event() {
        this.name = "";
        this.etype = EventType.external;
    }

    public static Event new_external() {
        return new Event();
    }

    public static Event new_simple(String name) {
        Event event = new Event();
        event.name = name;
        return event;
    }

    public static Event new_event(String prefix, String id, java.util.List<ParamPair> dataParams,
                                  Data dataContent, EventType eventType) {
        Event event = new Event();
        event.name = prefix + id;
        event.etype = eventType;
        event.param_values = dataParams;
        event.content = dataContent;
        return event;
    }

    public static Event error(String name) {
        Event event = new Event();
        event.name = "error." + name;
        event.etype = EventType.platform;
        return event;
    }

    public static Event error_execution_with_event(Event event) {
        Event err = new Event();
        err.name = "error.execution";
        err.etype = EventType.platform;
        err.sendid = event.sendid;
        err.origin = event.origin;
        err.invoke_id = event.invoke_id;
        err.origin_type = event.origin_type;
        return err;
    }

    public static Event error_execution(String sendId, Integer invokeId) {
        Event err = new Event();
        err.name = "error.execution";
        err.etype = EventType.platform;
        err.sendid = sendId;
        err.invoke_id = invokeId;
        return err;
    }

    public static Event error_communication(Event event) {
        Event err = new Event();
        err.name = "error.communication";
        err.etype = EventType.platform;
        err.sendid = event.sendid;
        err.origin = event.origin;
        err.invoke_id = event.invoke_id;
        err.origin_type = event.origin_type;
        return err;
    }

    public Event get_copy() {
        Event copy = new Event();
        copy.name = this.name;
        copy.etype = this.etype;
        copy.sendid = this.sendid;
        copy.origin = this.origin;
        copy.origin_type = this.origin_type;
        copy.invoke_id = this.invoke_id;
        copy.param_values = this.param_values;
        copy.content = this.content;
        return copy;
    }

    @Override
    public String toString() {
        return name;
    }
}
