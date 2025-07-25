package com.bw.fsm;

import java.util.ArrayList;
import java.util.List;

public class ExecutableContentRegion {

    public List<ExecutableContent> content = new ArrayList<>();
    public String tag;

    ExecutableContentRegion(ExecutableContent content, String tag) {
        this.content.add(content);
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(10);
        if (!content.isEmpty())
            sb.append(content.get(0)).append(' ');
        sb.append("#").append(tag);
        return sb.toString();
    }
}
