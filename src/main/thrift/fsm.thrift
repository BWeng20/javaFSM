namespace java com.bw.fsm.thrift

enum EventType {
  platform = 0,
  internal = 1,
  external = 2
}

struct Argument {
    1: string name,
    2: Data value
}

struct Event {
    1: string name,
    2: EventType type,
    3: string sourceFsm,
    4: string targetFsm,

    5: string sendid,
    6: string origin,
    7: string origin_type,
    8: string invoke_id,

    9: list<Argument> param_values,
    10: Data content
}


enum DatamodelType {
  Error   = 0,
  Null    = 1,
  None    = 2
  String  = 3,
  Number  = 4,
  Boolean = 5,
  Source  = 6,
  Array   = 7,
  Map     = 8,
  Fsm     = 10,
}

struct Data {
     /**
      * The type of the result.
      */
     1: DatamodelType type,

     /**
      * The string representation of the result
      */
     3: string value
}

/**
 * FSM IO Processor
 */
service EventIOProcessor {

    /**
     * Sends a external event to the processor.
     */
    oneway void sendEvent(1: i32 session, 2: Event event);

    list<string> getConfiguration(1: i32 session)

}

struct NamedArgument {
    1: string name,
    2: string value
}

/**
 * FSM Trace Server
 */
service TraceServer
 {
    /**
     * Processor registers a FSM.
     * @param clientAddress The thrift url of the TraceClient.
     * @param fsmName the name of the FSM model (only informational)
     * @param session the session id
     * @return the id to use in the other calls as value of argument "fsm".
     */
    string registerFsm(1: string clientAddress, 3: string fsmName, 2: i32 session);

    oneway void unregisterFsm(1: string fsmId );

    /**
     * Some generic message.
     */
    oneway void message( 1: string fsm, 2: string message );

    /**
     * A Event was sent.
     */
    oneway void sentEvent( 1:string fsm, 2: Event event );

    /**
     * A Event was received.
     */
    oneway void receivedEvent( 1:string fsm, 2: Event event );

    /**
     * A method was entered.
     */
    oneway void enterMethod(
        1: string fsm,
        2: string name,
        3: list<NamedArgument> arguments );

    /**
     * A method was finished.
     */
    oneway void exitMethod(
        1: string fsm,
        2: string name,
        3: list<NamedArgument> results );

}


/**
 * FSM Trace Client
 */
service TraceClient
 {
    /**
     * Injects a event in the session.
     * This method can send also internal or platform events.
     */
    oneway void event( 1: string fsm, 2: Event event );

    /**
     * Executes a expression on the datamodel of the FSM.
     * @param fsm The id of a registered FSM.
     * @param expression The expression to execute.
     * @return The result.
     */
    Data getData( 1: string fsm, 2: string expression );
}