package com.bw.fsm;

import com.bw.fsm.executable_content.Parameter;

import java.util.ArrayList;

/**
 * <b>W3C says</b>:
 * The &lt;invoke> element is used to create an instance of an external service.
 */
public class Invoke {

    /// The unique id, counting in document order.
    public Integer doc_id;

    /**
     * <b>W3C says</b>:<br>
     * Attribute 'idlocation':<br>
     * Location expression.<br>
     * Any data model expression evaluating to a data model location.<br>
     * Must not occur with the 'id' attribute.
     */
    public String external_id_location;

    /**
     * <b>W3C says</b>:<br>
     * Attribute 'type':<br>
     * A URI specifying the type of the external service.
     */
    public Data type_name;

    /**
     * <b>W3C says</b>:<br>
     * Attribute 'typeexpr':<br>
     * A dynamic alternative to 'type'. If this attribute is present, the SCXML Processor must evaluate it
     * when the parent &lt;invoke> element is evaluated and treat the result as if it had been entered as
     * the value of 'type'.
     */
    public Data type_expr;

    /**
     * <b>W3C says</b>:<br>
     * List of valid location expressions
     */
    public final java.util.List<String> name_list = new ArrayList<>();

    /**
     * <b>W3C says</b>:<br>
     * A URI to be passed to the external service.\
     * Must not occur with the 'srcexpr' attribute or the &lt;content> element.
     */
    public Data src;

    /**
     * <b>W3C says</b>:<br>
     * A dynamic alternative to 'src'. If this attribute is present,
     * the SCXML Processor must evaluate it when the parent &lt;invoke> element is evaluated and treat the result
     * as if it had been entered as the value of 'src'.
     */
    public Data src_expr;

    /**
     * <b>W3C says</b>:<br>
     * Boolean.<br>
     * A flag indicating whether to forward events to the invoked process.
     */
    public boolean autoforward;

    /**
     * <b>W3C says</b>:<br>
     * Executable content to handle the data returned from the invoked component.<br>
     * Occurs 0 or 1 times. See 6.5 &lt;finalize> for details.
     */
    public ExecutableContentRegion finalize;

    /**
     * Generated invokeId (identical to "id" if specified).
     */
    public String invoke_id;

    public String parent_state_name;

    /**
     * &lt;param> children
     */
    public java.util.List<Parameter> params;

    public void push_param(Parameter param) {
        if (params == null)
            params = new ArrayList<>(1);
        params.add(param);
    }

    public CommonContent content;
}
