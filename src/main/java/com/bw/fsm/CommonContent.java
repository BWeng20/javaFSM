package com.bw.fsm;

public class CommonContent {
    /**
     * content inside &lt;content> child
     */
    public String content;

    /**
     * expr-attribute of &lt;content> child
     */
    public String content_expr;

    public CommonContent(String content, String content_expr) {
        this.content = content;
        this.content_expr = content_expr;

    }
}
