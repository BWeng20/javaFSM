# Finite State Machine (FSM) Implementation in Java

![logo](logo.svg)


For a RUST version of the same implementation see [ruFSM](https://github.com/BWeng20/ruFSM).<br>
Both versions (if productive) will have the same features and API and can in fact work together.

This project implements an embeddable and extendable Harel Statechart interpreter.</br>
Multiple state machines can be read into the runtime and executed in parallel.
Different FSMs can communicate with each other internally or externally via events.

A datamodel can be used to maintain data and execute business logic.

## SW Design

This crate implements a _State Chart XML_ (SCXML) interpreter, according to the W3C Recommendations. See https://www.w3.org/TR/scxml/</br>
For the detailed design, see [SW Design](SW_Design.md)

## Main Features

Input of the FSM Interpreter are SCXML files (_"State Chart extensible Markup Language"_, see W3C for details of the schema).<br>
It describes Harel State Tables with states, transitions and how these element communicate with each other and the extern world.<br>
Also it describes a datamodel, which defines and stores writable data and can execute code to work on it.

Users can choose between different datamodels, extend them or even create new ones.

Some features of this library can be removed by static switches, 
the compiled JVM code will not contain any reverence of it if a switch is turned.<br>
<b>They can't be changed during runtime</b>. These can be used to increase performance and reduce the library size 
and runtime dependencies.

See [StaticOptions](src/main/java/com/bw/fsm/StaticOptions.java).

| Switch (StaticOptions) | Description                                                             | Runtime dependency<br/>if switched on |
|------------------------|-------------------------------------------------------------------------|---------------------------------------|
| debug                  | Adds debug output to FSM execution                                      |                                       |
| debug_reader           | Adds debug output to the SCXML reader (a lot)                           |                                       |
| trace                  | Enables tracing of the FSM algorithm                                    |                                       |
| trace_method           | Enables tracing of methods calls in the FSM (if "trace" is enabled)     |                                       |
| trace_event            | Enables tracing of events in the FSM (if "trace" is enabled)            |                                       |
| trace_state            | Enables tracing of state changes in the FSM (if "trace" is enabled)     |                                       |
| ecma_script_model      | Enabled ECMA Datamodel                                                  | graalvm.js                            |
| rfsm_expression_model  | Adds a datamodel implementation based on the internal Expression-Engine |                                       |


## About SCXML files

SCXML is an XML format, so you need some parser. FSMs will not get easily huge, but XML paring is expensive.
As alternative this project has a binary (but platform independent) file format. 
It has no XML parser dependency and it is much faster.</br>
But you need to convert your SCXML files with the `scxml_to_fsm` application.

FSM can create SCXML source dynamically via the scripting features. Such stuff will again need XML parsing.<br>
But we think this can easily be avoided.

## Tracer

The Tracer package can be used to monitor the FSM.<br/>
The default-tracer simply prints the traced actions. If the Remote-Trace-Server-Feature is enabled, a Server is
started that can be used by remote-clients (_to be done_).

The Tracer has different flags to control what is traced, see Enum [TraceMode](src/main/java/com/bw/fsm/tracer/TraceMode.java) for details.<br>
Remind, that these runtime flags only work, if these features are statically enabled (see above).

## How To Use

FSMs normally are used embedded inside other software to control some state-full workflow.<br/>
The transitions or the states trigger operations in the business workflow .  
To bind the business logic to a state machine different approaches exits. In hard-coded FSMs methods-calls are directly
linked to the transitions or state-actions during compile-time. Most state machine frameworks work this way.<br/>

This project loads FSMs during runtime, so the binding to the FSM needs to be dynamical.<br/>
SCXML defines a _Datamodel_ for this purpose.

### Datamodel

For details about the Datamodel-concept in SCXML see the W3C documentation. This lib provides some implementations of
the Datamodel, but you can implement your own models as well.<br/>
The Datamodel in SCXML is responsible to execute scripts and formulas. Custom business logic can be implemented this way.<br/>
For some huge project, it may be feasible to implement the Datamodel-trait with some optimized way to trigger the
business-functionality.<br/>

For a simpler approach (without implement a full Datamodel), see "Custom Actions" below.
You can also find some examples inside folder "src/examples".<br/>
Each Datamodel has a unique identifier, that can be selected in the SCXML-source, so you can provide multiple model-implementation in
one binary and use them in parallel in different FSMs.

To add new data-models, use function ``.

### Provided Datamodel Implementations

+ EMCAScript-Datamodel, use `datamodel="ecmascript"`. Available if feature _"ECMAScriptModel"_ is turned on.
+ The Null-Datamodel, use `datamodel="null"`
+ Internal Expression Engine Datamodel, use `datamodel="rfsm-expression"`. Available if switch _"rfsm_expression_model"_ is turned on.

As interpreting JavaScript is expensive and if you need only basic logic in your scripts, 
use "rfsm-expression" instead.

For details see the [Expression-Engine-Readme](src/main/java/com/bw/fsm/expression_engine/README.md).

### Custom Actions

You can use the trait "Action" to add custom functions to the FSM. See the Examples for a How-To.
Each FSM instance can have a different set of Actions. Action are inherited by child-sessions.

If using ECMAScript- or RfsmExpressions-Datamodel, these actions can be called like normal methods.
Arguments and return values will be converted from and to JavaScript/Data-values. See enum "Data" for supported data-types.

Actions have full access to the data and states of the FSM.

## Tests

For basic functions the project contains several unit tests. The current status of these tests can be seen on the
repository start page.

More complex tests are done by test scripts that executes SCXML-files provided by the W3C.</br>
Currently, the project passed all 160 of the mandatory automated tests from the W3C test-suite.
For the detailed test process see [W3C Test Readme](W3C_TESTS_README.md) and for the results [Test Report](w3ctest/REPORT.MD).
