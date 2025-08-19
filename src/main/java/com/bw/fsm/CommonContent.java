package com.bw.fsm;

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
}
