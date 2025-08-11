package com.bw.fsm.datamodel;

import com.bw.fsm.*;
import com.bw.fsm.event_io_processor.EventIOProcessor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Data model interface.<br>
 * <b>>W3C says</b>:<br>
 * The Data Model offers the capability of storing, reading, and modifying a set of data that is internal to the state machine.
 * This specification does not mandate any specific data model, but instead defines a set of abstract capabilities that can
 * be realized by various languages, such as ECMAScript or XML/XPath. Implementations may choose the set of data models that
 * they support. In addition to the underlying data structure, the data model defines a set of expressions as described in
 * 5.9 Expressions. These expressions are used to refer to specific locations in the data model, to compute values to
 * assign to those locations, and to evaluate boolean conditions.<br>
 * Finally, the data model includes a set of system variables, as defined in 5.10 System Variables, which are automatically maintained
 * by the SCXML processor.
 */
@SuppressWarnings("unused")
public abstract class Datamodel {

    public static final String DATAMODEL_OPTION_PREFIX = "datamodel:";

    public static final String SCXML_INVOKE_TYPE = "http://www.w3.org/TR/scxml";

    /**
     * W3C: Processors MAY define short form notations as an authoring convenience
     * (e.g., "scxml" as equivalent to <a href="http://www.w3.org/TR/scxml">http://www.w3.org/TR/scxml</a>).
     */
    public static final String SCXML_INVOKE_TYPE_SHORT = "scxml";

    public static final String SCXML_EVENT_PROCESSOR = "http://www.w3.org/TR/scxml/#SCXMLEventProcessor";

    public static final String BASIC_HTTP_EVENT_PROCESSOR = "http://www.w3.org/TR/scxml/#BasicHTTPEventProcessor";

    /**
     * Name of system variable "_sessionid".<br>
     * <b>W3C says</b>:<br>
     * The SCXML Processor MUST bind the variable _sessionid at load time to the system-generated id
     * for the current SCXML session. (This is of type NMTOKEN.) The Processor MUST keep the variable
     * bound to this value until the session terminates.
     */
    public static final String SESSION_ID_VARIABLE_NAME = "_sessionid";

    /**
     * Name of system variable "_name".
     * <b>W3C says</b>:<br>
     * The SCXML Processor MUST bind the variable _name at load time to the value of the 'name'
     * attribute of the &lt;scxml> element. The Processor MUST keep the variable bound to this
     * value until the session terminates.
     */
    public static final String SESSION_NAME_VARIABLE_NAME = "_name";

    /**
     * Name of system variable "_event" for events
     */
    public static final String EVENT_VARIABLE_NAME = "_event";

    /**
     * Name of field "name" of system variable "_event"
     */
    public static final String EVENT_VARIABLE_FIELD_NAME = "name";

    /**
     * Name of field "type" of system variable "_event"
     */
    public static final String EVENT_VARIABLE_FIELD_TYPE = "type";

    /**
     * Name of field of system variable "_event" "sendid"
     */
    public static final String EVENT_VARIABLE_FIELD_SEND_ID = "sendid";

    /**
     * Name of field "origin" of system variable "_event"
     */
    public static final String EVENT_VARIABLE_FIELD_ORIGIN = "origin";

    /**
     * Name of field "origintype" of system variable "_event"
     */
    public static final String EVENT_VARIABLE_FIELD_ORIGIN_TYPE = "origintype";

    /**
     * Name of field "invokeid" of system variable "_event"
     */
    public static final String EVENT_VARIABLE_FIELD_INVOKE_ID = "invokeid";

    /**
     * Name of field "data" of system variable "_event"
     */
    public static final String EVENT_VARIABLE_FIELD_DATA = "data";

    /**
     * Returns the global data.<br>
     * As the data model needs access to other global variables and rust doesn't like
     * accessing data of parents (Fsm in this case) from inside a child (the actual Datamodel), most global data is
     * store in the "GlobalData" struct that is owned by the data model.
     */
    public abstract GlobalData global();

    /**
     * Get the name of the data model as defined by the &lt;scxml> attribute "datamodel".
     */
    public abstract String get_name();

    /**
     * Adds the "In" and other function.<br>
     * If needed, adds also "log" function.
     */
    public abstract void add_functions(Fsm fsm);

    /**
     * sets '_ioprocessors'.
     */
    public void set_ioprocessors() {
    }

    /**
     * Initialize the data model for one data-store.<br>
     * This method is called for the global data and for the data of each state.
     */
    public void initializeDataModel(Fsm fsm, State state, boolean set_data) {
        // Set all (simple) global variables.
        this.set_from_state_data(state.data, set_data);
        if (state == fsm.pseudo_root) {
            this.set_from_state_data(this.global().environment, true);
        }
    }

    /**
     * Sets data from state data-store.<br>
     * All data-elements contain script-source and needs to be evaluated by the datamodel before use.
     *
     * @param data     The data to set.
     * @param set_data if true set the data, otherwise just initialize the variables.
     */
    public void set_from_state_data(Map<String, Data> data, boolean set_data) {
    }

    /**
     * Initialize a global read-only variable.<br>
     * Default implementation does nothing.
     */
    public void initialize_read_only(String name, Data value) {
    }

    /**
     * Sets a global variable.<br>
     * Default implementation does nothing.
     */
    public void set(String name, Data data, boolean allow_undefined) {
    }

    /**
     * Sets system variable "_event"<br>
     * Default implementation does nothing.
     */
    public void set_event(Event event) {
    }

    /**
     * Execute an assign expression.
     *
     * @return true if the assignment was correct.
     */
    public boolean assign(Data left_expr, Data right_expr) {
        return true;
    }

    /**
     * Gets a global variable by a location expression.<br>
     * If the location is undefined or the location expression is invalid,
     * "error.execute" shall be put inside the internal event queue.<br>
     * See {@link Datamodel#internal_error_execution}.
     */
    public Data get_by_location(String location) {
        throw new UnsupportedOperationException();
    }

    /**
     * Convenient function to retrieve a value that has an alternative expression-value.<br>
     * If value_expression is empty, Ok(value) is returned (if empty or not). If the expression
     * results in error Err(message) and "error.execute" is put in internal queue.
     * See [internal_error_execution](Datamodel::internal_error_execution).
     */
    public Data get_expression_alternative_value(Data value, Data value_expression) {
        if (value_expression.is_empty()) {
            return value;
        } else {
            return this.execute(value_expression);
        }
    }

    /**
     * Get an _ioprocessor by name.
     */
    public EventIOProcessor get_io_processor(String name) {
        return this.global().io_processors.get(name);
    }

    /**
     * Send an event via io-processor.
     * Mainly here because of optimization reasons (spared copies).
     */
    public boolean send(String ioc_processor, Data target, Event event) {
        EventIOProcessor ioc = this.get_io_processor(ioc_processor);
        if (ioc != null) {
            return ioc.send(this.global(), target.toString(), event);
        } else {
            return false;
        }
    }

    /**
     * Clear all data.
     */
    public void clear() {
    }

    /**
     * "log" function, use for &lt;log> content.
     */
    public void log(String msg) {
        Log.info("Log: %s", msg);
    }

    /**
     * Executes a script.<br>
     * If the script execution fails, "error.execute" shall be put
     * inside the internal event queue.
     * See {@link Datamodel#internal_error_execution()}.
     */
    public Data execute(Data script) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes a for-each loop
     */
    public boolean execute_for_each(
            Data array_expression,
            String item,
            String index,
            Supplier<Boolean> execute_body
    ) {
        throw new UnsupportedOperationException();
    }

    /**
     * <b>W3C says</b>:<br>
     * The set of operators in conditional expressions varies depending on the data model,
     * but all data models must support the 'In()' predicate, which takes a state ID as its
     * argument and returns true if the state machine is in that state.<br>
     * Conditional expressions in conformant SCXML documents should not have side effects.
     */
    public boolean execute_condition(Data script) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes content.
     */
    public boolean executeContent(Fsm fsm, ExecutableContent content) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes content.
     */
    public boolean executeContent(Fsm fsm, ExecutableContentRegion content) {
        if (content != null) {
            for (var ct : content.content) {
                if (!executeContent(fsm, ct)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <b>W3C says</b>:<br>
     * Indicates that an error internal to the execution of the document has occurred, such as one
     * arising from expression evaluation.
     */
    public void internal_error_execution_with_event(Event event) {
        global().enqueue_internal(Event.error_execution_with_event(event));
    }

    /**
     * <b>W3C says</b>:<br>
     * Indicates that an error internal to the execution of the document has occurred, such as one
     * arising from expression evaluation.
     */
    public void internal_error_execution_for_event(String send_id, String invoke_id) {
        global().enqueue_internal(Event.error_execution(send_id, invoke_id));
    }

    /**
     * <b>W3C says</b>:<br>
     * Indicates that an error internal to the execution of the document has occurred, such as one
     * arising from expression evaluation.
     */
    public void internal_error_execution() {
        global().enqueue_internal(Event.error_execution(null, null));
    }

    /**
     * <b>W3C says</b>:<br>
     * Indicates that an error has occurred while trying to communicate with an external entity.
     */
    public void internal_error_communication(Event event) {
        global().enqueue_internal(Event.error_communication(event));
    }

    /**
     * Evaluates a content element.<br>
     * Returns the static content or executes the expression.
     */
    public Data evaluate_content(CommonContent content) {
        if (content != null) {
            if (content.content_expr != null) {
                try {
                    double d = Double.parseDouble(content.content_expr);
                    if (d == Math.floor(d)) {
                        return new Data.Integer((int) d);
                    } else {
                        return new Data.Double((int) d);
                    }
                } catch (NumberFormatException ne) {
                    return new Data.String(content.content_expr);
                }
            } else if (content.content != null) {
                try {
                    return this.execute(new Data.Source(content.content));
                } catch (Exception e) {
                    Log.error("content expr '%s' is invalid (%s)", content.content, e.getMessage());
                    // W3C:<br>
                    // If the evaluation of 'expr' produces an error, the Processor must place
                    // error.execution in the internal event queue and use the empty string as
                    // the value of the <content> element.
                    this.internal_error_execution();
                }
            }
        }
        return null;
    }

    /**
     * Evaluates a list of Param-elements and
     * returns the resulting data
     */
    public void evaluate_params(List<Parameter> params, List<ParamPair> values) {
        if (params != null) {
            for (Parameter param : params) {
                if (!param.location.isEmpty()) {
                    Data data = get_by_location(param.location);
                    if (data == null) {
                        // W3C:<br>
                        // If the 'location' attribute does not refer to a valid location in
                        // the data model, ..., the SCXML Processor must place the error
                        // 'error.execution' on the internal event queue and must ignore the name
                        // and value.
                        Log.error("location of param %s is invalid", param);
                        // get_by_location already added "error.execution"
                    } else {
                        values.add(new ParamPair(param.name, data));
                    }
                } else if (!param.expr.isEmpty()) {
                    Data data = execute(new Data.Source(param.expr));
                    if (data == null) {
                        //  W3C:<br>
                        // ...if the evaluation of the 'expr' produces an error, the SCXML
                        // Processor must place the error 'error.execution' on the internal event
                        // queue and must ignore the name and value.
                        Log.error("expr of param %s is invalid", param);
                        this.internal_error_execution();
                    } else {
                        values.add(new ParamPair(param.name, data));
                    }
                }
            }
        }
    }
}
