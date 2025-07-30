package com.bw.fsm;

import java.util.ArrayList;
import java.util.List;

public class ExecutableContentRegion {

    public List<ExecutableContent> content = new ArrayList<>();
    public String tag;

    ExecutableContentRegion(ExecutableContent content, String tag) {
        if (content != null)
            this.content.add(content);
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        sb.append("ECRegion #").append(tag);
        if (!content.isEmpty())
            sb.append(" (").append(content.get(0)).append(')');
        return sb.toString();
    }
}
