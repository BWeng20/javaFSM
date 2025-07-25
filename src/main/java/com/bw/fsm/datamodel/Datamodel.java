package com.bw.fsm.datamodel;

import com.bw.fsm.*;
import com.bw.fsm.event_io_processor.EventIOProcessor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class Datamodel {

    /// Returns the global data.\
    /// As the data model needs access to other global variables and rust doesn't like
    /// accessing data of parents (Fsm in this case) from inside a child (the actual Datamodel), most global data is
    /// store in the "GlobalData" struct that is owned by the data model.
    public abstract GlobalData global();

    /// Get the name of the data model as defined by the \<scxml\> attribute "datamodel".
    public abstract String get_name();

    /// Adds the "In" and other function.\
    /// If needed, adds also "log" function.
    public abstract void add_functions(Fsm fsm);

    /// sets '_ioprocessors'.
    public abstract void set_ioprocessors();

    /// Initialize the data model for one data-store.
    /// This method is called for the global data and for the data of each state.
    public void initializeDataModel(Fsm fsm, State state, boolean set_data) {
        // Set all (simple) global variables.
        this.set_from_state_data(state.data, set_data);
        if (state == fsm.pseudo_root) {
            this.set_from_state_data(this.global().environment, true);
        }
    }

    /// Sets data from state data-store.\
    /// All data-elements contain script-source and needs to be evaluated by the datamodel before use.
    /// set_data - if true set the data, otherwise just initialize the variables.
    public abstract void set_from_state_data(Map<String, Data> data, boolean set_data);

    /// Initialize a global read-only variable.
    public abstract void initialize_read_only(String name, Data value);

    /// Sets a global variable.
    public abstract void set(String name, Data data, boolean allow_undefined);

    // Sets system variable "_event"
    public abstract void set_event(Event event);

    /// Execute an assign expression.
    /// Returns true if the assignment was correct.
    public abstract boolean assign(Data left_expr, Data right_expr);

    /// Gets a global variable by a location expression.\
    /// If the location is undefined or the location expression is invalid,
    /// "error.execute" shall be put inside the internal event queue.\
    /// See [internal_error_execution](Datamodel::internal_error_execution).
    public abstract Data get_by_location(String location);

    /// Convenient function to retrieve a value that has an alternative expression-value.\
    /// If value_expression is empty, Ok(value) is returned (if empty or not). If the expression
    /// results in error Err(message) and "error.execute" is put in internal queue.
    /// See [internal_error_execution](Datamodel::internal_error_execution).
    public Data get_expression_alternative_value(Data value, Data value_expression) {
        if (value_expression.is_empty()) {
            return value;
        } else {
            return this.execute(value_expression);
        }
    }

    /// Get an _ioprocessor by name.
    public EventIOProcessor get_io_processor(String name) {
        return this.global().io_processors.get(name);
    }

    /// Send an event via io-processor.
    /// Mainly here because of optimization reasons (spared copies).
    public boolean send(String ioc_processor, Data target, Event event) {
        EventIOProcessor ioc = this.get_io_processor(ioc_processor);
        if (ioc != null) {
            return ioc.send(this.global(), target.toString(), event);
        } else {
            return false;
        }
    }

    /// Clear all data.
    public abstract void clear();

    /// "log" function, use for \<log\> content.
    public void log(String msg) {
        StaticOptions.info("%s", msg);
    }

    /// Executes a script.\
    /// If the script execution fails, "error.execute" shall be put
    /// inside the internal event queue.
    /// See [internal_error_execution](Datamodel::internal_error_execution).
    public abstract Data execute(Data script);

    /// Executes a for-each loop
    public abstract boolean execute_for_each(
            Data array_expression,
            String item,
            String index,
            Function<Datamodel, Boolean> execute_body
    );

    /// *W3C says*:\
    /// The set of operators in conditional expressions varies depending on the data model,
    /// but all data models must support the 'In()' predicate, which takes a state ID as its
    /// argument and returns true if the state machine is in that state.\
    /// Conditional expressions in conformant SCXML documents should not have side effects.
    /// #Actual Implementation:
    /// As no side effects shall occur, this method should be "&this". But we assume that most script-engines have
    /// no read-only "eval" function and such method may be hard to implement.
    public abstract boolean execute_condition(Data script);

    /// Executes content.
    public abstract boolean executeContent(Fsm fsm, ExecutableContent content);

    /// *W3C says*:\
    /// Indicates that an error internal to the execution of the document has occurred, such as one
    /// arising from expression evaluation.
    public void internal_error_execution_with_event(Event event) {
        global().enqueue_internal(Event.error_execution_with_event(event));
    }

    /// *W3C says*:\
    /// Indicates that an error internal to the execution of the document has occurred, such as one
    /// arising from expression evaluation.
    public void internal_error_execution_for_event(String send_id, Integer invoke_id) {
        global().enqueue_internal(Event.error_execution(send_id, invoke_id));
    }

    /// *W3C says*:\
    /// Indicates that an error internal to the execution of the document has occurred, such as one
    /// arising from expression evaluation.
    public void internal_error_execution() {
        global().enqueue_internal(Event.error_execution(null, null));
    }

    /// *W3C says*:\
    /// W3C: Indicates that an error has occurred while trying to communicate with an external entity.
    public void internal_error_communication(Event event) {
        global().enqueue_internal(Event.error_communication(event));
    }

    /// Evaluates a content element.\
    /// Returns the static content or executes the expression.
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
                    StaticOptions.error("content expr '%s' is invalid (%s)", content.content, e.getMessage());
                    // W3C:\
                    // If the evaluation of 'expr' produces an error, the Processor must place
                    // error.execution in the internal event queue and use the empty string as
                    // the value of the <content> element.
                    this.internal_error_execution();
                }
            }
        }
        return null;
    }

    /// Evaluates a list of Param-elements and
    /// returns the resulting data
    public void evaluate_params(List<Parameter> params, List<ParamPair> values) {
        if (params != null) {
            for (Parameter param : params) {
                if (!param.location.isEmpty()) {
                    Data data = get_by_location(param.location);
                    if (data == null) {
                        // W3C:\
                        // If the 'location' attribute does not refer to a valid location in
                        // the data model, ..., the SCXML Processor must place the error
                        // 'error.execution' on the internal event queue and must ignore the name
                        // and value.
                        StaticOptions.error("location of param %s is invalid", param);
                        // get_by_location already added "error.execution"
                    } else {
                        values.add(new ParamPair(param.name, data));
                    }
                } else if (!param.expr.isEmpty()) {
                    Data data = execute(new Data.Source(param.expr));
                    if (data == null) {
                        //  W3C:\
                        // ...if the evaluation of the 'expr' produces an error, the SCXML
                        // Processor must place the error 'error.execution' on the internal event
                        // queue and must ignore the name and value.
                        StaticOptions.error("expr of param %s is invalid", param);
                        this.internal_error_execution();
                    } else {
                        values.add(new ParamPair(param.name, data));
                    }

                }
            }
        }
    }
}
