package com.bw.fsm;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ExecutableContentBlock {

    public @NotNull
    final List<ExecutableContent> content;
    public String tag;

    public ExecutableContentBlock(ExecutableContent content, String tag) {
        this.content = new ArrayList<>(1);
        if (content != null)
            this.content.add(content);
        this.tag = tag;
    }

    public ExecutableContentBlock(@NotNull List<ExecutableContent> content, String tag) {
        this.content = new ArrayList<>(content);
        this.tag = tag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        sb.append("ECBlock #").append(tag);
        if (!content.isEmpty())
            sb.append(" {").append(content.get(0)).append('}');
        return sb.toString();
    }
}
