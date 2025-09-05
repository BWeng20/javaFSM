package com.bw.fsm;

import java.util.Objects;

public class CommonContent {

    /**
     * content inside &lt;content> child
     */
    public final Data content;

    /**
     * expr-attribute of &lt;content> child
     */
    public final String content_expr;

    public CommonContent(Data content, String content_expr) {
        this.content = content;
        this.content_expr = content_expr;

    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(50);
        stringBuilder.append("{");
        if (content != null)
            stringBuilder.append("content: ").append(content);
        if (content_expr != null)
            stringBuilder.append("expr: ").append(content_expr);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof CommonContent that) {
            return Objects.equals(content, that.content) && Objects.equals(content_expr, that.content_expr);
        }
        return false;
    }
}
