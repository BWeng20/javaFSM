package com.bw.fsm;

import com.bw.fsm.tracer.TraceMode;

public class TestUseCase {
    public String name;
    public TestSpecification specification;
    public Fsm fsm;
    public TraceMode trace_mode;
    public final IncludePaths include_paths = new IncludePaths();
}
