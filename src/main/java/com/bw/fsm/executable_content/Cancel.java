package com.bw.fsm.executable_content;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

import java.util.Map;

public class Cancel implements ExecutableContent {

    public String send_id = null;
    public Data send_id_expr = Data.None.NONE;

    /**
     * <b>W3c says:</b><br>
     * The &lt;cancel> element is used to cancel a delayed &lt;send> event.<br>
     * The SCXML Processor MUST NOT allow \<cancel> to affect events that were not raised in the
     * same session. The Processor SHOULD make its best attempt to cancel all delayed events with
     * the specified id. Note, however, that it can not be guaranteed to succeed, for example if
     * the event has already been delivered by the time the &lt;cancel> tag executes.
     */
    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        var send_id_data = datamodel.get_expression_alternative_value(
                this.send_id == null ? null : new Data.Source(this.send_id), send_id_expr);
        if (send_id_data != null && !send_id_data.is_empty()) {
            datamodel.global().delayed_send.remove(send_id_data.as_script());
        }
        return true;
    }

    @Override
    public int get_type() {
        return TYPE_CANCEL;
    }


    @Override
    public Map<String, Object> get_trace() {
        return ExecutableContent.toMap("send_id", send_id, "send_id_expr", send_id_expr);
    }

}
