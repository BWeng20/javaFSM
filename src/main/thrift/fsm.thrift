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

    oneway void sendEvent(1: i32 session, 2: Event event);

}


/**
 * FSM Trace Server
 */
service TraceServer
 {
    string registerFsm(1: string clientAddress, 2: i32 session);

    oneway void message( 1: string fsm, 2: string message );

    oneway void sentEvent( 1: Event event );
    oneway void receivedEvent( 1: Event event );

    oneway void enterMethod(
        1: string fsm,
        2: string name,
        3: list<Argument> arguments );

    oneway void exitMethod(
        1: string fsm,
        2: string name,
        3: list<Argument> results );

}


/**
 * FSM Trace Client
 */
service TraceClient
 {
    oneway void event( 1: string fsm, 2: Event event );

    /**
     * Executes a expression on the datamodel of the FSM.
     * @param fsm The id of a registered FSM.
     * @param expression The expression to execute.
     * @return The result.
     */
    Data getData( 1: string fsm, 2: string expression );
}