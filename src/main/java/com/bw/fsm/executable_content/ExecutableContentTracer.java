package com.bw.fsm.executable_content;

import com.bw.fsm.ExecutableContent;
import com.bw.fsm.Fsm;

public interface ExecutableContentTracer {
    void print_name_and_attributes(ExecutableContent ec, String[][] attrs);

    void print_sub_content(String name, Fsm fsm, ExecutableContent content);
}
