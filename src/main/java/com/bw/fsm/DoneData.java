package com.bw.fsm;

import com.bw.fsm.executableContent.Parameter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DoneData {
    /* content of &lt;content> child. */
    public CommonContent content;

    /**
     * &lt;param> children
     */
    public @Nullable List<Parameter> params;

    public void push_param(Parameter param) {
        if (params == null)
            params = new ArrayList<>(1);
        params.add(param);
    }
}
