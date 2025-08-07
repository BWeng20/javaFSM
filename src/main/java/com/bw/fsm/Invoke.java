package com.bw.fsm;

/**
 * <b>W3C says</b>:
 * The &lt;invoke> element is used to create an instance of an external service.
 */
public class Invoke {

    public Integer doc_id;

    /// *W3C says*:
    /// Attribute 'idlocation':\
    /// Location expression.\
    /// Any data model expression evaluating to a data model location.\
    /// Must not occur with the 'id' attribute.
    public String external_id_location;

    /// *W3C says*:
    /// Attribute 'type':\
    /// A URI specifying the type of the external service.
    public Data type_name;

    /// *W3C says*:
    /// Attribute 'typeexpr':\
    /// A dynamic alternative to 'type'. If this attribute is present, the SCXML Processor must evaluate it
    /// when the parent \<invoke\> element is evaluated and treat the result as if it had been entered as
    /// the value of 'type'.
    public Data type_expr;

    /// *W3C says*:
    /// List of valid location expressions
    public java.util.List<String> name_list;

    /// *W3C says*:
    /// A URI to be passed to the external service.\
    /// Must not occur with the 'srcexpr' attribute or the \<content\> element.
    public Data src;

    /// *W3C says*:
    /// A dynamic alternative to 'src'. If this attribute is present,
    /// the SCXML Processor must evaluate it when the parent \<invoke\> element is evaluated and treat the result
    /// as if it had been entered as the value of 'src'.
    public Data src_expr;

    /// *W3C says*:
    /// Boolean.\
    /// A flag indicating whether to forward events to the invoked process.
    public boolean autoforward;

    /// *W3C says*:
    /// Executable content to handle the data returned from the invoked component.
    /// Occurs 0 or 1 times. See 6.5 \<finalize}> for details.
    public ExecutableContentRegion finalize;

    /// Generated invokeId (identical to "id" if specified).
    public String invoke_id;

    public String parent_state_name;

    /// \<param\> children
    public java.util.List<Parameter> params;

    public CommonContent content;
}
