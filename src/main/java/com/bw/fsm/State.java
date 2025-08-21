package com.bw.fsm;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <style></style>
 * Stores all data for a State.<br>
 * In this model "State" is used for SCXML elements "State" and "Parallel".<p>
 *
 * <b>W3C says:</b><br>
 * <b>3.3 &lt;state></b> Holds the representation of a state.<br>
 * <br>
 * <b>3.3.1 Attribute Details</b><br>
 * <table class="lined">
 * <tr><th>Name</th><th>Required</th><th>Attribute Constraints</th><th>Type</th><th>Default Value</th><th>Valid Values</th><th>Description</th></tr>
 * <tr><td>id</td><td>false</td><td>none</td><td>ID</td><td>none</td><td>A valid id as defined XML Schema.</td><td>The identifier for this state. See 3.14 IDs for details.</td></tr>
 * <tr><td>initial</td><td>false</td><td>MUST NOT be specified in conjunction with the &lt;initial> element. MUST NOT occur in atomic states.</td><td>IDREFS</td><td>none</td><td>A legal state specification. See 3.11 Legal State Configurations and Specifications for details.</td><td>The id of the default initial state (or states) for this state.</td></tr>
 * </table><p></p>
 * <b>3.3.2 Children:</b>
 * <ul>
 * <li>&lt;onentry> Optional element holding executable content to be run upon entering this &lt;state\>.
 *  Occurs 0 or more times. See 3.8 &lt;onentry></li>
 * <li>&lt;onexit> Optional element holding executable content to be run when exiting this &lt;state\>.
 *  Occurs 0 or more times. See 3.9 &lt;onexit></li>
 * <li>&lt;transition> Defines an outgoing transition from this state. Occurs 0 or more times.<br>
 *  See 3.5 &lt;transition></li>
 * <li>&lt;initial> In states that have substates, an optional child which identifies the default
 *  initial state. Any transition which takes the parent state as its target will result in the
 *  state machine also taking the transition contained inside the &lt;initial> element.:<br>
 *  See 3.6 &lt;initial>.</li>
 * <li>&lt;state> Defines a sequential substate of the parent state. Occurs 0 or more times.</li>
 * <li>&lt;parallel> Defines a parallel substate. Occurs 0 or more times. See 3.4 &lt;parallel></li>
 * <li>&lt;final>. Defines a final substate. Occurs 0 or more times. See 3.7 &lt;final>.</li>
 * <li>&lt;history> A child pseudo-state which records the descendant state(s) that the parent state
 *  was in the last time the system transitioned from the parent.<br>
 *  May occur 0 or more times. See 3.10 &lt;history>.</li>
 * <li>&lt;datamodel> Defines part or all of the data model. Occurs 0 or 1 times. See 5.2 &lt;datamodel></li>
 * <li>&lt;invoke> Invokes an external service. Occurs 0 or more times. See 6.4 &lt;invoke> for details.</li>
 * </ul>
 *
 * <b>Definitions:</b><br>
 * <ul>
 * <li>An atomic state is a &lt;state> that has no &lt;state>, &lt;parallel> or &lt;final> children.</li>
 * <li>A compound state is a &lt;state> that has &lt;state>, &lt;parallel>, or &lt;final> children
 *  (or a combination of these).</li>
 * <li>The default initial state(s) of a compound state are those specified by the 'initial' attribute
 *  or &lt;initial> element, if either is present. Otherwise it is the state's first child state
 *  in document order.</li>
 * </ul>
 * In a conformant SCXML document, a compound state may specify either an "initial" attribute or an
 * &lt;initial> element, but not both.<br>
 * See 3.6 &lt;initial> for a discussion of the difference between
 * the two notations.
 */
public class State {

    /**
     * The internal Id (not W3C). Used to refence the state during runtime.
     */
    public int id;

    /**
     * The unique id, counting in document order.
     * "id" is increasing on references to states, not declaration and may not result in correct order.
     */
    public int doc_id;

    /**
     * The SCXML id.
     */
    public String name;

    /**
     * The initial transition id (if the state has sub-states).
     */
    public Transition initial;

    /**
     * The sub-states of this state.
     */
    public final java.util.List<State> states = new java.util.ArrayList<>();

    /**
     * True for "parallel" states
     */
    public boolean is_parallel = false;

    /**
     * True for "final" states.
     */
    public boolean is_final = false;

    /**
     * The specified historic mode.
     */
    public HistoryType history_type = HistoryType.None;

    /**
     * The script that is executed if the state is entered. See W3c comments for &lt;onentry> above.<br>
     * The list consists of a separate list for each &lt;onentry> block, because each block needs local error handling.
     */
    public final java.util.List<ExecutableContentRegion> onentry = new ArrayList<>();

    /**
     * The script that is executed if the state is left. See W3c comments for &lt;onexit> above.<br>
     * The list consists of a separate list for each &lt;onexit> block, because each block needs local error handling.
     */
    public final java.util.List<ExecutableContentRegion> onexit = new ArrayList<>();

    /**
     * All transitions between sub-states.
     */
    public final List<Transition> transitions = new List<>();

    /**
     * Invoke-elements.
     */
    public final List<Invoke> invoke = new List<>();

    /**
     * The current history.
     */
    public final List<State> history = new List<>();

    /**
     * The initial data values on this state.
     */
    public final Map<String, Data> data = new HashMap<>();

    /**
     * True if the state was never entered before. Remembers that we need initialisation in lazy binding mode.
     */
    public boolean isFirstEntry = true;

    /**
     * The parent state. Only null for the pseudo root.
     */
    public @Nullable State parent;

    /**
     * Done Data for this state.
     */
    public @Nullable DoneData donedata;

    /**
     * Initializes a new state. Used only during loading of the FSM, never dynamically during runtime.
     *
     * @param name The name.
     */
    public State(String name) {
        id = 0;
        doc_id = 0;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }
}
