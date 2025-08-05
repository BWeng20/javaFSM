package com.bw.fsm.datamodel.null_datamodel;

import com.bw.fsm.Fsm;
import com.bw.fsm.State;
import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.Datamodel;
import com.bw.fsm.datamodel.DatamodelFactory;
import com.bw.fsm.datamodel.GlobalData;

import java.util.Map;

/// The "Null" Datamodel as specified by W3C. A minimal model without any data.
/// ## W3C says:
/// ### B.1 The Null Data Model
/// The value "null" for the 'datamodel' attribute results in an absent or empty data model. In particular:
/// - B.1.1 Data Model
///
///   There is no underlying data model.
/// - B.1.2 Conditional Expressions
///
///   The boolean expression language consists of the In predicate only. It has the form 'In(id)',
///   where id is the id of a state in the enclosing state machine.
///   The predicate must return 'true' if and only if that state is in the current state configuration.
/// - B.1.3 Location Expressions
///
///   There is no location expression language.
/// - B.1.4 Value Expressions
///
///   There is no value expression language.
/// - B.1.5 Scripting
///
///   There is no scripting language.
/// - B.1.6 System Variables
///
///   System variables are not accessible.
/// - B.1.7 Unsupported Elements
///
///   The \<foreach\> element and the elements defined in 5 Data Model and Data Manipulation are not
///   supported in the Null Data Model.
public class NullDatamodel extends Datamodel {

    public static final String NULL_DATAMODEL = "NULL";
    public static final String NULL_DATAMODEL_LC = "null";

    public static void register() {
        DatamodelFactory.register_datamodel(NULL_DATAMODEL_LC, new NullDatamodelFactory());
    }

    public GlobalData global;
    public Map<String, State> state_id_to_state;
    public ActionWrapper actions;

    public NullDatamodel(GlobalData global) {
        this.global = global;
    }

    @Override
    public GlobalData global() {
        return global;
    }

    @Override
    public String get_name() {
        return NULL_DATAMODEL;
    }

    @Override
    public void add_functions(Fsm fsm) {

    }

}
