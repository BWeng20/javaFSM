package com.bw.fsm;

import com.bw.fsm.executableContent.Parameter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DoneData {
    /* content of &lt;content> child. */
    public @Nullable CommonContent content;

    /**
     * &lt;param> children
     */
    public @Nullable List<Parameter> params;

    public void push_param(Parameter param) {
        if (params == null)
            params = new ArrayList<>(1);
        params.add(param);
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o)
            return true;
        if (o instanceof DoneData doneData) {
            return Objects.equals(content, doneData.content) && Objects.equals(params, doneData.params);
        }
        return false;
    }
}
