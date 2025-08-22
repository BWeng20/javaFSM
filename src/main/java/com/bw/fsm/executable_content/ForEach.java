package com.bw.fsm.executable_content;

import com.bw.fsm.Data;
import com.bw.fsm.ExecutableContent;
import com.bw.fsm.ExecutableContentBlock;
import com.bw.fsm.Fsm;
import com.bw.fsm.datamodel.Datamodel;

import java.util.Map;

public class ForEach implements ExecutableContent {

    public Data array;
    public String item;
    public String index;
    public ExecutableContentBlock content;

    public static final String INDEX_TEMP = "__$index";

    @Override
    public boolean execute(Datamodel datamodel, Fsm fsm) {
        String idx = this.index == null ? INDEX_TEMP : index;
        datamodel.execute_for_each(array, item, idx, () -> {
            if (content != null) {
                for (var e : content.content) {
                    if (!e.execute(datamodel, fsm)) {
                        return false;
                    }
                }
            }
            return true;
        });
        return true;
    }

    @Override
    public int get_type() {
        return TYPE_FOREACH;
    }

    @Override
    public Map<String, Object> get_trace() {
        return ExecutableContent.toMap("array", this.array,
                "item", this.item,
                "index", this.index,
                "content", this.content);
    }

    public ForEach() {
        array = Data.None.NONE;
    }
}
